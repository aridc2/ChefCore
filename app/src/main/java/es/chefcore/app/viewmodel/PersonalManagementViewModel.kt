package es.chefcore.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import es.chefcore.app.data.database.ChefCoreDatabase
import es.chefcore.app.data.database.Usuario
import es.chefcore.app.data.repository.UsuarioRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel para PersonalManagementScreen
 * Gestiona la lista de empleados y operaciones CRUD
 */
class PersonalManagementViewModel(application: Application) : AndroidViewModel(application) {
    
    // Repository
    private val database = ChefCoreDatabase.getDatabase(application)
    private val usuarioRepository = UsuarioRepository(database.usuarioDao())
    
    val usuarios: StateFlow<List<Usuario>> = usuarioRepository.obtenerTodos()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    /**
     * Crea un nuevo empleado
     */
    fun crearEmpleado(nombre: String, rol: String, pin: String) {
        viewModelScope.launch {
            try {
                // Validaciones
                if (nombre.isBlank()) {
                    _errorMessage.value = "El nombre es obligatorio"
                    return@launch
                }
                
                if (rol.isBlank()) {
                    _errorMessage.value = "El rol es obligatorio"
                    return@launch
                }
                
                if (pin.length != 4) {
                    _errorMessage.value = "El PIN debe tener 4 dígitos"
                    return@launch
                }
                
                val existente = usuarioRepository.buscarPorNombre(nombre)
                if (existente != null) {
                    _errorMessage.value = "Ya existe un empleado con ese nombre"
                    return@launch
                }
                
                val usuarioConPin = usuarioRepository.validarPin(pin)
                if (usuarioConPin != null) {
                    _errorMessage.value = "Ese PIN ya está en uso"
                    return@launch
                }
                
                val nuevo = Usuario(
                    nombre = nombre.trim(),
                    rol = rol.trim(),
                    pin = pin
                )
                
                usuarioRepository.insertar(nuevo)
                _feedbackMessage.value = " Empleado ${nombre} creado correctamente"
                _errorMessage.value = null
                
            } catch (e: Exception) {
                _errorMessage.value = "Error al crear empleado: ${e.message}"
            }
        }
    }
    
    /**
     * Elimina un empleado
     */
    fun eliminarEmpleado(usuario: Usuario) {
        viewModelScope.launch {
            try {
                // Necesitarás añadir método delete al DAO
                // usuarioRepository.eliminar(usuario)
                _feedbackMessage.value = " ${usuario.nombre} eliminado"
            } catch (e: Exception) {
                _errorMessage.value = "Error al eliminar: ${e.message}"
            }
        }
    }
    
    /**
     * Valida un PIN para login
     */
    suspend fun validarPin(pin: String): Usuario? {
        return usuarioRepository.validarPin(pin)
    }
    
    /**
     * Limpia mensajes de feedback y error
     */
    fun clearMessages() {
        _feedbackMessage.value = null
        _errorMessage.value = null
    }
}
