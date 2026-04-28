package es.chefcore.app.data.repository

import es.chefcore.app.data.database.Ingrediente
import es.chefcore.app.data.database.IngredienteDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Repository para gestión de ingredientes
 * Encapsula el acceso a datos y proporciona una API limpia al ViewModel
 */
class IngredienteRepository(private val ingredienteDao: IngredienteDao) {

    /**
     * Observa todos los ingredientes en tiempo real
     */
    fun obtenerTodos(): Flow<List<Ingrediente>> {
        return ingredienteDao.obtenerTodos()
    }

    /**
     * Busca un ingrediente por nombre
     */
    suspend fun buscarPorNombre(nombre: String): Ingrediente? {
        return ingredienteDao.buscarPorNombre(nombre)
    }

    /**
     * Busca un ingrediente por ID
     */
    suspend fun buscarPorId(id: Int): Ingrediente? {
        return ingredienteDao.obtenerTodos().first().find { it.id == id }
    }

    /**
     * Inserta un nuevo ingrediente
     */
    suspend fun insertar(ingrediente: Ingrediente) {
        ingredienteDao.insertar(ingrediente)
    }

    /**
     * Actualiza un ingrediente existente
     */
    suspend fun actualizar(ingrediente: Ingrediente) {
        ingredienteDao.actualizar(ingrediente)
    }

    /**
     * Elimina todos los ingredientes (útil para testing)
     */
    suspend fun borrarTodos() {
        ingredienteDao.borrarTodos()
    }
}
