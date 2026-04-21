package es.chefcore.app.logic

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
            // Buscamos el ingrediente real en la BD
            val ingredienteBD = iDao.buscarPorNombre(ing.nombre)
            val stockActual = ingredienteBD?.cantidad ?: 0.0

            // Si necesitamos 2 y tenemos 1... error.
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

        return true // ¡Plato cocinado con éxito y stock actualizado!
    }


    // Dentro de CocinaManager.kt
    suspend fun registrarEntradaStock(
        nombre: String,
        cantidad: Double,
        unidad: String,
        precioTotal: Double
    ): Boolean {
        val nombreLimpio = nombre.trim().replaceFirstChar { it.uppercase() }

        // 1. Normalización de unidades usando nuestro UnitConverter
        val factor = UnitConverter.obtenerFactor(unidad)
        val unidadBase = UnitConverter.obtenerUnidadBase(unidad)
        val cantNormalizada = cantidad * factor // Ej: 500g -> 0.5kg

        if (cantNormalizada <= 0) return false

        val existente = iDao.buscarPorNombre(nombreLimpio)

        if (existente != null) {
            // Comprobar compatibilidad (no sumar kg a litros)
            if (!UnitConverter.sonCompatibles(existente.unidad, unidad)) return false

            // 2. Cálculo de PMP real
            val valorEnAlmacen = existente.cantidad * existente.precio
            val valorNuevaCompra = precioTotal
            val stockTotal = existente.cantidad + cantNormalizada
            val nuevoPrecioPMP = (valorEnAlmacen + valorNuevaCompra) / stockTotal

            iDao.actualizar(existente.copy(
                cantidad = stockTotal,
                precio = nuevoPrecioPMP
            ))
        } else {
            // 3. Si es nuevo, precio unitario base
            val precioUnitarioBase = precioTotal / cantNormalizada
            iDao.insertar(es.chefcore.app.data.database.Ingrediente(
                nombre = nombreLimpio,
                cantidad = cantNormalizada,
                unidad = unidadBase,
                precio = precioUnitarioBase
            ))
        }
        return true
    }

    /**
     * Calcula los costes del plato y devuelve un texto formateado con el beneficio.
     */
    suspend fun calcularRentabilidad(recetaId: Int, precioVenta: Double): String {
        val ingredientes = rDao.obtenerIngredientesStatic(recetaId)

        var costeTotalPlato = 0.0
        for (ing in ingredientes) {
            costeTotalPlato += ing.costeTotal // Usa el cálculo automático que definimos en la data class
        }

        val beneficioPuro = precioVenta - costeTotalPlato
        val margenPorcentaje = if (precioVenta > 0) (beneficioPuro / precioVenta) * 100 else 0.0

        return "Coste: ${"%.2f".format(costeTotalPlato)}€ | Beneficio: ${"%.2f".format(beneficioPuro)}€ (${"%.2f".format(margenPorcentaje)}%)"
    }
}