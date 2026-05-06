package es.chefcore.app.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.*

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("ChefCoreSettings", Context.MODE_PRIVATE)


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


    private val _cameraPermissionGranted = MutableStateFlow(false)
    val cameraPermissionGranted: StateFlow<Boolean> = _cameraPermissionGranted.asStateFlow()

    fun updateCameraPermissionStatus(granted: Boolean) {
        _cameraPermissionGranted.value = granted
    }


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