package es.chefcore.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import es.chefcore.app.data.database.ChefCoreDatabase
import es.chefcore.app.data.database.Usuario
import es.chefcore.app.data.repository.UsuarioRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PersonalManagementViewModel(application: Application) : AndroidViewModel(application) {

    private val database = ChefCoreDatabase.getDatabase(application)
    private val usuarioRepository = UsuarioRepository(database.usuarioDao())

    val usuarios: StateFlow<List<Usuario>> = usuarioRepository.obtenerTodos()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun crearEmpleado(nombre: String, rol: String, pin: String) {
        viewModelScope.launch {
            try {
                if (nombre.isBlank() || rol.isBlank() || pin.length != 4) {
                    _errorMessage.value = "Rellena todos los campos correctamente. El PIN debe tener 4 dígitos."
                    return@launch
                }

                if (usuarioRepository.buscarPorNombre(nombre) != null) {
                    _errorMessage.value = "Ya existe un empleado con ese nombre"
                    return@launch
                }

                if (usuarioRepository.validarPin(pin) != null) {
                    _errorMessage.value = "Ese PIN ya está en uso por otro empleado"
                    return@launch
                }

                usuarioRepository.insertar(Usuario(nombre = nombre.trim(), rol = rol.trim(), pin = pin))
                _feedbackMessage.value = " Empleado ${nombre} creado correctamente"
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Error al crear empleado: ${e.message}"
            }
        }
    }

    fun actualizarEmpleado(usuario: Usuario, nombre: String, rol: String, pin: String) {
        viewModelScope.launch {
            try {
                if (nombre.isBlank() || rol.isBlank() || pin.length != 4) {
                    _errorMessage.value = "Datos inválidos. El PIN debe ser de 4 dígitos."
                    return@launch
                }

                val actualizado = usuario.copy(nombre = nombre.trim(), rol = rol.trim(), pin = pin)
                usuarioRepository.actualizar(actualizado)
                _feedbackMessage.value = " Empleado ${nombre} actualizado"
            } catch (e: Exception) {
                _errorMessage.value = "Error al actualizar: ${e.message}"
            }
        }
    }

    fun eliminarEmpleado(usuario: Usuario) {
        viewModelScope.launch {
            try {
                // Solo un seguro de vida: evitar que el gerente principal se borre a sí mismo
                if (usuario.rol == "Gerente" && usuarios.value.count { it.rol == "Gerente" } == 1) {
                    _errorMessage.value = "No puedes eliminar al único Gerente del sistema."
                    return@launch
                }

                usuarioRepository.eliminar(usuario)
                _feedbackMessage.value = " ${usuario.nombre} eliminado"
            } catch (e: Exception) {
                _errorMessage.value = "Error al eliminar: ${e.message}"
            }
        }
    }

    suspend fun validarPin(pin: String): Usuario? {
        return usuarioRepository.validarPin(pin)
    }

    fun clearMessages() {
        _feedbackMessage.value = null
        _errorMessage.value = null
    }
}