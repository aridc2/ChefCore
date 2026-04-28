package es.chefcore.app.viewmodel

import android.app.Application
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

class RecipesViewModel(application: Application) : AndroidViewModel(application) {

    private val database = ChefCoreDatabase.getDatabase(application)
    private val recetaRepository = RecetaRepository(database.recetaDao())
    private val ingredienteRepository = IngredienteRepository(database.ingredienteDao())
    private val cocinaManager = CocinaManager(database.ingredienteDao(), database.recetaDao())

    // Inventario disponible para el desplegable de la pantalla de creación
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

    init {
        calcularTodasRentabilidades()
    }

    fun actualizarBusqueda(query: String) { _searchQuery.value = query }
    fun seleccionarReceta(recetaId: Int?) { _recetaSeleccionada.value = recetaId }

    /**
     * ✅ FUNCIÓN ACTUALIZADA: Guarda la receta y su escandallo
     */
    fun crearReceta(
        nombre: String,
        precioVenta: Double,
        instrucciones: String,
        imagenUri: String?,
        ingredientes: List<Pair<Int, Double>> // Lista de (ID de ingrediente, Cantidad normalizada)
    ) {
        viewModelScope.launch {
            try {
                val nueva = Receta(
                    nombre = nombre.trim().replaceFirstChar { it.uppercase() },
                    precioVenta = precioVenta,
                    instrucciones = instrucciones,
                    imagenUri = imagenUri
                )
                recetaRepository.insertar(nueva)

                val recetaCreada = recetaRepository.buscarPorNombre(nueva.nombre)

                if (recetaCreada != null) {
                    ingredientes.forEach { (idIng, cant) ->
                        val relacion = RecetaIngrediente(
                            recetaId = recetaCreada.id,
                            ingredienteId = idIng,
                            cantidadNecesaria = cant
                        )
                        recetaRepository.añadirIngrediente(relacion)
                    }

                    calcularRentabilidad(recetaCreada.id)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

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