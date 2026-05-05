package es.chefcore.app.data.repository

import es.chefcore.app.data.database.Usuario
import es.chefcore.app.data.database.UsuarioDao
import es.chefcore.app.data.database.Albaran
import es.chefcore.app.data.database.AlbaranDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Repository para gestión de usuarios/empleados
 */
class UsuarioRepository(private val usuarioDao: UsuarioDao) {

    fun obtenerTodos(): Flow<List<Usuario>> {
        return usuarioDao.obtenerTodos()
    }

    suspend fun insertar(usuario: Usuario) {
        usuarioDao.insertar(usuario)
    }

    suspend fun actualizar(usuario: Usuario) {
        usuarioDao.actualizar(usuario)
    }

    suspend fun eliminar(usuario: Usuario) {
        usuarioDao.eliminar(usuario)
    }
    suspend fun validarPin(pin: String): Usuario? {
        return usuarioDao.obtenerTodos().first().find { it.pin == pin }
    }

    suspend fun buscarPorNombre(nombre: String): Usuario? {

        return usuarioDao.obtenerTodos().first()
            .find { it.nombre.equals(nombre, ignoreCase = true) }
    }
}

/**
 * Repository para gestión de albaranes
 */
class AlbaranRepository(private val albaranDao: AlbaranDao) {

    fun obtenerTodos(): Flow<List<Albaran>> {
        return albaranDao.obtenerTodos()
    }

    suspend fun insertar(albaran: Albaran) {
        albaranDao.insertar(albaran)
    }
}
