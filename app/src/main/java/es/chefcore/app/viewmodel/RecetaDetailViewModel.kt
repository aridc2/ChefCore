package es.chefcore.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import es.chefcore.app.data.database.*
import es.chefcore.app.logic.CocinaManager
import es.chefcore.app.logic.UnitConverter
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel del detalle de receta.
 * Gestiona el escandallo (ingredientes y cantidades necesarias), el cálculo
 * del coste de producción y el descuento de stock al preparar raciones.
 */
class RecetaDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val database = ChefCoreDatabase.getDatabase(application)
    private val recetaDao = database.recetaDao()
    private val ingredienteDao = database.ingredienteDao()
    private val cocinaManager = CocinaManager(database.ingredienteDao(), database.recetaDao())

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    private val _recetaId = MutableStateFlow<Int?>(null)

    val receta: StateFlow<Receta?> = _recetaId
        .filterNotNull()
        .flatMapLatest { id -> recetaDao.observarPorId(id) }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val ingredientesEnReceta: StateFlow<List<IngredienteEnReceta>> = _recetaId
        .filterNotNull()
        .flatMapLatest { id -> recetaDao.observarIngredientesDeReceta(id) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val ingredientesDisponibles: StateFlow<List<Ingrediente>> = ingredienteDao.obtenerTodos()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val costeTotalProduccion: StateFlow<Double> = ingredientesEnReceta
        .map { lista -> lista.sumOf { it.costeTotal } }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    private var recetaActual: Receta? = null

    fun cargarReceta(recetaId: Int) {
        _recetaId.value = recetaId
        viewModelScope.launch {
            recetaActual = recetaDao.buscarPorId(recetaId)
        }
    }

    fun añadirIngrediente(ingredienteId: Int, cantidad: Double, unidad: String) {
        val recetaId = _recetaId.value ?: return
        viewModelScope.launch {
            val ingrediente = ingredienteDao.obtenerTodos().first()
                .find { it.id == ingredienteId } ?: return@launch

            val cantidadNormalizada = try {
                UnitConverter.convertir(cantidad, unidad, ingrediente.unidad)
            } catch (e: Exception) {
                cantidad
            }

            recetaDao.asociarIngrediente(
                RecetaIngrediente(
                    recetaId = recetaId,
                    ingredienteId = ingredienteId,
                    cantidadNecesaria = cantidadNormalizada
                )
            )
        }
    }

    fun actualizarCantidadIngrediente(ingredienteId: Int, nuevaCantidad: Double, nuevaUnidad: String) {
        viewModelScope.launch {
            eliminarIngrediente(ingredienteId)
            añadirIngrediente(ingredienteId, nuevaCantidad, nuevaUnidad)
        }
    }

    fun eliminarIngrediente(ingredienteId: Int) {
        val recetaId = _recetaId.value ?: return
        viewModelScope.launch {
            recetaDao.desasociarIngrediente(recetaId, ingredienteId)
        }
    }

    fun actualizarPrecioVenta(receta: Receta, nuevoPrecio: Double) {
        viewModelScope.launch {
            val actualizada = receta.copy(precioVenta = nuevoPrecio)
            recetaDao.actualizar(actualizada)
            recetaActual = actualizada
        }
    }

    /**
     * Descuenta los ingredientes del inventario al preparar [raciones] del plato.
     * Si falta stock en cualquier ingrediente, no se descuenta nada (operación atómica).
     */
    fun prepararPlato(recetaId: Int, raciones: Int) {
        viewModelScope.launch {
            val exito = cocinaManager.cocinar(recetaId, raciones)
            _feedbackMessage.value = if (exito) {
                " Ingredientes descontados para $raciones ración(es)"
            } else {
                " Stock insuficiente para preparar $raciones ración(es)"
            }
        }
    }

    fun actualizarInstrucciones(nuevasInstrucciones: String) {
        viewModelScope.launch {
            recetaActual?.let { receta ->
                val actualizada = receta.copy(instrucciones = nuevasInstrucciones)
                recetaDao.actualizar(actualizada)
                recetaActual = actualizada
            }
        }
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }
}
