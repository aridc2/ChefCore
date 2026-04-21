package es.chefcore.app.logic

import es.chefcore.app.data.database.Ingrediente

object VoiceParser {
    private val palabrasANumeros: Map<String, String> = mapOf(
        "un" to "1", "uno" to "1", "una" to "1",
        "dos" to "2", "tres" to "3", "cuatro" to "4",
        "cinco" to "5", "seis" to "6", "siete" to "7",
        "ocho" to "8", "nueve" to "9", "diez" to "10"
    )

    fun parsearIngrediente(texto: String): Ingrediente {
        // Limpiamos espacios, pasamos a minúsculas y QUITAMOS PUNTOS finales
        var t = texto.lowercase().trim().replace(".", "").replace(Regex("\\s+"), " ")

        val palabras = t.split(" ")

        // Traducimos "Cinco" -> "5" (Usando el primer índice correctamente)
        if (palabras.isNotEmpty() && palabrasANumeros.containsKey(palabras[0])) {
            val primeraPalabra = palabras[0]
            val valorNumerico = palabrasANumeros[primeraPalabra] ?: ""
            val resto = t.substringAfter(primeraPalabra, "").trim()
            t = "$valorNumerico $resto"
        }

        // REGEX CORREGIDA:
        // Usamos \b antes y después de las unidades para que "l" sea una palabra suelta
        val regex = Regex("""^(\d+[.,]?\d*)\s*\b(kg|g|ud|l|ml|litros|unidades)?\b\s*(?:de|del)?\s*(.+?)(?:\s+a\s*(.+))?${'$'}""")
        val match = regex.find(t)

        return if (match != null) {
            val (cantStr, uniStr, nomStr, preRaw) = match.destructured
            val precioFinal = if (preRaw.isNotBlank()) {
                val limpio = preRaw.replace(Regex("[^0-9,. ]"), "").replace(",", ".").replace(" ", "").trim()
                var valor = limpio.toDoubleOrNull() ?: 0.0
                if (valor >= 100 && !limpio.contains(".")) valor /= 100.0
                valor
            } else 0.0

            Ingrediente(
                nombre = nomStr.trim().replaceFirstChar { it.uppercase() },
                cantidad = cantStr.replace(",", ".").toDoubleOrNull() ?: 1.0,
                unidad = if (uniStr.isBlank()) "ud" else uniStr,
                precio = precioFinal
            )
        } else {
            // Si no hay número al principio, tratamos todo como el nombre
            Ingrediente(
                nombre = t.replaceFirstChar { it.uppercase() },
                cantidad = 1.0,
                unidad = "ud",
                precio = 0.0
            )
        }
    }
}