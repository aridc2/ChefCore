package es.chefcore.app.logic

import android.util.Log
import com.google.mlkit.vision.text.Text
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class ItemAlbaran(
    val descripcion: String,
    val cantidad: Double = 1.0,
    val unidad: String = "ud",
    val precioUnitario: Double
)

data class ResultadoOcr(
    val proveedor: String,
    val fecha: String,
    val totalEuros: Double,
    val items: List<ItemAlbaran>,
    val textoRaw: String,
    val tipoAlbaran: String = "GENERICO"
)

enum class TipoAlbaran {
    SERCODI,        // Cash & carry: EAN en línea, muchos ítems
    MAKRO,          // Mayorista: datos en 2ª línea (KG/BL + números)
    CAMPOY,         // Pescados Motril: código+desc+lote+[Cajas,Pzas,Ud/Kg,Precio,Dto,IVA,Total]
    PARIS,          // J.Juarez Cabrera vinos: código+desc+lote+[Und,Precio,IVA,Total]
    SORBITO,        // Vinos Granada: código+desc+[cantidad,precio,total]
    COVIGRAN,       // Vinos Covigran: código+ref+desc+[Cajas,Uds,Precio,Neto,Importe,IVA]
    GENERICO
}

object AlbaranOcrParser {

    private fun parsearLineas(lineas: List<String>, textoRaw: String): ResultadoOcr {
        val tipo       = detectarTipo(textoRaw)
        val lineasPrep = prepararLineas(lineas, tipo)

        return ResultadoOcr(
            proveedor   = extraerProveedor(lineas, tipo),
            fecha       = extraerFecha(lineas),
            totalEuros  = extraerTotal(lineas),
            items       = extraerItems(lineasPrep, tipo),
            textoRaw    = textoRaw,
            tipoAlbaran = tipo.name
        )
    }
    // Reconstrucción de filas desde bloques ML Kit (con posición X,Y)
    //
    // Algoritmo:
    // 1. Recopilar TODAS las TextLines de todos los bloques con su centro Y
    // 2. Ordenar por Y
    // 3. Agrupar las que tienen Y parecido (misma fila de la tabla)
    // 4. Dentro de cada fila, ordenar por X (izquierda → derecha)
    // 5. Unir el texto → resultado: "611467 CAVA CASTELLBLANC...  8410035801558  12,00  2,399  28,79  21,0  2,902"

    private data class LineaConPos(val texto: String, val x: Int, val y: Int)

    private fun reconstruirFilas(bloques: List<Text.TextBlock>, toleranciaY: Int = 25): List<String> {
        // Recopilar todas las líneas con posición
        val lineas = bloques.flatMap { bloque ->
            bloque.lines.mapNotNull { linea ->
                val box = linea.boundingBox ?: return@mapNotNull null
                LineaConPos(linea.text, box.left, box.centerY())
            }
        }.sortedBy { it.y }

        if (lineas.isEmpty()) return emptyList()

        // Agrupar por proximidad vertical
        // IMPORTANTE: se ancla al primer elemento del grupo (no a la media movil).
        // Con la media movil el grupo puede "derivar" 400+ px hacia abajo en cadena,
        // fusionando productos de distintas filas. El ancla fija lo evita.
        val filas = mutableListOf<MutableList<LineaConPos>>()
        var filaActual = mutableListOf(lineas.first())

        for (i in 1 until lineas.size) {
            val linea = lineas[i]
            val yAncla = filaActual.first().y   // ancla fija: primer elemento del grupo
            if (kotlin.math.abs(linea.y - yAncla) <= toleranciaY) {
                filaActual.add(linea)
            } else {
                filas.add(filaActual)
                filaActual = mutableListOf(linea)
            }
        }
        filas.add(filaActual)

        // Ordenar cada fila por X y concatenar
        val filasTexto = filas.map { fila ->
            fila.sortedBy { it.x }.joinToString("  ") { it.texto }
        }
        // LOG: filas reconstruidas (las primeras 60 para no saturar)
        Log.d("ChefCore_OCR", "FILAS RECONSTRUIDAS: ${filasTexto.size}")
        filasTexto.take(60).forEachIndexed { i, f ->
            Log.d("ChefCore_OCR", "  Fila[$i]: ${f.take(120)}")
        }
        return filasTexto
    }

    /**
     * Entrada principal usando bloques ML Kit con posición.
     * Las filas de tabla se reconstruyen por coordenada Y antes de parsear.
     * Esto permite asociar precio/cantidad con su producto en el mismo Sercodi.
     */
    fun parsear(mlKit: ResultadoMLKit): ResultadoOcr {
        val lineasReconstruidas = reconstruirFilas(mlKit.bloques)
        return parsearLineas(lineasReconstruidas, mlKit.texto)
    }

    /** Compatibilidad: parsear solo desde texto plano (sin posición) */
    fun parsear(texto: String): ResultadoOcr {
        val lineas = texto.lines().map { it.trim() }.filter { it.isNotBlank() }
        return parsearLineas(lineas, texto)
    }



    // Detección de proveedor

    private fun detectarTipo(texto: String): TipoAlbaran {
        val t = texto.lowercase()
        return when {
            t.contains("sercodi") || t.contains("alipensa") || t.contains("alimentacion peninsular") -> TipoAlbaran.SERCODI
            t.contains("makro")          -> TipoAlbaran.MAKRO
            t.contains("campoy")         -> TipoAlbaran.CAMPOY
            t.contains("paris paris") || t.contains("parisparissl") || t.contains("juarez cabrera") -> TipoAlbaran.PARIS
            t.contains("sorbito")        -> TipoAlbaran.SORBITO
            t.contains("covigran")       -> TipoAlbaran.COVIGRAN
            else                         -> TipoAlbaran.GENERICO
        }
    }

    // Preparación de líneas: une continuaciones (Makro 2 líneas)

    // Abreviaturas de unidad que en Makro van solas en la 2ª línea de un ítem
    private val makroUnidades = setOf("KG","BL","BI","U","ES","BO","GF","PH","PZ","BT","UD","CB","CJ","PQ","M","TA","DS")

    private fun prepararLineas(lineas: List<String>, tipo: TipoAlbaran): List<String> {
        if (tipo != TipoAlbaran.MAKRO) return lineas

        // Normalizar misreads OCR frecuentes en Makro antes de procesar
        val norm = lineas.map { l -> l
            .replace("BỊ", "BL")
            .replace(Regex("""(\d),\s+(\d)"""), "$1,$2")          // "11, 340" → "11,340"
            .replace(Regex("""^(\d{4,9})\s(\d{3,7})\s(\d{3,7})\s""")) { mr ->   // EAN 3 partes: "08437004 602 602 ..."
                val d = mr.groupValues[1] + mr.groupValues[2] + mr.groupValues[3]
                if (d.length in 12..14) "$d " else mr.value
            }
            .replace(Regex("""^(\d{7,9})\s(\d{5,7})\s"""), "$1$2 ") // EAN 2 partes: "08437004 602602 ..."
        }

        // Estrategia: cada producto ocupa 2 líneas físicas
        //   Línea 1: [código]  [descripción]         ← el código puede faltar si el OCR lo mezcló
        //   Línea 2: [código_siguiente?]  [Cont]  [Prec.Ud]  [ContP]  [Precio]  [Cant]  [Importe]  [Imp]
        //
        // codigoPendiente: cuando la línea de datos empieza con [código_siguiente], lo extrae
        // y lo guarda para pegarlo a la siguiente descripción (que llega sin código)

        val resultado = mutableListOf<String>()
        var codigoPendiente: String? = null
        var i = 0

        while (i < norm.size) {
            val linea = norm[i]
            val siguiente = norm.getOrNull(i + 1) ?: ""
            val tokSig = siguiente.trim().split(Regex("\\s+"))
            val primerTokenSig = tokSig.firstOrNull() ?: ""

            // Si hay código pendiente y esta línea no empieza ya por dígito → prepender
            val lineaNorm = if (codigoPendiente != null && !linea.trim().firstOrNull()?.isDigit().orFalse()) {
                "$codigoPendiente  $linea"
            } else linea
            codigoPendiente = null

            // Caso A: siguiente línea empieza directamente por unidad
            if (primerTokenSig in makroUnidades) {
                resultado.add("$lineaNorm $siguiente")
                i += 2
                continue
            }

            // Caso B: siguiente línea = [código_siguiente]  [unidad]  [números]
            //   → quitar el código, guardar como codigoPendiente, fusionar solo unidad+números
            val esCodigo6 = primerTokenSig.matches(Regex("#?\\d{5,7}"))
            if (esCodigo6) {
                val sinCodigo = siguiente.trim().removePrefix(primerTokenSig).trim()
                val segundoToken = sinCodigo.split(Regex("\\s+")).firstOrNull() ?: ""
                if (segundoToken in makroUnidades) {
                    resultado.add("$lineaNorm $sinCodigo")
                    codigoPendiente = primerTokenSig   // código del siguiente producto
                    i += 2
                    continue
                }
            }

            // Caso C: siguiente línea empieza por decimal (OCR no detectó la unidad Cont)
            // Solo cuando la línea actual tiene código al inicio
            val tieneCodigoActual = codMakro6.containsMatchIn(lineaNorm) || codMakroEan.containsMatchIn(lineaNorm)
            val siguienteEsDatos  = primerTokenSig.matches(Regex("""\d+[.,]\d+"""))
            if (tieneCodigoActual && siguienteEsDatos) {
                resultado.add("$lineaNorm $siguiente")
                i += 2
                continue
            }

            resultado.add(lineaNorm)
            i++
        }
        return resultado
    }

    private fun Boolean?.orFalse() = this ?: false

    // Proveedor

    private val nombresConocidos = listOf(
        "sercodi", "alimentacion peninsular", "alipensa",
        "makro", "campoy", "paris paris", "parisparissl", "juarez cabrera",
        "sorbito", "covigran", "valle aguirre", "bodegas mar",
        "cooperativa", "albama", "albamagold"
    )

    private fun extraerProveedor(lineas: List<String>, tipo: TipoAlbaran): String {
        val texto = lineas.joinToString(" ").lowercase()
        for (nombre in nombresConocidos) {
            if (texto.contains(nombre)) {
                return lineas.firstOrNull { it.lowercase().contains(nombre) }?.take(60)
                    ?: nombre.replaceFirstChar { it.uppercase() }
            }
        }
        return lineas.firstOrNull { l ->
            l.length > 5
                    && !l.matches(Regex(""".*\d{1,2}[/\-.]\d{1,2}[/\-.]\d{2,4}.*"""))
                    && !l.matches(Regex("""^[\d\s.,€]+$"""))
        }?.take(60) ?: "Proveedor desconocido"
    }

    // Fecha

    private fun extraerFecha(lineas: List<String>): String {
        val patron = Regex("""\b(\d{1,2})[/\-.](\d{1,2})[/\-.](\d{1,2}\.?\d{2,3}|\d{2,4})\b""")
        for (l in lineas.take(25)) {
            val m = patron.find(l) ?: continue
            val (d, mes, y) = m.destructured
            val año = y.replace(".", "").let { if (it.length == 2) "20$it" else it }
            if (año.length == 4 && año.toIntOrNull() in 2020..2030) {
                return "${d.padStart(2,'0')}/${mes.padStart(2,'0')}/$año"
            }
        }
        return LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    }

    // Total

    private val patronDecimal    = Regex("""(\d{1,6}[.,]\d{1,3})""")
    // Formato español miles: 1.205,43 → quitar puntos de miles, cambiar coma → punto
    private val patronDecimalES   = Regex("""(\d{1,3}(?:\.\d{3})+,\d{2})""")

    private fun parseNumeroES(s: String): Double? =
        s.replace(".", "").replace(",", ".").toDoubleOrNull()

    private fun extraerTotal(lineas: List<String>): Double {
        val claves = listOf(
            "total albaran", "total albarán", "total factura", "importe factura",
            "total a pagar", "liquidado", "total página", "total pagina",
            "importe total", "a pagar", "total iva incluido", "total"
        )
        for (i in lineas.indices) {
            val lower = lineas[i].lowercase()
            if (claves.any { lower.contains(it) }) {
                val zona = lineas[i] + " " + (lineas.getOrNull(i + 1) ?: "")
                // Primero intentar número en formato español grande (1.205,43 → 1205.43)
                val esNum = patronDecimalES.findAll(zona).mapNotNull { parseNumeroES(it.value) }.toList()
                if (esNum.isNotEmpty()) return esNum.max()
                val nums = patronDecimal.findAll(zona)
                    .mapNotNull { it.value.replace(",", ".").toDoubleOrNull() }
                    .filter { it > 1.0 }
                if (nums.toList().isNotEmpty()) return nums.max()
            }
        }
        return 0.0
    }

    // Unidades

    private val mapaUnidades = mapOf(
        "l" to "L","lt" to "L","lts" to "L","litro" to "L","litros" to "L",
        "ml" to "ml","cl" to "cl",
        "kg" to "kg","kilo" to "kg","kilos" to "kg",
        "g" to "g","gr" to "g","gramo" to "g","gramos" to "g",
        "ud" to "ud","uds" to "ud","unidad" to "ud","unidades" to "ud",
        "bl" to "ud","bi" to "ud","u" to "ud","ta" to "ud","ds" to "ud","bote" to "ud","botella" to "ud","botellas" to "ud","bt" to "ud",
        "cb" to "ud","cj" to "ud","caja" to "ud","cajas" to "ud",
        "pq" to "ud","pack" to "ud","packs" to "ud",
        "es" to "ud","estuche" to "ud","estuches" to "ud",
        "bo" to "ud","gf" to "ud","ph" to "ud","pz" to "ud","m" to "ud"
    )

    private val patronUnidadDesc = Regex(
        """(?i)\b(\d+(?:[.,]\d+)?)\s*(kg|g|gr|l|lt|lts|litros?|ml|cl|ud|uds|unidades?|bote|botellas?|bl|bt|caja|cajas|cb|cj|pack|packs|pq|estuches?|es|pz)\b"""
    )

    private fun adivinarUnidad(texto: String): String {
        val m = patronUnidadDesc.find(texto) ?: return "ud"
        val unidadRaw = mapaUnidades[m.groupValues[2].lowercase()] ?: "ud"
        val cantidad  = m.groupValues[1].replace(",", ".").toDoubleOrNull() ?: 1.0

        // cl y ml son tamaños de envase, NO la unidad de venta (una botella se compra por "ud")
        // Solo usamos L si la cantidad es ≥ 1 litro (producto a granel: aceite 5L, etc.)
        return when {
            unidadRaw == "ml"            -> "ud"
            unidadRaw == "cl"            -> "ud"
            unidadRaw == "L" && cantidad < 1.0  -> "ud"  // 0.33L, 0.5L → botella = ud
            else -> unidadRaw
        }
    }

    private fun normalizarUnidad(raw: String): String =
        mapaUnidades[raw.lowercase().trim()] ?: raw.uppercase()

    /**
     * Extrae el tamano de envase del nombre del producto (250 GR, 25 LT, 1 KG...).
     * Devuelve null si no hay unidad convertible (botellas en cl/ml, unidades, etc.).
     * Usado en parseSercodi para convertir "16 packs de 250g" → "4000 g".
     */
    private data class InfoPack(val tamano: Double, val unidad: String)

    private fun extraerInfoPack(desc: String): InfoPack? {
        val m = patronUnidadDesc.find(desc) ?: return null
        val tamano     = m.groupValues[1].replace(",", ".").toDoubleOrNull() ?: return null
        val unidadRaw  = mapaUnidades[m.groupValues[2].lowercase()] ?: return null
        val unidadFinal = when {
            unidadRaw == "ud"                    -> return null  // ya es unidad, sin conversion
            unidadRaw == "ml"                    -> return null  // ml = envase pequeno = ud
            unidadRaw == "cl"                    -> return null  // cl = botella = ud
            unidadRaw == "L" && tamano < 1.0     -> return null  // 0,33L / 0,5L = botella = ud
            else                                 -> unidadRaw    // g, kg, L >= 1 -> convertir
        }
        return InfoPack(tamano, unidadFinal)
    }

    // Limpieza descripción

    private fun limpiarDesc(desc: String): String = desc
        .replace(Regex("""(?i)\b\d+\s*(kg|g|gr|l|lt|ml|cl|ud|uds|bl|bt|pq|es)\b"""), "")
        .replace(Regex("""\b\d{8,14}\b"""), "")   // EANs numericos
        .replace(Regex("""(?i)\b(ref|lote|cad\.?)\b[:\s]?\S*"""), "")
        .replace(Regex("""\s+\d[\d,.]*(\s*[-x]\s*\d[\d,.]*)?\s*$"""), "")  // numeros sueltos al final (precios/cantidades filtrados)
        .replace(Regex("""\s{2,}"""), " ")
        .trim()

    // Líneas a ignorar

    // Patron anclado al INICIO de la línea: cabeceras, encabezados
    private val ignorarPatron = Regex(
        """(?i)^\s*(lote|cad|n\.?reg|bc\s+gtin|gtin|ean|num\.|n[úu]mero|n[oº]|total|subtotal|base|importe iva|iva|dto|descuento|recargo|factura|albar[áa]n|nota|observa|compra|envases|pagina|p[áa]gina|fecha|cliente|entregado|fin de|n[úu]mero de ped""" +
                """|avda|avd\.|calle|c/\s|tel\.|fax|cif|nif|registro|datos fiscal|datos comercial""" +
                """|bf\s+\d|hora:|cod\.?\s+cle|num\.?\s+factura|p\s+gina|\d{1,2}:\d{2}:\d{2}|cod\.\s*ean|c[óo]digo|descripci[óo]n""" +
                """|www\.|\.com|\.es|@""" +
                """|firmado|fimago|recib[íi]|conforme|observaci|no se a[cd][ep]tan|devoluci|atencion|atenci[óo]n|bultos)"""
    )

    // Patron NO anclado: ciudades, teclado visible en la foto, textos legales
    private val ignorarContenido = Regex(
        """(?i)\b(motril|almu[ñn]ecar|m[áa]laga|granada|c[óo]rdoba|almer[íi]a|sevilla|v[ée]lez)\b""" +
                """|\b(alt\s*(gr|or)|control|gontol|gontrol|may[úu]s|may[úu]b|copia)\b""" +
                """|\b(gesturan|playa\s*cabria|casa\s*antonio)\b""" +
                """|\b(bruto|dsto|imponible|r\.?\s*equiv|albaran|aibaran)\b"""
    )

    private fun esIgnorada(l: String) =
        ignorarPatron.containsMatchIn(l) || ignorarContenido.containsMatchIn(l) || l.length < 5


    // Extracción de ítems

    private fun extraerItems(lineas: List<String>, tipo: TipoAlbaran): List<ItemAlbaran> {
        val items = mutableListOf<ItemAlbaran>()

        // Fix huerfanas Sercodi:
        var huerfana: String? = null
        val regexCodigoSercodi = Regex("""^.{0,8}(?<!\d)\d{6}(?!\d)""")

        // Fix filas partidas Campoy:
        // OCR separa [código+desc] y [números] en filas distintas
        var pendienteCampoy: String? = null

        for (l in lineas) {
            if (esIgnorada(l)) continue

            if (tipo == TipoAlbaran.SERCODI) {
                val tieneCodigo = regexCodigoSercodi.containsMatchIn(l)
                val esHuerfana = !tieneCodigo &&
                        patronDecimal.containsMatchIn(l) &&
                        !Regex("""\d{8,}""").containsMatchIn(l)

                if (esHuerfana) {
                    huerfana = l
                    continue
                }

                val lineaFinal = if (tieneCodigo && huerfana != null) {
                    val h = huerfana!!
                    huerfana = null
                    "$l  $h"
                } else {
                    huerfana = null
                    l
                }

                val item = parseSercodi(lineaFinal)
                if (item != null && item.precioUnitario >= 0 && item.descripcion.length > 2) {
                    items += item
                }
            } else if (tipo == TipoAlbaran.CAMPOY) {
                // Fix filas partidas Campoy:
                // OCR separa [código+desc] y [números] en filas distintas:
                //   Fila[N]:   "200014  CALAMAR TROCEADO 6X1KG"    (sin decimales)
                //   Fila[N+1]: "42232  1  1  18,00  5,95  10  107,10" (solo números)
                val tieneCodCampoy = codCampoy.containsMatchIn(l) || esProdCampoy.containsMatchIn(l)
                val numDecimales = patronDecimal.findAll(l).count()

                if (tieneCodCampoy && numDecimales < 2 && pendienteCampoy == null) {
                    pendienteCampoy = l
                    continue
                }

                val lineaFinal: String
                if (pendienteCampoy != null && !codCampoy.containsMatchIn(l)) {
                    // Fila de números sin código → fusionar con la pendiente
                    lineaFinal = "${pendienteCampoy!!}  $l"
                    pendienteCampoy = null
                } else {
                    // Nueva fila con código → parsear la pendiente sola primero
                    if (pendienteCampoy != null) {
                        val itemPend = parseCampoy(pendienteCampoy!!)
                        if (itemPend != null && itemPend.descripcion.length > 2) items += itemPend
                    }
                    pendienteCampoy = null
                    lineaFinal = l
                }

                val item = parseCampoy(lineaFinal)
                if (item != null && item.precioUnitario >= 0 && item.descripcion.length > 2) {
                    items += item
                }
            } else {
                val item = when (tipo) {
                    TipoAlbaran.MAKRO    -> parseMakro(l)
                    TipoAlbaran.PARIS    -> parseParis(l)
                    TipoAlbaran.SORBITO  -> parseSorbito(l)
                    TipoAlbaran.COVIGRAN -> parseCovigran(l)
                    else                 -> parseGenerico(l)
                }
                if (item != null && item.precioUnitario >= 0 && item.descripcion.length > 2) {
                    items += item
                }
            }
        }
        // Flush pendiente Campoy (ultimo producto sin fila de numeros)
        if (pendienteCampoy != null) {
            val itemPend = parseCampoy(pendienteCampoy!!)
            if (itemPend != null && itemPend.descripcion.length > 2) items += itemPend
        }
        return items.distinctBy { it.descripcion }
    }

    // Helpers numéricos

    private val ivaValues = setOf(4.0, 5.0, 10.0, 21.0)
    private val patronEAN = Regex("""\b\d{8,14}\b""")

    private fun decimales(texto: String): List<Double> =
        patronDecimal.findAll(texto)
            .mapNotNull { it.value.replace(",", ".").toDoubleOrNull() }
            .toList()

    private fun sinIva(nums: List<Double>): List<Double> {
        val list = nums.toMutableList()
        for (i in (maxOf(0, list.size - 3)..list.lastIndex).reversed()) {
            if (list[i] in ivaValues) { list.removeAt(i); break }
        }
        return list
    }

    // SERCODI
    // [5-7d código] [descripción] [13d EAN] [bultos] [cantidad] [precio] [importe] [IVA%] [PVP/ud]

    // Permite texto antes del código (márgenes laterales que ML Kit puede leer junto a la línea)
    private val codSercodi = Regex("""(?:^|\s)(\d{5,7})\s+""")

    // ML Kit lee Sercodi con código+descripción en una línea y EAN en la siguiente.
    // Los precios aparecen en bloques separados al final — no están en la misma línea.
    // Patrón flexible: el código puede tener 1-3 caracteres de ruido antes (columnas adyacentes).
    private val prodPatternSercodi = Regex(
        """^.{0,4}(\d{5,6})\s*[^\d\s]?\s+([A-ZÁÉÍÓÚÑ][A-Za-záéíóúñ0-9\s.,/\'-]{4,})"""
    )

    private fun parseSercodi(linea: String): ItemAlbaran? {
        // Sercodi usa EXACTAMENTE 6 dígitos — excluye códigos postales (5d) y nº factura (7d)
        // (?<!\d) y (?!\d) en lugar de \b: permiten "610782QUESo" (OCR sin espacio)
        // sin capturar fragmentos de codigos de 7 digitos (1234567 → 234567 se rechaza
        // porque 2 esta precedido de digito)
        // dropWhile salta separadores (espacio, |, /) entre codigo y descripcion
        val mcCodigo = Regex("""^.{0,8}(?<!\d)(\d{6})(?!\d)""").find(linea) ?: return null
        val restoSinCodigo = linea.substring(mcCodigo.range.last + 1)
            .dropWhile { !it.isLetterOrDigit() }

        // Ahora con la reconstrucción de filas, el EAN está en la misma línea.
        // Usamos el EAN como separador: antes del EAN = descripción, después = números.
        val ean = patronEAN.find(restoSinCodigo)
        val desc: String
        val zonaNum: String

        if (ean != null && ean.range.first > 3) {
            desc    = restoSinCodigo.substring(0, ean.range.first).trim()
            zonaNum = restoSinCodigo.substring(ean.range.last + 1)
        } else {
            // Sin EAN: cortar antes del primer número decimal
            val primerDecimal = patronDecimal.find(restoSinCodigo)?.range?.first ?: restoSinCodigo.length
            desc    = restoSinCodigo.substring(0, primerDecimal).trim()
            zonaNum = if (primerDecimal < restoSinCodigo.length) restoSinCodigo.substring(primerDecimal) else ""
        }

        if (desc.length < 3) return null

        // CANTIDAD en Sercodi es siempre número redondo: 1,00 · 2,00 · 12,00 · 24,00
        // PRECIO nunca es redondo: 12,728 · 35,289 · 2,399
        // → buscamos el primer número cuya parte decimal sea ~0 (número entero representado con decimales)
        val nums = sinIva(decimales(zonaNum))
        // Umbral 0.001 (no 0.01): evita que precios como 6,990 pasen por "redondo"
        // ya que abs(6.99 - 7.0) = 0.00999... < 0.01 en IEEE 754 → falso positivo
        val cantidad = nums.firstOrNull { n ->
            n in 0.1..999.0 && kotlin.math.abs(n - kotlin.math.round(n)) < 0.001
        } ?: 1.0   // si no hay número redondo, defecto 1

        // PRECIO = último número = PVP/ud con IVA incluido (lo que realmente se paga)
        // La columna "Precio" sin IVA (2,399) queda descartada; nos interesa el coste real
        val precio = nums.lastOrNull()?.takeIf { it > 0 } ?: 0.0

        // Conversión de pack: si el nombre indica tamaño (250 GR, 25 LT, 1 KG...),
        // transformar "N packs × tamaño" → cantidad total en unidad base con precio/unidad.
        // Ejemplo: 16 packs de 250g a 1,099€/pack → 4000 g a 0,004396 €/g
        // Botellas (cl, ml, <1L) y unidades NO se convierten → quedan como "ud".
        val infoPack = extraerInfoPack(desc)
        val cantidadFinal: Double
        val unidadFinal: String
        val precioFinal: Double
        if (infoPack != null) {
            cantidadFinal = cantidad * infoPack.tamano
            unidadFinal   = infoPack.unidad
            precioFinal   = precio / infoPack.tamano
        } else {
            cantidadFinal = cantidad
            unidadFinal   = adivinarUnidad(desc)
            precioFinal   = precio
        }

        val item = ItemAlbaran(limpiarDesc(desc), cantidadFinal, unidadFinal, precioFinal)
        Log.d("ChefCore_OCR", "  SERCODI cant=${item.cantidad} ${item.unidad} precio=${item.precioUnitario} desc=${item.descripcion}")
        return item
    }

    // MAKRO
    // Líneas ya unidas: [6d/13d código] [descripción] [UNIDAD] [Prec.Ud] [ContP] [Precio] [Cant] [Importe] [ImpIVA]
    // IMPORTANTE: patronUnitMakro sin IGNORE_CASE para no matchear "kg" dentro de descripciones
    // como "0,8-1 kg origen Grecia". La columna real siempre es mayúscula (KG, BL, UD...).

    private val codMakroEan  = Regex("""^\d{12,14}\s+""")
    private val codMakro6    = Regex("""^#?\d{5,7}\s+""")
    private val patronUnitMakro = Regex("""\s+(KG|BL|BI|U|ES|BO|GF|PH|PZ|BT|UD|CB|CJ|PQ|M|TA|DS)\s+""")  // SIN ignore_case

    // Prefijos de marca que Makro añade a sus productos propios
    private val prefijosMakro = Regex("""(?i)^(ma\s*kro|hakro|metro)\s+(chef|professi\s*onal|pro|premi\s*um)\s+|(ma\s*kro|hakro|metro)\s+""")

    private fun limpiarDescMakro(desc: String): String =
        desc.replace(prefijosMakro, "").trim()

    private fun parseMakro(linea: String): ItemAlbaran? {
        // Normalizar espacios en decimales: "11, 340" -> "11,340"
        val lineaN = linea.replace(Regex("""(\d),\s+(\d)"""), "$1,$2")

        // -- Variante A: EAN 12-14 digitos ----------------------------------------
        val meEan = codMakroEan.find(lineaN)
        if (meEan != null) {
            val resto  = lineaN.substring(meEan.range.last + 1)
            val mu     = patronUnitMakro.find(resto)
            val desc   = (if (mu != null) resto.substring(0, mu.range.first) else resto).trim()
            val unidad = mu?.let { normalizarUnidad(it.groupValues[1]) } ?: adivinarUnidad(desc)
            val zona   = mu?.let { resto.substring(it.range.last + 1) } ?: resto
            val nums   = decimales(zona)
            if (nums.size < 2) return null
            val cantidad = if (unidad == "kg") nums.getOrNull(1) ?: 1.0
            else Math.round(nums.last() / nums[0]).toDouble().takeIf { it > 0 } ?: 1.0
            return ItemAlbaran(limpiarDescMakro(limpiarDesc(desc)), cantidad, unidad, nums[0])
        }

        // -- Variante B: codigo de 6 digitos --------------------------------------
        val me6   = codMakro6.find(lineaN) ?: return null
        val resto = lineaN.substring(me6.range.last + 1)

        val muMatch = patronUnitMakro.find(resto)

        // -- Fallback: OCR no detecto la unidad (Servilleta, etc.) ----------------
        if (muMatch == null) {
            val numStart = Regex("""\b\d+[.,]\d+""").find(resto)?.range?.first ?: return null
            val descFb   = resto.substring(0, numStart).trim()
            if (descFb.length < 3) return null
            val numsFb   = decimales(resto.substring(numStart))
            if (numsFb.isEmpty()) return null
            val unidadFb = adivinarUnidad(descFb)
            val precFb   = numsFb[0]
            val impFb    = numsFb.last()
            val prcFb    = numsFb.getOrElse(1) { precFb }
            val cantFb   = when {
                unidadFb == "kg"  -> numsFb.getOrNull(1) ?: 1.0
                prcFb > 0         -> Math.round(impFb / prcFb).toDouble().takeIf { it > 0 } ?: 1.0
                else              -> 1.0
            }
            return ItemAlbaran(limpiarDescMakro(limpiarDesc(descFb)), cantFb, unidadFb, precFb)
        }

        // -- Camino normal: unidad detectada --------------------------------------
        val desc    = resto.substring(0, muMatch.range.first).trim()
        val unidad  = normalizarUnidad(muMatch.groupValues[1])
        val zonaNum = resto.substring(muMatch.range.last + 1)
        val nums    = decimales(zonaNum)
        if (nums.isEmpty()) return null
        val precUd  = nums[0]

        val cantidad = if (unidad == "kg") {
            nums.getOrNull(1) ?: 1.0
        } else {
            // BL/UD: importe a veces aparece en la fila de descripcion (OCR merge Y similar)
            // Si zonaNum.last aprox precUd, buscar importe real en desc
            val importeZona = nums.last()
            val importeReal = if (importeZona > precUd * 1.15) {
                importeZona
            } else {
                decimales(desc).lastOrNull()?.takeIf { it > precUd } ?: importeZona
            }
            val precio = nums.getOrElse(1) { precUd }
            if (precio > 0) Math.round(importeReal / precio).toDouble().takeIf { it > 0 } ?: 1.0
            else 1.0
        }

        return ItemAlbaran(limpiarDescMakro(limpiarDesc(desc)), cantidad, unidad, precUd)
    }

    // CAMPOY
    // Formato: [5-6d código]  [descripción NxMKG]  [lote]  [Cajas]  [Pzas]  [Ud/Kg]  [Precio]  [%IVA]  [Total]
    // Columnas clave:
    //   Ud/Kg  = total kg recibido (siempre en kg, independientemente de Cajas/Pzas)
    //   Precio = precio neto €/kg sin IVA
    //   Total  = Ud/Kg × Precio  (base imponible del item)
    //   %IVA   = entero (10, 4...) — NO lo captura patronDecimal → siempre 3 decimales reales
    // Precio final = Precio × (1 + %IVA/100)  ó  (Total/Ud/Kg) × (1 + %IVA/100)
    // Unidad siempre "kg" (pescados/carnes se usan por peso en escandallo)

    private val codCampoy = Regex("""^\d{5,7}\s+""")

    // Sin código: detectar línea de producto por notación NxM (1X1, 6X1KG...) o unidad de peso
    private val esProdCampoy = Regex("""(?i)\d[Xx]\d|KGS?\b|GRS?\b|LTS?\b|MLS?\b""")

    private fun parseCampoy(linea: String): ItemAlbaran? {
        val mc = codCampoy.find(linea)
        val resto = if (mc != null) linea.substring(mc.range.last + 1) else linea

        // Sin código: exigir patrón de producto (NxM o unidad de peso)
        if (mc == null && !esProdCampoy.containsMatchIn(linea)) return null

        // Descripción: primer token (antes de 2+ espacios)
        val desc = resto.split(Regex("""\s{2,}""")).firstOrNull()?.trim() ?: return null
        if (desc.length < 3) return null

        // Preprocesar errores OCR de comas
        // OCR confunde comas con espacios: "32 04" → "32,04", "34, 80" → "34,80"
        val restoPreprocesado = resto
            .replace(Regex(""",\s+(\d)"""), ",$1")                // "34, 80" → "34,80"
            .replace(Regex("""(\d)\s(\d{2})(?!\d)"""), "$1,$2")   // "32 04" → "32,04"
            .replace(Regex("""(\d)\s(\d{3})(?!\d)"""), "$1,$2")   // "107 10" → "107,10" (tres decimales)

        // Decimales reales de la línea: siempre [Ud/Kg, Precio, Total]
        val nums = decimales(restoPreprocesado)

        // Detectar %IVA como entero en la línea (10 → entre Precio y Total)
        val ivaPorc = Regex("""\b(4|10|21)\b""").findAll(restoPreprocesado)
            .lastOrNull { m ->
                val i = m.range.first
                (i == 0 || restoPreprocesado[i - 1] == ' ') &&
                        (m.range.last + 1 >= restoPreprocesado.length || restoPreprocesado[m.range.last + 1] == ' ')
            }?.groupValues?.get(1)?.toDoubleOrNull() ?: 10.0

        val multiplicador = 1.0 + ivaPorc / 100.0

        return when {
            nums.size >= 3 -> {
                // Caso ideal: Ud/Kg | Precio | Total
                val udKg       = nums[nums.size - 3]
                val precioNeto = nums[nums.size - 2]
                if (udKg <= 0) return null
                ItemAlbaran(limpiarDesc(desc), udKg, "kg", precioNeto * multiplicador)
            }
            nums.size == 2 -> {
                // 2 decimales: puede ser [Ud/Kg, Precio] o [Ud/Kg, Total]
                // Heurística: si segundo < primero → es Precio (€/kg < kg); si no → es Total
                val udKg   = nums[0]
                val second = nums[1]
                if (udKg <= 0) return null
                val precioConIva = if (second < udKg) {
                    second * multiplicador             // [Ud/Kg, Precio]
                } else {
                    (second / udKg) * multiplicador    // [Ud/Kg, Total]
                }
                ItemAlbaran(limpiarDesc(desc), udKg, "kg", precioConIva)
            }
            nums.size == 1 -> {
                // Solo Ud/Kg disponible: precio desconocido
                val udKg = nums[0]
                if (udKg <= 0) return null
                ItemAlbaran(limpiarDesc(desc), udKg, "kg", 0.0)
            }
            else -> {
                // Sin columnas numéricas: inferir de NxMKG en nombre
                val pack = Regex("""(?i)(\d+)[Xx](\d+[.,]?\d*)\s*(KG|GR?|LT?|ML)""").find(desc)
                if (pack != null) {
                    val n = pack.groupValues[1].toDoubleOrNull() ?: 1.0
                    val m = pack.groupValues[2].replace(",", ".").toDoubleOrNull() ?: 1.0
                    val u = normalizarUnidad(pack.groupValues[3])
                    ItemAlbaran(limpiarDesc(desc), n * m, u, 0.0)
                } else {
                    ItemAlbaran(limpiarDesc(desc), 1.0, "kg", 0.0)
                }
            }
        }
    }

    // PARIS / JUAREZ CABRERA
    // [4-5d código] [descripción] [lote alfanum LE220] [Plt.Caj] [Und] [Tot.Unds] [Precio] [IVA%] [Total]
    // Decimales: [Tot.Unds, Precio, IVA%(21.00), Total]

    private val codParis = Regex("""^\d{3,6}\s+""")
    private val ignorarParis = Regex("""(?i)\bportes?\b""")  // gastos de envío

    private fun parseParis(linea: String): ItemAlbaran? {
        if (ignorarParis.containsMatchIn(linea)) return null
        val mc = codParis.find(linea) ?: return null
        val resto = linea.substring(mc.range.last + 1)
        val desc = resto.split(Regex("""\s{2,}""")).firstOrNull()?.trim() ?: return null
        val nums = sinIva(decimales(resto))
        if (nums.size < 2) return null
        val total    = nums.last()
        val precio   = nums.getOrElse(nums.size - 2) { nums.last() }
        val cantidad = if (nums.size >= 3) nums.first() else 1.0
        return ItemAlbaran(limpiarDesc(desc), cantidad, adivinarUnidad(desc), precio)
    }

    // SORBITO A SORBITO
    // Vinos y bebidas. Formato:
    // [4-6d ref]  [Descripción]  [Cantidad(entero)]  [Precio]  [Dcto%]  [Importe]
    // Cantidad = entero (botellas) → no lo captura patronDecimal
    // Precio = neto sin IVA; Dcto% puede existir o estar a 0
    // IVA = siempre 21% (vinos en España) — aparece solo en el pie, no en la línea
    // Precio final = (Importe / Cantidad) × 1.21  ← absorbe el descuento automáticamente
    // Ejemplo: 17006  VIUDA NEGRA CRIANZA 2020  36  6,29  5,00  215,12
    //   → importe=215.12, precio=6.29, dcto=5% → precioNeto=5.9755 → cant=36 → precio×IVA=7.23

    // SORBITO A SORBITO
    // Vinos y bebidas. Formato:
    // [4-6d ref]  [Descripción]  [Cantidad(entero)]  [Precio]  [Dcto%]  [Importe]
    //
    // Prioridad de cálculo del precio final:
    //   1. Importe disponible  → (Importe / Cantidad) × 1.21
    //      (absorbe descuento ya aplicado, más preciso)
    //   2. Solo Precio + Dcto% → Precio × (1 - Dcto/100) × 1.21
    //      (fallback cuando OCR pierde la columna Importe)
    //
    // Cantidad: se lee directamente del token entero tras la descripción.
    // Si OCR lo pierde, se deriva de Importe/precioNeto.
    // IVA siempre 21% (vinos/bebidas en España, no aparece en la línea).
    //
    // Ejemplo: 17006  VIUDA NEGRA CRIANZA 2020  36  6,29  5,00  215,12
    //   decimales=[6.29, 5.00, 215.12] → cant=36 → precio=(215.12/36)×1.21=7.23€/ud

    private val codSorbito = Regex("""^\d{4,6}\s+""")

    private fun parseSorbito(linea: String): ItemAlbaran? {
        val mc = codSorbito.find(linea) ?: return null
        val resto = linea.substring(mc.range.last + 1)

        val tokens = resto.split(Regex("""\s{2,}"""))
        val desc = tokens.firstOrNull()?.trim() ?: return null
        if (desc.length < 3) return null

        // Cantidad: primer token entero (sin coma/punto) después de la descripción
        val cantidadDirecta = tokens.drop(1)
            .firstOrNull { it.trim().matches(Regex("""\d+""")) }
            ?.trim()?.toDoubleOrNull()

        // Decimales: [Precio, Dcto%, Importe] o subconjunto si OCR pierde alguno
        val nums = decimales(resto)
        if (nums.size < 1) return null

        // Identificar columnas por posición (de derecha a izquierda es más fiable):
        //   Última  = Importe
        //   Penúltima = Dcto%
        //   Antepenúltima = Precio
        val importe = if (nums.size >= 3) nums.last() else null
        val dcto    = if (nums.size >= 2) nums[nums.size - 2] else 0.0
        val precio  = nums.first()

        val precioNeto = precio * (1.0 - dcto / 100.0)

        val importeVal  = importe ?: 0.0          // Double no nullable para operar

        // Cantidad: directa del token entero, o derivada del importe si no se leyó
        val cantidad: Double = cantidadDirecta
            ?: if (importeVal > 0 && precioNeto > 0)
                Math.round(importeVal / precioNeto).toDouble().takeIf { it > 0.0 } ?: 1.0
            else 1.0

        val precioFinal: Double = if (importeVal > 0 && cantidad > 0) {
            importeVal / cantidad
        } else {
            precioNeto
        }

        return ItemAlbaran(limpiarDesc(desc), cantidad, "ud", precioFinal)
    }
    // [6d CDAR] [referencia alfanum Ma-P21] [descripción] [Cajas] [Uds] [Precio] [Neto] [Importe] [%IVA]
    // Decimales: [Precio, Neto, Importe]  — Uds es entero

    private val codCovigran = Regex("""^\d{5,7}\s+""")

    private fun parseCovigran(linea: String): ItemAlbaran? {
        val mc = codCovigran.find(linea) ?: return null
        val resto = linea.substring(mc.range.last + 1)
        // Quitar la referencia alfanum inicial (Ma-P21, etc.)
        val sinRef = resto.replace(Regex("""^[A-Za-z]{1,4}-?[A-Za-z0-9]{1,6}\s+"""), "")
        val desc = sinRef.split(Regex("""\s{2,}""")).firstOrNull()?.trim()
            ?: sinRef.split(" ").take(5).joinToString(" ")
        val nums = sinIva(decimales(linea))
        if (nums.size < 2) return null
        // [Precio, Neto, Importe] → precio=1º, importe=último
        val importe = nums.last()
        val precio  = nums.first()
        // Cantidad = Importe / Precio (redondeado)
        val cantidad = if (precio > 0) Math.round(importe / precio).toDouble().takeIf { it > 0 } ?: 1.0 else 1.0
        return ItemAlbaran(limpiarDesc(desc), cantidad, adivinarUnidad(desc), precio)
    }

    // GENÉRICO (Sorbito a Sorbito, Valle Aguirre, Cooperativa, …)

    private val codGenerico = Regex("""^\d{2,8}\s+""")

    private fun parseGenerico(linea: String): ItemAlbaran? {
        val mc = codGenerico.find(linea) ?: return null
        val resto = linea.substring(mc.range.last + 1)
        val nums = sinIva(decimales(resto))
        if (nums.size < 2) return null
        val primerNum = patronDecimal.find(resto)?.range?.first ?: resto.length
        val desc = resto.substring(0, primerNum).trim().ifBlank { limpiarDesc(resto) }
        val total    = nums.last()
        val precio   = nums.getOrElse(nums.size - 2) { nums.last() }
        val cantidad = if (nums.size >= 3) nums.first() else 1.0
        return ItemAlbaran(limpiarDesc(desc), cantidad, adivinarUnidad(desc), precio)
    }
}