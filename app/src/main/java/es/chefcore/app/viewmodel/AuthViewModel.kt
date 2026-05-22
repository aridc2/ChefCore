package es.chefcore.app.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import es.chefcore.app.workers.SyncManager
import es.chefcore.app.data.database.ChefCoreDatabase
import es.chefcore.app.data.database.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * ViewModel de autenticación.
 * Gestiona el ciclo de registro, creación de PIN y login diario por PIN.
 * El estado Firebase se mantiene entre sesiones; el PIN se valida contra Room.
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val database = ChefCoreDatabase.getDatabase(application)
    private val usuarioDao = database.usuarioDao()

    private val prefs = application.getSharedPreferences("ChefCoreAuth", Context.MODE_PRIVATE)


    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Loading)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        checkInitialState()
    }

    /**
     * Determina la pantalla inicial según el estado de la sesión:
     * - Sin usuario Firebase → Registro
     * - Con usuario Firebase y PIN creado → Login con PIN
     * - Con usuario Firebase sin PIN → Crear PIN
     */
    private fun checkInitialState() {
        val firebaseUser = auth.currentUser
        val pinCreado = prefs.getBoolean("pin_creado", false)

        _uiState.value = when {
            firebaseUser == null -> AuthUiState.NeedRegister
            !pinCreado          -> AuthUiState.NeedCreatePin
            else                -> AuthUiState.NeedPinLogin
        }
    }

    /**
     * Registra un nuevo restaurante:
     * 1. Crea cuenta en Firebase Auth
     * 2. Guarda datos del restaurante en Firestore
     * 3. Guarda el gerente en Room como usuario local
     */
    fun registrarRestaurante(
        nombreRestaurante: String,
        email: String,
        password: String
    ) {
        if (nombreRestaurante.isBlank() || email.isBlank() || password.isBlank()) {
            _errorMessage.value = "Rellena todos los campos"
            return
        }
        if (password.length < 6) {
            _errorMessage.value = "La contraseña debe tener al menos 6 caracteres"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val uid = result.user?.uid ?: throw Exception("Error al crear usuario")

                val restauranteData = hashMapOf(
                    "nombre" to nombreRestaurante,
                    "ownerUid" to uid,
                    "email" to email,
                    "creadoEn" to Timestamp.now()
                )
                firestore.collection("restaurantes").document(uid).set(restauranteData).await()

                prefs.edit().putString("restaurante_nombre", nombreRestaurante).apply()
                prefs.edit().putString("owner_uid", uid).apply()

                _uiState.value = AuthUiState.NeedCreatePin

            } catch (e: Exception) {
                _errorMessage.value = when {
                    e.message?.contains("email address is already in use") == true ->
                        "Ese correo ya tiene una cuenta. Inicia sesión."
                    e.message?.contains("badly formatted") == true ->
                        "El formato del correo no es válido."
                    else -> "Error: ${e.message}"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Inicia sesión con email y contraseña (Firebase Auth)
     * Usado cuando el usuario no recuerda su PIN o en un dispositivo nuevo
     */
    fun loginConEmail(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _errorMessage.value = "Introduce tu correo y contraseña"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                auth.signInWithEmailAndPassword(email, password).await()
                val usuariosEnRoom = usuarioDao.obtenerTodos().first()
                if (usuariosEnRoom.isNotEmpty()) {
                    prefs.edit().putBoolean("pin_creado", true).apply()
                    _uiState.value = AuthUiState.NeedPinLogin
                } else {
                    _uiState.value = AuthUiState.NeedCreatePin
                }
            } catch (e: Exception) {
                _errorMessage.value = when {
                    e.message?.contains("password is invalid") == true ||
                            e.message?.contains("no user record") == true ->
                        "Correo o contraseña incorrectos."
                    else -> "Error al iniciar sesión: ${e.message}"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Guarda el PIN del gerente en Room y marca como creado en prefs
     */
    // PIN e ID del usuario actualmente autenticado (en memoria, nunca persiste)
    private var _currentUserPin: String = ""
    private var _currentUserId:  Int    = 0

    fun getCurrentUserId(): Int = _currentUserId

    /** Comprobación síncrona del PIN actual — para validar en el diálogo sin esperar corrutina. */
    fun checkCurrentPin(pin: String): Boolean = pin == _currentUserPin

    private val _pinChangeResult = MutableStateFlow<String?>(null)
    val pinChangeResult: StateFlow<String?> = _pinChangeResult.asStateFlow()

    fun guardarPin(pin: String) {
        viewModelScope.launch {
            val nombreGerente = prefs.getString("restaurante_nombre", "Gerente") ?: "Gerente"

            val gerente = Usuario(
                nombre = nombreGerente,
                pin = pin,
                rol = "Gerente"
            )
            usuarioDao.insertar(gerente)

            // Recuperar el ID generado por Room
            val insertado = usuarioDao.obtenerTodos().first().find { it.pin == pin }
            _currentUserId  = insertado?.id ?: 0
            _currentUserPin = pin
            prefs.edit().putString("pin_gerente", pin).apply()

            _currentUserPin = pin
            _uiState.value = AuthUiState.LoggedIn(rol = "Gerente")
            SyncManager.syncNow(getApplication())
        }
    }

    /**
     * Valida el PIN introducido contra los usuarios de Room.
     * Devuelve el rol del usuario si es correcto.
     */
    fun validarPin(pin: String, usuarios: List<Usuario>) {
        val usuario = usuarios.find { it.pin == pin }
        if (usuario != null) {
            _currentUserPin = pin
            _currentUserId  = usuario.id
            _uiState.value = AuthUiState.LoggedIn(rol = usuario.rol)
            SyncManager.syncNow(getApplication())
        } else {
            _errorMessage.value = "PIN incorrecto"
        }
    }

    /**
     * Cambia el PIN del usuario actualmente autenticado.
     * Requiere confirmar el PIN actual antes de cambiarlo.
     */
    fun cambiarPin(pinActual: String, pinNuevo: String) {
        viewModelScope.launch {
            if (pinActual != _currentUserPin) {
                _pinChangeResult.value = "error:El PIN actual no es correcto"
                return@launch
            }
            if (pinNuevo.length != 4 || !pinNuevo.all { it.isDigit() }) {
                _pinChangeResult.value = "error:El nuevo PIN debe tener exactamente 4 dígitos"
                return@launch
            }
            if (pinNuevo == pinActual) {
                _pinChangeResult.value = "error:El nuevo PIN debe ser diferente al actual"
                return@launch
            }

            try {
                val todos = usuarioDao.obtenerTodos().first()
                // Verificar que el nuevo PIN no esté ya en uso por otro usuario
                if (todos.any { it.pin == pinNuevo && it.pin != _currentUserPin }) {
                    _pinChangeResult.value = "error:Ese PIN ya lo usa otro empleado"
                    return@launch
                }

                val usuario = todos.find { it.pin == _currentUserPin }
                    ?: run { _pinChangeResult.value = "error:Usuario no encontrado"; return@launch }

                usuarioDao.actualizar(usuario.copy(pin = pinNuevo))
                _currentUserPin = pinNuevo

                // Si es el Gerente, actualizar también en prefs
                if (usuario.rol == "Gerente") {
                    prefs.edit().putString("pin_gerente", pinNuevo).apply()
                }

                _pinChangeResult.value = "ok:PIN actualizado correctamente"
            } catch (e: Exception) {
                _pinChangeResult.value = "error:Error al cambiar el PIN: ${e.message}"
            }
        }
    }

    fun clearPinChangeResult() { _pinChangeResult.value = null }

    fun cerrarSesion() {
        // NO se desconecta de Firebase — la cuenta del restaurante se mantiene
        // "Cerrar sesión" = volver a la pantalla de selección de perfil (cambio de turno)
        _currentUserPin = ""
        _currentUserId  = 0
        SyncManager.cancelAll(getApplication())
        _uiState.value = AuthUiState.NeedPinLogin
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun getNombreRestaurante(): String =
        prefs.getString("restaurante_nombre", "") ?: ""

    fun getFirebaseUser(): FirebaseUser? = auth.currentUser
}


sealed class AuthUiState {
    object Loading        : AuthUiState()  // Comprobando estado inicial
    object NeedRegister   : AuthUiState()  // Primera vez — ir a registro
    object NeedCreatePin  : AuthUiState()  // Registrado pero sin PIN
    object NeedPinLogin   : AuthUiState()  // Tiene cuenta — pedir PIN diario
    data class LoggedIn(val rol: String) : AuthUiState() // Autenticado
}