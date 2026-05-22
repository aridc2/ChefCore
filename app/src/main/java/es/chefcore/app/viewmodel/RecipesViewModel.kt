package es.chefcore.app.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import es.chefcore.app.data.database.ChefCoreDatabase
import es.chefcore.app.data.database.Receta
import es.chefcore.app.data.database.RecetaIngrediente
import es.chefcore.app.data.repository.RecetaRepository
import es.chefcore.app.data.repository.IngredienteRepository
import es.chefcore.app.logic.CocinaManager
import es.chefcore.app.logic.RentabilidadReceta
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

/**
 * ViewModel de la pantalla de recetas.
 * Expone la lista de recetas con filtrado por nombre y la rentabilidad calculada
 * de cada plato a partir del PMP actual de sus ingredientes.
 */
class RecipesViewModel(application: Application) : AndroidViewModel(application) {

    private val database = ChefCoreDatabase.getDatabase(application)
    private val recetaRepository = RecetaRepository(database.recetaDao())
    private val ingredienteRepository = IngredienteRepository(database.ingredienteDao())
    private val cocinaManager = CocinaManager(database.ingredienteDao(), database.recetaDao())

    val ingredientesDisponibles = ingredienteRepository.obtenerTodos()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val recetas: StateFlow<List<Receta>> = recetaRepository.obtenerTodas()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _rentabilidades = MutableStateFlow<Map<Int, RentabilidadReceta>>(emptyMap())
    val rentabilidades: StateFlow<Map<Int, RentabilidadReceta>> = _rentabilidades.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val recetasFiltradas: StateFlow<List<Receta>> = combine(recetas, searchQuery) { lista, query ->
        if (query.isBlank()) lista else lista.filter { it.nombre.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _recetaSeleccionada = MutableStateFlow<Int?>(null)
    val recetaSeleccionada: StateFlow<Int?> = _recetaSeleccionada.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    init {
        calcularTodasRentabilidades()
    }

    fun actualizarBusqueda(query: String) { _searchQuery.value = query }

    fun seleccionarReceta(recetaId: Int?) { _recetaSeleccionada.value = recetaId }

    fun seleccionarRecetaPorNombre(nombre: String) {
        val receta = recetasFiltradas.value.firstOrNull { it.nombre.equals(nombre, ignoreCase = true) }
        if (receta != null) _recetaSeleccionada.value = receta.id
    }

    fun clearFeedback() { _feedbackMessage.value = null }

    fun crearReceta(
        nombre: String,
        precioVenta: Double,
        tiempoPreparacionMinutos: Int = 30,
        instrucciones: String,
        imagenUri: String?,
        ingredientes: List<Pair<Int, Double>>
    ) {
        viewModelScope.launch {
            try {
                val nueva = Receta(
                    nombre = nombre.trim().replaceFirstChar { it.uppercase() },
                    precioVenta = precioVenta,
                    tiempoPreparacionMinutos = tiempoPreparacionMinutos,
                    instrucciones = instrucciones,
                    imagenUri = imagenUri
                )
                val nuevoId = recetaRepository.insertar(nueva).toInt()

                ingredientes.forEach { (idIng, cant) ->
                    val relacion = RecetaIngrediente(
                        recetaId = nuevoId,
                        ingredienteId = idIng,
                        cantidadNecesaria = cant
                    )
                    recetaRepository.añadirIngrediente(relacion)
                }

                calcularRentabilidad(nuevoId)
                _feedbackMessage.value = " Receta '${nueva.nombre}' creada"
            } catch (e: Exception) {
                _feedbackMessage.value = " Error al crear: ${e.message}"
                e.printStackTrace()
            }
        }
    }

    fun actualizarReceta(
        recetaId: Int,
        nombre: String,
        precioVenta: Double,
        tiempoPreparacionMinutos: Int,
        instrucciones: String,
        imagenUri: String?,
        ingredientes: List<Pair<Int, Double>>
    ) {
        viewModelScope.launch {
            try {
                val recetaExistente = recetaRepository.buscarPorId(recetaId) ?: return@launch

                // Si cambia la imagen, liberar la anterior del almacenamiento local
                if (recetaExistente.imagenUri != null && recetaExistente.imagenUri != imagenUri) {
                    try {
                        val oldFile = File(Uri.parse(recetaExistente.imagenUri).path ?: "")
                        if (oldFile.exists()) oldFile.delete()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                val actualizada = recetaExistente.copy(
                    nombre = nombre.trim().replaceFirstChar { it.uppercase() },
                    precioVenta = precioVenta,
                    tiempoPreparacionMinutos = tiempoPreparacionMinutos,
                    instrucciones = instrucciones,
                    imagenUri = imagenUri
                )

                recetaRepository.actualizar(actualizada)
                recetaRepository.eliminarTodosIngredientes(recetaId)

                ingredientes.forEach { (idIng, cant) ->
                    val relacion = RecetaIngrediente(
                        recetaId = recetaId,
                        ingredienteId = idIng,
                        cantidadNecesaria = cant
                    )
                    recetaRepository.añadirIngrediente(relacion)
                }

                calcularRentabilidad(recetaId)
                _feedbackMessage.value = " Receta actualizada"
            } catch (e: Exception) {
                _feedbackMessage.value = " Error al actualizar: ${e.message}"
                e.printStackTrace()
            }
        }
    }

    fun eliminarReceta(receta: Receta) {
        viewModelScope.launch {
            try {
                if (receta.imagenUri != null) {
                    try {
                        val file = File(Uri.parse(receta.imagenUri).path ?: "")
                        if (file.exists()) file.delete()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                recetaRepository.eliminarTodosIngredientes(receta.id)
                recetaRepository.eliminar(receta)

                _rentabilidades.value = _rentabilidades.value - receta.id

                if (_recetaSeleccionada.value == receta.id) {
                    _recetaSeleccionada.value = null
                }

                _feedbackMessage.value = " Receta '${receta.nombre}' eliminada"
            } catch (e: Exception) {
                _feedbackMessage.value = " Error al eliminar: ${e.message}"
                e.printStackTrace()
            }
        }
    }

    fun obtenerReceta(recetaId: Int) = database.recetaDao().observarPorId(recetaId)

    fun obtenerIngredientesDeReceta(recetaId: Int) =
        database.recetaDao().observarIngredientesDeReceta(recetaId)

    fun calcularRentabilidad(recetaId: Int) {
        viewModelScope.launch {
            try {
                val receta = recetaRepository.buscarPorId(recetaId) ?: return@launch
                val rentabilidad = cocinaManager.calcularRentabilidad(recetaId, receta.precioVenta)
                _rentabilidades.value = _rentabilidades.value + (recetaId to rentabilidad)
            } catch (e: Exception) { }
        }
    }

    private fun calcularTodasRentabilidades() {
        viewModelScope.launch {
            recetas.collectLatest { lista ->
                lista.forEach { calcularRentabilidad(it.id) }
            }
        }
    }
}
