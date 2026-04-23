package es.chefcore.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import es.chefcore.app.data.database.*
import es.chefcore.app.logic.UnitConverter
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RecetaDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val database = ChefCoreDatabase.getDatabase(application)
    private val recetaDao = database.recetaDao()
    private val ingredienteDao = database.ingredienteDao()

    // Estado de la receta actual
    private val _recetaId = MutableStateFlow<Int?>(null)

    val receta: StateFlow<Receta?> = _recetaId
        .filterNotNull()
        .flatMapLatest { id ->
            recetaDao.observarPorId(id)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    // Ingredientes en la receta (escandallo)
    val ingredientesEnReceta: StateFlow<List<IngredienteEnReceta>> = _recetaId
        .filterNotNull()
        .flatMapLatest { id ->
            recetaDao.observarIngredientesDeReceta(id)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Todos los ingredientes disponibles
    val ingredientesDisponibles: StateFlow<List<Ingrediente>> = ingredienteDao.obtenerTodos()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Coste total de producción calculado
    val costeTotalProduccion: StateFlow<Double> = ingredientesEnReceta
        .map { lista ->
            lista.sumOf { it.costeTotal }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    // Estado interno de la receta (para tener acceso síncrono)
    private var recetaActual: Receta? = null

    /**
     * Carga la receta por ID
     */
    fun cargarReceta(recetaId: Int) {
        _recetaId.value = recetaId
        viewModelScope.launch {
            recetaActual = recetaDao.buscarPorId(recetaId)
        }
    }

    /**
     * Añade un ingrediente a la receta
     */
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

            val relacion = RecetaIngrediente(
                recetaId = recetaId,
                ingredienteId = ingredienteId,
                cantidadNecesaria = cantidadNormalizada
            )

            recetaDao.asociarIngrediente(relacion)
        }
    }

    /**
     * Actualiza la cantidad de un ingrediente existente en la receta
     */
    fun actualizarCantidadIngrediente(
        ingredienteId: Int,
        nuevaCantidad: Double,
        nuevaUnidad: String
    ) {
        val recetaId = _recetaId.value ?: return

        viewModelScope.launch {
            eliminarIngrediente(ingredienteId)
            añadirIngrediente(ingredienteId, nuevaCantidad, nuevaUnidad)
        }
    }

    /**
     * Elimina un ingrediente de la receta
     */
    fun eliminarIngrediente(ingredienteId: Int) {
        val recetaId = _recetaId.value ?: return

        viewModelScope.launch {
            recetaDao.desasociarIngrediente(recetaId, ingredienteId)
        }
    }

    /**
     * Actualiza el precio de venta de la receta
     */
    fun actualizarPrecioVenta(receta: Receta, nuevoPrecio: Double) {
        viewModelScope.launch {
            val actualizada = receta.copy(precioVenta = nuevoPrecio)
            recetaDao.actualizar(actualizada)
            recetaActual = actualizada
        }
    }

    /**
     * Actualiza las instrucciones de la receta
     */
    fun actualizarInstrucciones(nuevasInstrucciones: String) {
        viewModelScope.launch {
            recetaActual?.let { receta ->
                val actualizada = receta.copy(instrucciones = nuevasInstrucciones)
                recetaDao.actualizar(actualizada)
                recetaActual = actualizada
            }
        }
    }
}