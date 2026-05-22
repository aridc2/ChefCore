package es.chefcore.app.logic

import es.chefcore.app.data.database.Ingrediente

object VoiceParser {

    private val palabrasANumeros = mapOf(
        "un" to 1.0, "uno" to 1.0, "una" to 1.0,
        "dos" to 2.0, "tres" to 3.0, "cuatro" to 4.0,
        "cinco" to 5.0, "seis" to 6.0, "siete" to 7.0,
        "ocho" to 8.0, "nueve" to 9.0, "diez" to 10.0,
        "once" to 11.0, "doce" to 12.0, "trece" to 13.0,
        "catorce" to 14.0, "quince" to 15.0, "veinte" to 20.0,
        "veinticinco" to 25.0, "treinta" to 30.0, "cuarenta" to 40.0,
        "cincuenta" to 50.0, "cien" to 100.0,
        "medio" to 0.5, "media" to 0.5
    )

    private val sinonimosUnidad = mapOf(
        "kg" to "kg", "kilo" to "kg", "kilos" to "kg", "kilogramo" to "kg", "kilogramos" to "kg",
        "g" to "g", "gramo" to "g", "gramos" to "g",
        "l" to "L", "litro" to "L", "litros" to "L",
        "ml" to "ml", "mililitro" to "ml", "mililitros" to "ml",
        "cl" to "cl", "centilitro" to "cl", "centilitros" to "cl",
        "ud" to "ud", "unidad" to "ud", "unidades" to "ud",
        "bote" to "ud", "botella" to "ud", "caja" to "ud", "paquete" to "ud"
    )

    fun parsearIngrediente(texto: String): Ingrediente {
        var t = texto.lowercase().trim()
            .replace(".", "")
            .replace(Regex("\\s+"), " ")

        // Convertir palabras numéricas al inicio
        val primerasPalabras = t.split(" ")
        val cantidadPalabra = palabrasANumeros[primerasPalabras.firstOrNull()]
        if (cantidadPalabra != null) {
            t = "$cantidadPalabra ${t.substringAfter(primerasPalabras.first()).trim()}"
        }

        // Patrón: [cantidad] [unidad] [de] [nombre] [a precio]
        val regex = Regex(
            """^(\d+[.,]?\d*)\s*\b(kg|kilo|kilos|kilogramo|kilogramos|g|gramo|gramos|l|litro|litros|ml|cl|ud|unidad|unidades|bote|botella|caja|paquete)?\b\s*(?:de[l]?)?\s*(.+?)(?:\s+a\s*(.+))?$"""
        )
        val match = regex.find(t)

        return if (match != null) {
            val (cantStr, uniRaw, nomStr, preRaw) = match.destructured
            val unidad = sinonimosUnidad[uniRaw.lowercase()] ?: if (uniRaw.isBlank()) "ud" else uniRaw
            val precio = parsearPrecio(preRaw)
            Ingrediente(
                nombre   = nomStr.trim().replaceFirstChar { it.uppercase() },
                cantidad = cantStr.replace(",", ".").toDoubleOrNull() ?: 1.0,
                unidad   = unidad,
                precio   = precio
            )
        } else {
            // Sin número → todo es el nombre
            Ingrediente(
                nombre   = t.replaceFirstChar { it.uppercase() },
                cantidad = 1.0,
                unidad   = "ud",
                precio   = 0.0
            )
        }
    }

    private fun parsearPrecio(raw: String): Double {
        if (raw.isBlank()) return 0.0
        val limpio = raw.replace(Regex("[^0-9,.]"), "")
            .replace(",", ".").trim()
        val valor = limpio.toDoubleOrNull() ?: 0.0
        // Si dicen "150" en vez de "1,50"
        return if (valor >= 100 && !limpio.contains(".")) valor / 100.0 else valor
    }
}