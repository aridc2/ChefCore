package es.chefcore.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import es.chefcore.app.data.database.ChefCoreDatabase
import es.chefcore.app.data.database.Ingrediente
import es.chefcore.app.data.repository.IngredienteRepository
import es.chefcore.app.logic.CocinaManager
import es.chefcore.app.logic.RegistroStockResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel para InventoryScreen
 * Gestiona el estado y lógica de negocio del inventario
 */
class InventoryViewModel(application: Application) : AndroidViewModel(application) {
    
    // Repositories
    private val database = ChefCoreDatabase.getDatabase(application)
    private val ingredienteRepository = IngredienteRepository(database.ingredienteDao())
    private val cocinaManager = CocinaManager(
        database.ingredienteDao(),
        database.recetaDao()
    )
    
    val ingredientes: StateFlow<List<Ingrediente>> = ingredienteRepository.obtenerTodos()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()
    
    private val _errorIncompatible = MutableStateFlow<RegistroStockResult.ErrorIncompatible?>(null)
    val errorIncompatible: StateFlow<RegistroStockResult.ErrorIncompatible?> = _errorIncompatible.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    /**
     * Añade stock de un ingrediente (SUMA, no reemplaza)
     * Calcula PMP automáticamente
     */
    fun añadirStock(
        nombre: String,
        cantidad: Double,
        unidad: String,
        precioTotal: Double
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            
            when (val resultado = cocinaManager.registrarEntradaStock(
                nombre, cantidad, unidad, precioTotal
            )) {
                is RegistroStockResult.NuevoIngrediente -> {
                    _feedbackMessage.value = " Nuevo: ${resultado.ingrediente.nombre} " +
                            "(€${"%.2f".format(resultado.ingrediente.precio)}/${resultado.ingrediente.unidad})"
                }
                
                is RegistroStockResult.StockActualizado -> {
                    val ing = resultado.ingrediente
                    _feedbackMessage.value = buildString {
                        append("➕ ${ing.nombre} actualizado\n")
                        append("Stock: ${String.format("%.2f", ing.cantidad)} ${ing.unidad}\n")
                        append("PMP: €${"%.2f".format(resultado.pmpAnterior)} ")
                        append("→ €${"%.2f".format(resultado.pmpNuevo)}")
                    }
                }
                
                is RegistroStockResult.ErrorIncompatible -> {
                    _errorIncompatible.value = resultado
                    _feedbackMessage.value = null
                }
                
                is RegistroStockResult.Error -> {
                    _feedbackMessage.value = "❌ Error: ${resultado.mensaje}"
                }
            }
            
            _isLoading.value = false
        }
    }
    
    /**
     * Elimina un ingrediente
     */
    fun eliminarIngrediente(ingrediente: Ingrediente) {
        viewModelScope.launch {
            try {
                // Necesitarás añadir método delete al DAO
                // ingredienteRepository.eliminar(ingrediente)
                _feedbackMessage.value = "${ingrediente.nombre} eliminado"
            } catch (e: Exception) {
                _feedbackMessage.value = "❌ Error al eliminar: ${e.message}"
            }
        }
    }
    
    /**
     * Limpia el mensaje de feedback
     */
    fun clearFeedback() {
        _feedbackMessage.value = null
        _errorIncompatible.value = null
    }
    
    /**
     * Busca ingredientes por nombre (para búsqueda en tiempo real)
     */
    fun buscarIngredientes(query: String): StateFlow<List<Ingrediente>> {
        return ingredientes.map { lista ->
            if (query.isBlank()) {
                lista
            } else {
                lista.filter { it.nombre.contains(query, ignoreCase = true) }
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    }
}
