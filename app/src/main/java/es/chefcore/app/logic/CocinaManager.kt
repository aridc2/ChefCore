package es.chefcore.app.logic

import es.chefcore.app.data.database.Ingrediente
import es.chefcore.app.data.database.IngredienteDao
import es.chefcore.app.data.database.RecetaDao
import kotlinx.coroutines.flow.first

class CocinaManager(
    private val iDao: IngredienteDao,
    private val rDao: RecetaDao
) {

    /**
     * Intenta cocinar una receta.
     * Retorna 'true' si había stock suficiente y se ha descontado.
     * Retorna 'false' si faltan ingredientes en el inventario.
     * Busca ingredientes por ID (fiable) en lugar de por nombre.
     */
    suspend fun cocinar(recetaId: Int, raciones: Int = 1): Boolean {
        val ingredientesNecesarios = rDao.obtenerIngredientesStatic(recetaId)

        // 1. COMPROBAR STOCK — si falta algo, no cocinamos nada
        for (ing in ingredientesNecesarios) {
            val ingredienteBD = iDao.obtenerPorId(ing.ingredienteId) ?: return false
            if (ingredienteBD.cantidad < (ing.cantidadNecesaria * raciones)) {
                return false
            }
        }

        // 2. DESCONTAR STOCK — solo llegamos aquí si hay suficiente de todo
        for (ing in ingredientesNecesarios) {
            val ingredienteBD = iDao.obtenerPorId(ing.ingredienteId) ?: continue
            val nuevoStock = ingredienteBD.cantidad - (ing.cantidadNecesaria * raciones)
            iDao.actualizar(ingredienteBD.copy(cantidad = nuevoStock))
        }

        return true
    }

    // Similitud de nombres (Levenshtein normalizado) ────────────────────────

    private fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) for (j in 1..b.length) {
            dp[i][j] = if (a[i - 1] == b[j - 1]) dp[i - 1][j - 1]
            else 1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
        }
        return dp[a.length][b.length]
    }

    fun similitud(a: String, b: String): Double {
        val maxLen = maxOf(a.length, b.length)
        if (maxLen == 0) return 1.0
        return 1.0 - levenshtein(a.lowercase(), b.lowercase()).toDouble() / maxLen
    }

    /**
     * Busca el ingrediente existente mas similar al nombre dado.
     * Devuelve el par (ingrediente, similitud) si supera el umbral, o null.
     * Umbral recomendado: 0.82 — captura "IFA-ELIGES" vs "IFA-ELIGEN" (1 letra en 16 = 0.9375)
     * sin confundir productos distintos como "QUESO GRANA PADANO" vs "QUESO ROQUEFORT".
     */
    suspend fun buscarSimilar(nombre: String, umbral: Double = 0.82): Pair<Ingrediente, Double>? {
        val todos: List<Ingrediente> = iDao.obtenerTodos().first()
        return todos
            .map { ing -> ing to similitud(nombre, ing.nombre) }
            .filter { (_, sim) -> sim >= umbral }
            .maxByOrNull { (_, sim) -> sim }
    }

    /**
     * Registra entrada de stock con validación anti-duplicados y conversión de unidades
     * SUMA cantidades si ya existe, NO sobreescribe
     * Retorna un resultado sellado con información detallada de la operación
     */
    suspend fun registrarEntradaStock(
        nombre: String,
        cantidad: Double,
        unidad: String,
        precioTotal: Double
    ): RegistroStockResult {
        val nombreLimpio = nombre.trim().replaceFirstChar { it.uppercase() }

        // Validación básica
        if (cantidad <= 0) {
            return RegistroStockResult.Error("La cantidad debe ser mayor a 0")
        }
        if (precioTotal < 0) {
            return RegistroStockResult.Error("El precio no puede ser negativo")
        }

        val existente = iDao.buscarPorNombre(nombreLimpio)

        if (existente != null) {
            // ========== CASO 1: INGREDIENTE YA EXISTE ==========

            if (!UnitConverter.sonCompatibles(existente.unidad, unidad)) {
                return RegistroStockResult.ErrorIncompatible(
                    ingredienteExistente = existente,
                    unidadIntentada = unidad,
                    mensaje = "No puedes añadir '$unidad' a un ingrediente que usa '${existente.unidad}'"
                )
            }

            val cantidadEnUnidadExistente = UnitConverter.convertir(
                cantidad = cantidad,
                unidadOrigen = unidad,
                unidadDestino = existente.unidad
            )

            // CÁLCULO DE PMP (Precio Medio Ponderado)
            val valorEnAlmacen = existente.cantidad * existente.precio
            val valorNuevaCompra = precioTotal
            val stockTotal = existente.cantidad + cantidadEnUnidadExistente
            val nuevoPrecioPMP = if (stockTotal > 0) {
                (valorEnAlmacen + valorNuevaCompra) / stockTotal
            } else {
                existente.precio
            }

            val actualizado = existente.copy(
                cantidad = stockTotal,
                precio = nuevoPrecioPMP
            )

            iDao.actualizar(actualizado)

            return RegistroStockResult.StockActualizado(
                ingrediente = actualizado,
                cantidadSumada = cantidadEnUnidadExistente,
                pmpAnterior = existente.precio,
                pmpNuevo = nuevoPrecioPMP
            )

        } else {
            // ========== CASO 2: SIN COINCIDENCIA EXACTA ==========
            // Comprobar si hay un nombre muy similar → fusionar automaticamente
            val similar = buscarSimilar(nombreLimpio)
            if (similar != null) {
                val (candidato, _) = similar
                // Reutilizar el ingrediente existente con su nombre correcto
                return registrarEntradaStock(candidato.nombre, cantidad, unidad, precioTotal)
            }

            // ========== CASO 3: INGREDIENTE REALMENTE NUEVO ==========

            // Normalizar a unidad base
            val unidadBase = UnitConverter.obtenerUnidadBase(unidad)
            val cantidadEnBase = UnitConverter.convertir(cantidad, unidad, unidadBase)
            val precioUnitarioBase = if (cantidadEnBase > 0) {
                precioTotal / cantidadEnBase
            } else {
                0.0
            }

            val nuevo = Ingrediente(
                nombre = nombreLimpio,
                cantidad = cantidadEnBase,
                unidad = unidadBase,
                precio = precioUnitarioBase
            )

            iDao.insertar(nuevo)

            return RegistroStockResult.NuevoIngrediente(nuevo)
        }
    }

    /**
     * Calcula la rentabilidad de una receta
     * Retorna información detallada sobre coste, PVP, margen y rentabilidad
     */
    suspend fun calcularRentabilidad(recetaId: Int, precioVenta: Double): RentabilidadReceta {
        val ingredientes = rDao.obtenerIngredientesStatic(recetaId)

        var costeTotalPlato = 0.0
        for (ing in ingredientes) {
            costeTotalPlato += ing.costeTotal
        }

        val beneficioPuro = precioVenta - costeTotalPlato
        val margenPorcentaje = if (precioVenta > 0) {
            (beneficioPuro / precioVenta) * 100
        } else {
            0.0
        }

        return RentabilidadReceta(
            coste = costeTotalPlato,
            pvp = precioVenta,
            margen = beneficioPuro,
            porcentajeMargen = margenPorcentaje,
            esRentable = margenPorcentaje >= 20.0
        )
    }



}


// DATA CLASSES DE RESULTADO

/**
 * Resultado de registrar entrada de stock
 */
sealed class RegistroStockResult {
    /**
     * Se creó un nuevo ingrediente
     */
    data class NuevoIngrediente(val ingrediente: Ingrediente) : RegistroStockResult()

    /**
     * Se sumó cantidad a un ingrediente existente y se recalculó el PMP
     */
    data class StockActualizado(
        val ingrediente: Ingrediente,
        val cantidadSumada: Double,
        val pmpAnterior: Double,
        val pmpNuevo: Double
    ) : RegistroStockResult()

    /**
     * Error: intentaste sumar unidades incompatibles (ej: kg + L)
     */
    data class ErrorIncompatible(
        val ingredienteExistente: Ingrediente,
        val unidadIntentada: String,
        val mensaje: String
    ) : RegistroStockResult()

    /**
     * Nombre muy similar a un ingrediente existente — pedir confirmacion al usuario.
     * El caller decide si usar el nombre existente o crear uno nuevo.
     */
    data class PosibleDuplicado(
        val nombreOcr: String,
        val candidato: Ingrediente,
        val similitud: Double
    ) : RegistroStockResult()

    /**
     * Error genérico (validación fallida)
     */
    data class Error(val mensaje: String) : RegistroStockResult()
}

/**
 * Información de rentabilidad de una receta
 */
data class RentabilidadReceta(
    val coste: Double,
    val pvp: Double,
    val margen: Double,
    val porcentajeMargen: Double,
    val esRentable: Boolean
)