package es.chefcore.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import es.chefcore.app.data.database.Albaran
import es.chefcore.app.data.database.ChefCoreDatabase
import es.chefcore.app.data.database.Usuario
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlbaranViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ChefCoreDatabase.getDatabase(application)
    private val albaranDao = db.albaranDao()
    private val usuarioDao = db.usuarioDao()

    val albaranes: StateFlow<List<Albaran>> = albaranDao.obtenerTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val usuarios: StateFlow<List<Usuario>> = usuarioDao.obtenerTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun nombreDeUsuario(idUsuario: Int): String {
        return usuarios.value.find { it.id == idUsuario }?.nombre ?: "Desconocido"
    }

    fun eliminar(albaran: Albaran) {
        viewModelScope.launch {
            albaranDao.eliminar(albaran.id)
        }
    }
}