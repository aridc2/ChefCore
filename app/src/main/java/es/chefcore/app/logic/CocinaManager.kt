package es.chefcore.app.logic

import es.chefcore.app.data.database.Ingrediente
import es.chefcore.app.data.database.IngredienteDao
import es.chefcore.app.data.database.RecetaDao

class CocinaManager(
    private val iDao: IngredienteDao,
    private val rDao: RecetaDao
) {

    /**
     * Intenta cocinar una receta.
     * Retorna 'true' si había stock suficiente y se ha descontado.
     * Retorna 'false' si faltan ingredientes en el inventario.
     */
    suspend fun cocinar(recetaId: Int, raciones: Int = 1): Boolean {
        val ingredientesNecesarios = rDao.obtenerIngredientesStatic(recetaId)

        // 1. COMPROBAR STOCK (No cocinamos si falta algo)
        for (ing in ingredientesNecesarios) {
            val ingredienteBD = iDao.buscarPorNombre(ing.nombre)
            val stockActual = ingredienteBD?.cantidad ?: 0.0

            if (stockActual < (ing.cantidadNecesaria * raciones)) {
                return false
            }
        }

        // 2. DESCONTAR STOCK (Si llegamos aquí, hay de todo)
        for (ing in ingredientesNecesarios) {
            val ingredienteBD = iDao.buscarPorNombre(ing.nombre)!!
            val nuevoStock = ingredienteBD.cantidad - (ing.cantidadNecesaria * raciones)
            iDao.actualizar(ingredienteBD.copy(cantidad = nuevoStock))
        }

        return true
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
            
            // Verificar compatibilidad de unidades
            if (!UnitConverter.sonCompatibles(existente.unidad, unidad)) {
                return RegistroStockResult.ErrorIncompatible(
                    ingredienteExistente = existente,
                    unidadIntentada = unidad,
                    mensaje = "No puedes añadir '$unidad' a un ingrediente que usa '${existente.unidad}'"
                )
            }

            // Convertir cantidad nueva a la unidad base del ingrediente existente
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
            // ========== CASO 2: INGREDIENTE NUEVO ==========
            
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
            esRentable = margenPorcentaje >= 20.0 // Umbral configurable (20% es estándar en restauración)
        )
    }
    
    /**
     * Versión legacy que retorna String (para compatibilidad)
     * RECOMENDADO: Usar calcularRentabilidad() que retorna RentabilidadReceta
     */
    @Deprecated(
        message = "Usa calcularRentabilidad() que retorna RentabilidadReceta",
        replaceWith = ReplaceWith("calcularRentabilidad(recetaId, precioVenta)")
    )
    suspend fun calcularRentabilidadString(recetaId: Int, precioVenta: Double): String {
        val rentabilidad = calcularRentabilidad(recetaId, precioVenta)
        return "Coste: ${"%.2f".format(rentabilidad.coste)}€ | " +
               "Beneficio: ${"%.2f".format(rentabilidad.margen)}€ " +
               "(${"%.2f".format(rentabilidad.porcentajeMargen)}%)"
    }
}

// ============================================================================
// DATA CLASSES DE RESULTADO
// ============================================================================

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
     * Error genérico (validación fallida)
     */
    data class Error(val mensaje: String) : RegistroStockResult()
}

/**
 * Información de rentabilidad de una receta
 */
data class RentabilidadReceta(
    val coste: Double,           // Coste de producción
    val pvp: Double,             // Precio de venta
    val margen: Double,          // Ganancia bruta (pvp - coste)
    val porcentajeMargen: Double,// % de margen sobre PVP
    val esRentable: Boolean      // true si margen >= 20%
)
