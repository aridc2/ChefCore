package es.chefcore.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import es.chefcore.app.data.database.ChefCoreDatabase
import es.chefcore.app.logic.VoiceCommander
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel del módulo de voz.
 * Recibe el texto reconocido por SpeechRecognizer y lo delega a VoiceCommander.
 * Expone señales de navegación separadas del feedback de operación.
 */
class VoiceViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ChefCoreDatabase.getDatabase(application)

    private val commander = VoiceCommander(
        iDao  = db.ingredienteDao(),
        rDao  = db.recetaDao(),
        aDao  = db.albaranDao(),
        scope = viewModelScope
    )

    private val _feedback = MutableStateFlow<String?>(null)
    val feedback: StateFlow<String?> = _feedback.asStateFlow()

    private val _navSenal = MutableStateFlow<String?>(null)
    val navSenal: StateFlow<String?> = _navSenal.asStateFlow()

    val recetaActiva: StateFlow<String> = commander.recetaActiva

    fun procesarVoz(texto: String) {
        commander.ejecutarComando(texto) { msg ->
            when {
                msg.startsWith("NAV_DETALLE|") -> _navSenal.value = "DETALLE|" + msg.substringAfter("NAV_DETALLE|")
                msg.startsWith("NAV_CREAR|")   -> _navSenal.value = "CREAR|"   + msg.substringAfter("NAV_CREAR|")
                msg.startsWith("NAV_EDITAR|")  -> _navSenal.value = "EDITAR|"  + msg.substringAfter("NAV_EDITAR|")
                else -> _feedback.value = msg
            }
        }
    }

    fun clearFeedback() { _feedback.value = null }
    fun clearNavSenal() { _navSenal.value = null }
}