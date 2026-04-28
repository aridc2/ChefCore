package es.chefcore.app.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel para SettingsScreen
 * Gestiona configuración de la app y permisos
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val prefs = application.getSharedPreferences("ChefCoreSettings", Context.MODE_PRIVATE)
    
    // ========== CONFIGURACIÓN DE NEGOCIO ==========
    
    private val _currency = MutableStateFlow(prefs.getString("currency", "EUR") ?: "EUR")
    val currency: StateFlow<String> = _currency.asStateFlow()
    
    private val _ivaPercentage = MutableStateFlow(prefs.getFloat("iva", 21f))
    val ivaPercentage: StateFlow<Float> = _ivaPercentage.asStateFlow()
    
    fun setCurrency(value: String) {
        _currency.value = value
        prefs.edit().putString("currency", value).apply()
    }
    
    fun setIva(value: Float) {
        _ivaPercentage.value = value
        prefs.edit().putFloat("iva", value).apply()
    }
    
    // ========== CONFIGURACIÓN DE INTERFAZ ==========
    
    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("darkMode", false))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()
    
    private val _fontSize = MutableStateFlow(prefs.getFloat("fontSize", 1f))
    val fontSize: StateFlow<Float> = _fontSize.asStateFlow()
    
    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        prefs.edit().putBoolean("darkMode", enabled).apply()
    }
    
    fun setFontSize(multiplier: Float) {
        _fontSize.value = multiplier
        prefs.edit().putFloat("fontSize", multiplier).apply()
    }
    
    // ========== CONFIGURACIÓN DE VOZ ==========
    
    private val _voiceEnabled = MutableStateFlow(prefs.getBoolean("voiceEnabled", true))
    val voiceEnabled: StateFlow<Boolean> = _voiceEnabled.asStateFlow()
    
    private val _voiceLanguage = MutableStateFlow(prefs.getString("voiceLang", "Español") ?: "Español")
    val voiceLanguage: StateFlow<String> = _voiceLanguage.asStateFlow()
    
    fun setVoiceEnabled(enabled: Boolean) {
        _voiceEnabled.value = enabled
        prefs.edit().putBoolean("voiceEnabled", enabled).apply()
    }
    
    fun setVoiceLanguage(language: String) {
        _voiceLanguage.value = language
        prefs.edit().putString("voiceLang", language).apply()
    }
    
    // ========== PERMISOS DE CÁMARA ==========
    
    private val _cameraPermissionGranted = MutableStateFlow(false)
    val cameraPermissionGranted: StateFlow<Boolean> = _cameraPermissionGranted.asStateFlow()
    
    private val _shouldShowRationale = MutableStateFlow(false)
    val shouldShowRationale: StateFlow<Boolean> = _shouldShowRationale.asStateFlow()
    
    fun updateCameraPermissionStatus(granted: Boolean) {
        _cameraPermissionGranted.value = granted
    }
    
    fun setShouldShowRationale(show: Boolean) {
        _shouldShowRationale.value = show
    }
    
    // ========== DATOS ==========
    
    private val _showDeleteConfirmation = MutableStateFlow(false)
    val showDeleteConfirmation: StateFlow<Boolean> = _showDeleteConfirmation.asStateFlow()
    
    fun setShowDeleteConfirmation(show: Boolean) {
        _showDeleteConfirmation.value = show
    }
    
    /**
     * Borra toda la base de datos
     * PELIGRO: Esta acción es irreversible
     */
    fun deleteAllData(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {

                prefs.edit().clear().apply()
                
                _currency.value = "EUR"
                _ivaPercentage.value = 21f
                _isDarkMode.value = false
                _fontSize.value = 1f
                _voiceEnabled.value = true
                _voiceLanguage.value = "Español"
                
                onComplete()
            } catch (e: Exception) {
            }
        }
    }
    
    // ========== ESTADÍSTICAS (OPCIONAL) ==========

    fun getAppVersion(): String {
        return try {
            val context = getApplication<Application>()
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }
}
