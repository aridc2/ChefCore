package es.chefcore.app.logic

import es.chefcore.app.data.database.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VoiceCommander(
    private val iDao:  IngredienteDao,
    private val rDao:  RecetaDao,
    private val aDao:  AlbaranDao,
    private val scope: CoroutineScope
) {
    private val cocinaManager = CocinaManager(iDao, rDao)
    private val _recetaActiva = MutableStateFlow("")
    val recetaActiva: StateFlow<String> = _recetaActiva.asStateFlow()

    fun ejecutarComando(texto: String, onFeedback: (String) -> Unit) {
        val t = normalizar(texto)
        scope.launch {
            when {

                // CONTEXTO: cerrar / salir
                t.startsWith("cerrar receta") || t.startsWith("salir") ||
                        t.startsWith("terminar receta") || t == "cancelar" -> {
                    if (_recetaActiva.value.isEmpty()) {
                        onFeedback("No hay ninguna receta abierta")
                    } else {
                        val nombre = _recetaActiva.value
                        _recetaActiva.value = ""
                        onFeedback("Receta '$nombre' cerrada")
                    }
                }

                // CONTEXTO: abrir receta existente
                t.startsWith("abrir") || t.startsWith("abre") || t.startsWith("editar receta") -> {
                    val nombre = t.quitarPrefijos(
                        "abrir la receta de", "abrir la receta", "abrir receta de", "abrir receta",
                        "abre la receta de",  "abre la receta",  "abre receta de",  "abre receta",
                        "editar la receta de","editar la receta", "editar receta de","editar receta"
                    )
                    val receta = rDao.buscarPorNombreIgnorandoCase(nombre)
                    if (receta != null) {
                        _recetaActiva.value = nombre
                        onFeedback("NAV_DETALLE|$nombre")
                    } else {
                        onFeedback("AVISO: No existe la receta '$nombre'")
                    }
                }

                // NUEVO: receta
                t.startsWith("nueva receta") || t.startsWith("nuevo receta") ||
                        t.startsWith("crea receta") || t.startsWith("crear receta") -> {
                    val nombre = t.quitarPrefijos(
                        "nueva receta de", "nueva receta", "nuevo receta de", "nuevo receta",
                        "crea receta de",  "crea receta",  "crear receta de", "crear receta"
                    )
                    if (nombre.isBlank()) { onFeedback("AVISO: Di nueva receta de [nombre]"); return@launch }
                    val nomCap = nombre.replaceFirstChar { it.uppercase() }
                    if (rDao.buscarPorNombre(nomCap) == null) rDao.insertar(Receta(nombre = nomCap))
                    _recetaActiva.value = nomCap
                    onFeedback("NAV_DETALLE|$nomCap")
                }

                // NUEVO: ingrediente
                t.startsWith("nuevo") -> {
                    val nombre = t.quitarPrefijos("nuevo").replaceFirstChar { it.uppercase() }
                    if (nombre.isBlank()) { onFeedback("AVISO: Di nuevo [nombre]"); return@launch }
                    if (iDao.buscarPorNombreIgnorandoCase(nombre) != null) {
                        onFeedback("AVISO: '$nombre' ya existe. Usa 'anadir' para sumar stock.")
                    } else {
                        iDao.insertar(Ingrediente(nombre = nombre, cantidad = 0.0, unidad = "ud", precio = 0.0))
                        onFeedback("OK: Creado $nombre")
                    }
                }

                // ANADIR: paso a receta
                t.startsWith("anadir paso") || t.startsWith("agregar paso") -> {
                    val contenido = t.quitarPrefijos("anadir paso", "agregar paso")
                        .replaceFirstChar { it.uppercase() }
                    if (_recetaActiva.value.isEmpty()) {
                        onFeedback("AVISO: Primero di nueva receta de [nombre]"); return@launch
                    }
                    val receta = rDao.buscarPorNombreIgnorandoCase(_recetaActiva.value)
                    if (receta != null) {
                        val nuevas = if (receta.instrucciones.isEmpty()) contenido
                        else receta.instrucciones + "\n" + contenido
                        rDao.actualizar(receta.copy(instrucciones = nuevas))
                        onFeedback("Paso: $contenido -> ${_recetaActiva.value}")
                    } else onFeedback("AVISO: No se encontro la receta activa")
                }

                // ANADIR: ingrediente al escandallo de la receta
                t.startsWith("anadir ingrediente") || t.startsWith("agregar ingrediente") -> {
                    val resto = t.quitarPrefijos("anadir ingrediente", "agregar ingrediente")
                    if (_recetaActiva.value.isEmpty()) {
                        onFeedback("AVISO: Primero di nueva receta de [nombre]"); return@launch
                    }
                    val receta = rDao.buscarPorNombreIgnorandoCase(_recetaActiva.value)
                    if (receta == null) { onFeedback("AVISO: Receta no encontrada"); return@launch }
                    val ing = VoiceParser.parsearIngrediente(resto)
                    if (ing.nombre.isBlank()) {
                        onFeedback("AVISO: Di anadir ingrediente [cantidad] [unidad] de [nombre]"); return@launch
                    }
                    val ingrediente = iDao.buscarPorNombreIgnorandoCase(ing.nombre)
                    if (ingrediente == null) {
                        onFeedback("AVISO: '${ing.nombre}' no esta en inventario. Crealo con nuevo ${ing.nombre}")
                        return@launch
                    }
                    val relacionActual = rDao.obtenerIngredientesStatic(receta.id)
                        .firstOrNull { it.ingredienteId == ingrediente.id }
                    val cantidadFinal = (relacionActual?.cantidadNecesaria ?: 0.0) + ing.cantidad
                    rDao.asociarIngredienteConReemplazo(
                        RecetaIngrediente(receta.id, ingrediente.id, cantidadFinal)
                    )
                    onFeedback("OK: ${ingrediente.nombre} -> $cantidadFinal ${ingrediente.unidad} en '${_recetaActiva.value}'")
                }

                // ANADIR: stock de ingrediente
                t.startsWith("anadir") || t.startsWith("agregar") -> {
                    val resto = t.quitarPrefijos("anadir", "agregar")
                    val ing = VoiceParser.parsearIngrediente(resto)
                    if (ing.nombre.isBlank()) { onFeedback("AVISO: Di anadir [cantidad] [unidad] de [nombre]"); return@launch }
                    if (ing.precio > 0.0) {
                        val precioTotal = ing.cantidad * ing.precio
                        val resultado = cocinaManager.registrarEntradaStock(ing.nombre, ing.cantidad, ing.unidad, precioTotal)
                        when (resultado) {
                            is RegistroStockResult.NuevoIngrediente     -> onFeedback("OK: ${resultado.ingrediente.nombre} ${ing.cantidad} ${resultado.ingrediente.unidad} a ${ing.precio}/u")
                            is RegistroStockResult.StockActualizado     -> onFeedback("PMP: ${resultado.ingrediente.nombre} precio ${String.format("%.2f", resultado.pmpAnterior)} -> ${String.format("%.2f", resultado.pmpNuevo)}")
                            is RegistroStockResult.ErrorIncompatible    -> onFeedback("AVISO: Unidad incompatible. ${resultado.ingredienteExistente.nombre} usa '${resultado.ingredienteExistente.unidad}'")
                            is RegistroStockResult.Error                -> onFeedback("AVISO: ${resultado.mensaje}")
                            is RegistroStockResult.PosibleDuplicado     -> onFeedback("OK: fusionado con ${resultado.candidato.nombre}")
                        }
                    } else {
                        val existente = iDao.buscarPorNombreIgnorandoCase(ing.nombre)
                        if (existente != null) {
                            iDao.actualizar(existente.copy(
                                cantidad = existente.cantidad + ing.cantidad,
                                unidad   = if (ing.unidad != "ud") ing.unidad else existente.unidad
                            ))
                            onFeedback("Sumado: ${existente.nombre} +${ing.cantidad} ${existente.unidad}")
                        } else {
                            iDao.insertar(ing)
                            onFeedback("OK: ${ing.nombre} ${ing.cantidad} ${ing.unidad}")
                        }
                    }
                }

                // MODIFICAR: precio de venta de una receta
                t.startsWith("modificar precio receta") -> {
                    val resto = t.quitarPrefijos("modificar precio receta de", "modificar precio receta")
                    val partes = resto.split(Regex("\\s+a\\s+|\\s+a(?=\\d)"))
                    if (partes.size < 2) { onFeedback("AVISO: Di modificar precio receta [nombre] a [valor]"); return@launch }
                    val nombre = partes[0].trim().replaceFirstChar { it.uppercase() }
                    val valor  = parsearValor(partes[1])
                    val receta = rDao.buscarPorNombreIgnorandoCase(nombre)
                        ?: rDao.obtenerTodasStatic().firstOrNull { normalizar(it.nombre) == normalizar(nombre) }
                    if (receta == null) { onFeedback("AVISO: No existe la receta '$nombre'"); return@launch }
                    rDao.actualizar(receta.copy(precioVenta = valor))
                    onFeedback("Precio de venta de '${receta.nombre}' -> ${"%.2f".format(valor)}")
                }

                // MODIFICAR: cantidad de un ingrediente en el escandallo de la receta activa
                t.startsWith("modificar cantidad ingrediente") -> {
                    val resto = t.quitarPrefijos("modificar cantidad ingrediente de", "modificar cantidad ingrediente")
                    val partes = resto.split(Regex("\\s+a\\s+|\\s+a(?=\\d)"))
                    if (partes.size < 2) { onFeedback("AVISO: Di modificar cantidad ingrediente [nombre] a [valor]"); return@launch }
                    val nombre = partes[0].trim().replaceFirstChar { it.uppercase() }
                    val valor  = parsearValor(partes[1])
                    if (_recetaActiva.value.isEmpty()) { onFeedback("AVISO: Primero abre una receta con abrir receta de [nombre]"); return@launch }
                    val receta = rDao.buscarPorNombreIgnorandoCase(_recetaActiva.value)
                    if (receta == null) { onFeedback("AVISO: Receta activa no encontrada"); return@launch }
                    val ingrediente = iDao.buscarPorNombreIgnorandoCase(nombre)
                    if (ingrediente == null) { onFeedback("AVISO: '${nombre}' no esta en inventario"); return@launch }
                    rDao.actualizarCantidadIngrediente(receta.id, ingrediente.id, valor)
                    onFeedback("OK: ${ingrediente.nombre} -> $valor ${ingrediente.unidad} en '${_recetaActiva.value}'")
                }

                // MODIFICAR: receta (navegar a edicion)
                t.startsWith("modificar receta") -> {
                    val nombre = t.quitarPrefijos("modificar receta de", "modificar receta")
                    val receta = rDao.buscarPorNombreIgnorandoCase(nombre)
                        ?: rDao.obtenerTodasStatic().firstOrNull { normalizar(it.nombre) == normalizar(nombre) }
                    if (receta != null) {
                        onFeedback("NAV_EDITAR|${receta.id}")
                    } else {
                        onFeedback("AVISO: No existe la receta '$nombre'")
                    }
                }

                // MODIFICAR: precio o cantidad de ingrediente
                t.startsWith("modificar") -> {
                    val resto    = t.quitarPrefijos("modificar")
                    val esPrecio = resto.startsWith("precio")
                    val esCant   = resto.startsWith("cantidad")
                    val sinTipo  = resto.quitarPrefijos("precio de", "precio", "cantidad de", "cantidad")
                    val partes = resto.split(Regex("\\s+a\\s+|\\s+a(?=\\d)"))
                    if (partes.size < 2) { onFeedback("AVISO: Di modificar precio de [nombre] a [valor]"); return@launch }
                    val nombre = partes[0].trim().replaceFirstChar { it.uppercase() }
                    val valor  = parsearValor(partes[1])
                    val ing    = iDao.buscarPorNombreIgnorandoCase(nombre)
                    if (ing == null) { onFeedback("AVISO: No existe $nombre"); return@launch }
                    when {
                        esPrecio -> { iDao.actualizar(ing.copy(precio   = valor)); onFeedback("Precio $nombre -> ${"%.2f".format(valor)}") }
                        esCant   -> { iDao.actualizar(ing.copy(cantidad = valor)); onFeedback("Stock $nombre -> $valor ${ing.unidad}") }
                        else     -> onFeedback("AVISO: Di precio o cantidad despues de modificar")
                    }
                }

                // ELIMINAR: ingrediente del escandallo de la receta activa
                t.startsWith("eliminar ingrediente") || t.startsWith("borrar ingrediente") -> {
                    val nombre = t.quitarPrefijos(
                        "eliminar ingrediente de", "eliminar ingrediente",
                        "borrar ingrediente de",  "borrar ingrediente"
                    ).replaceFirstChar { it.uppercase() }
                    if (_recetaActiva.value.isEmpty()) { onFeedback("AVISO: Primero abre una receta con abrir receta de [nombre]"); return@launch }
                    val receta = rDao.buscarPorNombreIgnorandoCase(_recetaActiva.value)
                    if (receta == null) { onFeedback("AVISO: Receta activa no encontrada"); return@launch }
                    val ingrediente = iDao.buscarPorNombreIgnorandoCase(nombre)
                    if (ingrediente == null) { onFeedback("AVISO: '$nombre' no esta en inventario"); return@launch }
                    rDao.desasociarIngrediente(receta.id, ingrediente.id)
                    onFeedback("OK: ${ingrediente.nombre} eliminado del escandallo de '${_recetaActiva.value}'")
                }

                // ELIMINAR: receta
                t.startsWith("eliminar receta") || t.startsWith("borrar receta") -> {
                    val nombre = t.quitarPrefijos("eliminar receta de", "eliminar receta", "borrar receta")
                        .replaceFirstChar { it.uppercase() }
                    val receta = rDao.buscarPorNombreIgnorandoCase(nombre)
                        ?: rDao.obtenerTodasStatic().firstOrNull { normalizar(it.nombre) == normalizar(nombre) }
                    if (receta != null) { rDao.eliminar(receta); onFeedback("Eliminado: receta ${receta.nombre}") }
                    else onFeedback("AVISO: No existe la receta $nombre")
                }

                // ELIMINAR: cantidad o todo el ingrediente
                t.startsWith("eliminar") || t.startsWith("borrar") -> {
                    val resto = t.quitarPrefijos("eliminar", "borrar")
                    val restoNorm = normalizarNumeroInicial(resto)
                    val conCant = Regex("""^(\d+[.,]?\d*)\s+de\s+(.+)""").find(restoNorm)
                    if (conCant != null) {
                        val cant   = conCant.groupValues[1].replace(",", ".").toDoubleOrNull() ?: 1.0
                        val nombre = conCant.groupValues[2].trim().replaceFirstChar { it.uppercase() }
                        val ing    = iDao.buscarPorNombreIgnorandoCase(nombre)
                        if (ing != null) {
                            val nueva = (ing.cantidad - cant).coerceAtLeast(0.0)
                            iDao.actualizar(ing.copy(cantidad = nueva))
                            onFeedback("Stock: ${ing.nombre} -> $nueva ${ing.unidad}")
                        } else onFeedback("AVISO: No existe $nombre")
                    } else {
                        val nombre = resto.replaceFirstChar { it.uppercase() }
                        val ing    = iDao.buscarPorNombreIgnorandoCase(nombre)
                        if (ing != null) { iDao.eliminar(ing); onFeedback("Eliminado: ${ing.nombre}") }
                        else onFeedback("AVISO: No existe $nombre")
                    }
                }

                else -> onFeedback("AVISO: Di nuevo, anadir, modificar o eliminar")
            }
        }
    }

    private fun normalizar(texto: String) = texto.trim().lowercase()
        .replace("a\u0301","a").replace("e\u0301","e").replace("i\u0301","i")
        .replace("o\u0301","o").replace("u\u0301","u")
        .replace("\u00e1","a").replace("\u00e9","e").replace("\u00ed","i")
        .replace("\u00f3","o").replace("\u00fa","u").replace("\u00f1","n")

    private fun String.quitarPrefijos(vararg prefijos: String): String {
        for (p in prefijos.sortedByDescending { it.length }) {
            if (this.startsWith(p)) return this.removePrefix(p).trim()
        }
        return this
    }

    private fun normalizarNumeroInicial(texto: String): String {
        val palabrasNum = mapOf(
            "un" to "1", "uno" to "1", "una" to "1",
            "dos" to "2", "tres" to "3", "cuatro" to "4",
            "cinco" to "5", "seis" to "6", "siete" to "7",
            "ocho" to "8", "nueve" to "9", "diez" to "10",
            "once" to "11", "doce" to "12", "quince" to "15",
            "veinte" to "20", "medio" to "0.5", "media" to "0.5"
        )
        val partes = texto.split(" ", limit = 2)
        val sustituido = palabrasNum[partes[0]]
        return if (sustituido != null && partes.size == 2) "$sustituido ${partes[1]}"
        else texto
    }

    private fun parsearValor(texto: String): Double {
        val t = texto.trim()
        val palabrasNum = mapOf(
            "un" to 1.0, "uno" to 1.0, "una" to 1.0,
            "dos" to 2.0, "tres" to 3.0, "cuatro" to 4.0,
            "cinco" to 5.0, "seis" to 6.0, "siete" to 7.0,
            "ocho" to 8.0, "nueve" to 9.0, "diez" to 10.0,
            "once" to 11.0, "doce" to 12.0, "quince" to 15.0,
            "veinte" to 20.0, "medio" to 0.5, "media" to 0.5
        )
        palabrasNum[t]?.let { return it }
        return t.replace(",", ".").replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
    }
}