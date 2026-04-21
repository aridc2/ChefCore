package es.chefcore.app.logic

object UnitConverter {
    // Definimos qué unidad es la "base" para cada tipo
    val UNIDADES_PESO = listOf("kg", "g")
    val UNIDADES_VOLUMEN = listOf("L", "ml")
    val UNIDADES_CONTEO = listOf("ud")

    val TODAS_LAS_UNIDADES = UNIDADES_PESO + UNIDADES_VOLUMEN + UNIDADES_CONTEO

    // Devuelve el factor de conversión a la unidad base (kg, L o ud)
    fun obtenerFactor(unidad: String): Double {
        return when (unidad) {
            "g", "ml" -> 0.001
            else -> 1.0 // kg, L, ud son base
        }
    }

    // Devuelve la unidad base correspondiente
    fun obtenerUnidadBase(unidad: String): String {
        return when {
            unidad in UNIDADES_PESO -> "kg"
            unidad in UNIDADES_VOLUMEN -> "L"
            else -> "ud"
        }
    }

    // Comprueba si se pueden mezclar (Ej: no puedes sumar kg con L)
    fun sonCompatibles(u1: String, u2: String): Boolean {
        return obtenerUnidadBase(u1) == obtenerUnidadBase(u2)
    }

    fun formatearCantidad(cantidad: Double, unidad: String): String {
        // Ponemos 2 decimales y cambiamos coma por punto por si acaso
        val cantidadFormateada = "%.2f".format(cantidad).replace(",", ".")

        // Si termina en .00 (ej: 5.00 kg), lo dejamos limpio como "5 kg"
        val limpia = if (cantidadFormateada.endsWith(".00")) {
            cantidadFormateada.substringBefore(".00")
        } else {
            cantidadFormateada
        }

        return "$limpia $unidad"
    }
}