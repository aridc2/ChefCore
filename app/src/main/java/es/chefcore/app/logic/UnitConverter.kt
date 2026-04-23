package es.chefcore.app.logic

object UnitConverter {
    // Familias de unidades
    enum class TipoUnidad {
        PESO,
        VOLUMEN,
        UNIDAD
    }
    
    // Definición de unidades por familia
    val UNIDADES_PESO = listOf("kg", "g", "lb", "oz")
    val UNIDADES_VOLUMEN = listOf("L", "l", "ml", "cl", "dl")
    val UNIDADES_CONTEO = listOf("ud", "unidad", "unidades", "u")
    
    val TODAS_LAS_UNIDADES = UNIDADES_PESO + UNIDADES_VOLUMEN + UNIDADES_CONTEO
    
    /**
     * Convierte cantidad de una unidad a otra DENTRO de la misma familia
     */
    fun convertir(cantidad: Double, unidadOrigen: String, unidadDestino: String): Double {
        if (unidadOrigen.lowercase() == unidadDestino.lowercase()) return cantidad
        
        val familia = detectarFamilia(unidadOrigen)
        if (familia != detectarFamilia(unidadDestino)) {
            throw IllegalArgumentException(
                "No se puede convertir $unidadOrigen a $unidadDestino (familias diferentes)"
            )
        }
        
        // Convertir a base → luego a destino
        val enBase = cantidad * obtenerFactor(unidadOrigen)
        return enBase / obtenerFactor(unidadDestino)
    }
    
    /**
     * Factor de conversión: cuántas unidades base representa 1 unidad de entrada
     * Ej: 1g = 0.001kg, 1ml = 0.001L, 1ud = 1ud
     */
    fun obtenerFactor(unidad: String): Double {
        return when (unidad.lowercase()) {
            // PESO (base: kg)
            "kg" -> 1.0
            "g" -> 0.001
            "lb" -> 0.453592
            "oz" -> 0.0283495
            
            // VOLUMEN (base: L)
            "l" -> 1.0
            "ml" -> 0.001
            "cl" -> 0.01
            "dl" -> 0.1
            
            // CONTEO (base: ud)
            "ud", "unidad", "unidades", "u" -> 1.0
            
            else -> 1.0 // Por defecto, sin conversión
        }
    }
    
    /**
     * Devuelve la unidad base de la familia
     */
    fun obtenerUnidadBase(unidad: String): String {
        return when (detectarFamilia(unidad)) {
            TipoUnidad.PESO -> "kg"
            TipoUnidad.VOLUMEN -> "L"
            TipoUnidad.UNIDAD -> "ud"
        }
    }
    
    /**
     * Detecta a qué familia pertenece una unidad
     */
    fun detectarFamilia(unidad: String): TipoUnidad {
        val u = unidad.lowercase()
        return when {
            u in UNIDADES_PESO.map { it.lowercase() } -> TipoUnidad.PESO
            u in UNIDADES_VOLUMEN.map { it.lowercase() } -> TipoUnidad.VOLUMEN
            else -> TipoUnidad.UNIDAD
        }
    }
    
    /**
     * Comprueba si dos unidades son compatibles (misma familia)
     */
    fun sonCompatibles(u1: String, u2: String): Boolean {
        return detectarFamilia(u1) == detectarFamilia(u2)
    }
    
    /**
     * Calcula el coste de una cantidad específica de ingrediente
     * Útil para escandallos
     */
    fun calcularCoste(
        cantidadNecesaria: Double,
        unidadReceta: String,
        precioUnitarioBase: Double,
        unidadBase: String
    ): Double {
        // Convertir cantidad de receta a unidad base
        val cantidadEnBase = convertir(cantidadNecesaria, unidadReceta, unidadBase)
        
        // Coste = cantidad en base × precio por unidad base
        return cantidadEnBase * precioUnitarioBase
    }
    
    /**
     * Formatea cantidad con unidad de forma legible
     * Auto-convierte 1000g -> 1kg, 1000ml -> 1L, etc.
     */
    fun formatearCantidad(cantidad: Double, unidad: String): String {
        val cantidadFormateada = when {
            // Auto-conversión a unidades más grandes si tiene sentido
            cantidad >= 1000 && unidad.lowercase() == "g" -> {
                val enKg = cantidad / 1000.0
                "%.2f kg".format(enKg)
            }
            cantidad >= 1000 && unidad.lowercase() == "ml" -> {
                val enL = cantidad / 1000.0
                "%.2f L".format(enL)
            }
            cantidad % 1.0 == 0.0 -> {
                // Sin decimales si es entero
                "${cantidad.toInt()} $unidad"
            }
            else -> {
                "%.2f $unidad".format(cantidad)
            }
        }
        
        return cantidadFormateada.replace(",", ".")
    }
    
    /**
     * Lista de unidades compatibles para mostrar en dropdowns
     */
    fun obtenerUnidadesCompatibles(unidadActual: String): List<String> {
        return when (detectarFamilia(unidadActual)) {
            TipoUnidad.PESO -> listOf("kg", "g")
            TipoUnidad.VOLUMEN -> listOf("L", "ml", "cl")
            TipoUnidad.UNIDAD -> listOf("ud", "unidad")
        }
    }
}
