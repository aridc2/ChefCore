package es.chefcore.app.viewmodel

import android.app.Application
import android.util.Log
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import es.chefcore.app.data.database.Albaran
import es.chefcore.app.data.database.ChefCoreDatabase
import es.chefcore.app.data.database.Ingrediente
import es.chefcore.app.logic.AlbaranOcrParser
import es.chefcore.app.logic.CocinaManager
import es.chefcore.app.logic.ItemAlbaran
import es.chefcore.app.logic.OcrAnalyzer
import es.chefcore.app.logic.RegistroStockResult
import es.chefcore.app.logic.ResultadoMLKit
import es.chefcore.app.logic.ResultadoOcr
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class OcrViewModel(application: Application) : AndroidViewModel(application) {

    private val db             = ChefCoreDatabase.getDatabase(application)
    private val albaranDao     = db.albaranDao()
    private val ingredienteDao = db.ingredienteDao()
    private val recetaDao      = db.recetaDao()
    private val cocinaManager  = CocinaManager(ingredienteDao, recetaDao)

    private val _estado = MutableStateFlow<EstadoOcr>(EstadoOcr.Idle)
    val estado: StateFlow<EstadoOcr> = _estado.asStateFlow()

    /** Lista de ingredientes actuales para comprobación de similitud en OcrValidacionScreen */
    val ingredientes: StateFlow<List<Ingrediente>> = ingredienteDao.obtenerTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Texto acumulado de todas las páginas escaneadas
    private var textoAcumulado    = StringBuilder()
    private var bloquesAcumulados = mutableListOf<com.google.mlkit.vision.text.Text.TextBlock>()
    private var paginasEscaneadas = 0

    /**
     * Procesa una página (foto o imagen de galería).
     * El texto se ACUMULA con el de páginas anteriores — soporta albaranes multipágina.
     */
    fun analizarPagina(bitmap: Bitmap) {
        _estado.value = EstadoOcr.Procesando
        viewModelScope.launch {
            try {
                val resultadoMLKit = OcrAnalyzer.recognizeWithBlocks(bitmap)
                val textoPagina = resultadoMLKit.texto

                // LOG: texto crudo de ML Kit
                Log.d("ChefCore_OCR", "══════════════════════════════════════")
                Log.d("ChefCore_OCR", "BLOQUES ML Kit: ${resultadoMLKit.bloques.size}")
                resultadoMLKit.bloques.forEachIndexed { i, bloque ->
                    val box = bloque.boundingBox
                    Log.d("ChefCore_OCR", "  Bloque[$i] Y=${box?.top}-${box?.bottom} X=${box?.left}-${box?.right}")
                    bloque.lines.forEach { linea ->
                        val lb = linea.boundingBox
                        Log.d("ChefCore_OCR", "    Linea Y=${lb?.centerY()} X=${lb?.left}: ${linea.text}")
                    }
                }

                if (textoPagina.isBlank()) {
                    _estado.value = EstadoOcr.Error(
                        "No se detectó texto en la imagen.\n" +
                                "Prueba con mejor iluminación o elige una foto de la galería."
                    )
                    return@launch
                }

                // Acumular texto de todas las páginas
                if (textoAcumulado.isNotEmpty()) textoAcumulado.append("\n\n")
                textoAcumulado.append(textoPagina)
                bloquesAcumulados.addAll(resultadoMLKit.bloques)
                paginasEscaneadas++

                // Usar bloques con posición para reconstruir filas de tabla (detecta precios)
                val mlKitAcumulado = ResultadoMLKit(textoAcumulado.toString(), bloquesAcumulados)
                val resultado = AlbaranOcrParser.parsear(mlKitAcumulado)

                // LOG: resultado del parser
                Log.d("ChefCore_OCR", "══ RESULTADO PARSER ══════════════════")
                Log.d("ChefCore_OCR", "Tipo: ${resultado.tipoAlbaran}")
                Log.d("ChefCore_OCR", "Proveedor: ${resultado.proveedor}")
                Log.d("ChefCore_OCR", "Fecha: ${resultado.fecha}")
                Log.d("ChefCore_OCR", "Total: ${resultado.totalEuros}")
                Log.d("ChefCore_OCR", "Items detectados: ${resultado.items.size}")
                resultado.items.forEachIndexed { i, item ->
                    Log.d("ChefCore_OCR", "  [$i] ${item.descripcion} | cant=${item.cantidad} ${item.unidad} | precio=${item.precioUnitario}")
                }
                Log.d("ChefCore_OCR", "══════════════════════════════════════")

                _estado.value = EstadoOcr.PaginaLista(paginasEscaneadas, resultado)

            } catch (e: Exception) {
                _estado.value = EstadoOcr.Error("Error al procesar la imagen: ${e.message}")
            }
        }
    }

    /**
     * Guarda el albarán validado y actualiza el inventario con CocinaManager.
     * CocinaManager fusiona automaticamente nombres con similitud >= 0.82.
     */
    fun guardarAlbaranConItems(
        proveedor: String,
        fecha: String,
        total: Double,
        idUsuario: Int,
        itemsSeleccionados: List<ItemAlbaran>
    ) {
        viewModelScope.launch {
            albaranDao.insertar(
                Albaran(
                    proveedor  = proveedor.ifBlank { "Proveedor desconocido" },
                    fecha      = fecha.ifBlank {
                        LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    },
                    totalEuros = total,
                    idUsuario  = idUsuario
                )
            )
            for (item in itemsSeleccionados) {
                cocinaManager.registrarEntradaStock(
                    nombre      = item.descripcion,
                    cantidad    = item.cantidad,
                    unidad      = item.unidad,
                    precioTotal = item.cantidad * item.precioUnitario
                )
            }
            _estado.value = EstadoOcr.Guardado
        }
    }

    fun resetear() {
        textoAcumulado.clear()
        bloquesAcumulados.clear()
        paginasEscaneadas = 0
        _estado.value = EstadoOcr.Idle
    }
}

sealed class EstadoOcr {
    object Idle       : EstadoOcr()
    object Procesando : EstadoOcr()
    /** Una o más páginas escaneadas — puede seguir escaneando o ir a validar */
    data class PaginaLista(val paginas: Int, val resultado: ResultadoOcr) : EstadoOcr()
    data class Error(val mensaje: String) : EstadoOcr()
    object Guardado   : EstadoOcr()
}