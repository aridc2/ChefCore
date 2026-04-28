package es.chefcore.app.data.repository

import es.chefcore.app.data.database.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository para gestión de recetas y sus ingredientes
 */
class RecetaRepository(private val recetaDao: RecetaDao) {
    
    // ========== OPERACIONES BÁSICAS DE RECETA ==========
    
    fun obtenerTodas(): Flow<List<Receta>> {
        return recetaDao.obtenerTodas()
    }
    
    suspend fun obtenerTodasStatic(): List<Receta> {
        return recetaDao.obtenerTodasStatic()
    }
    
    fun observarPorId(id: Int): Flow<Receta?> {
        return recetaDao.observarPorId(id)
    }
    
    suspend fun buscarPorId(id: Int): Receta? {
        return recetaDao.buscarPorId(id)
    }
    
    suspend fun buscarPorNombre(nombre: String): Receta? {
        return recetaDao.buscarPorNombre(nombre)
    }
    
    suspend fun insertar(receta: Receta) {
        recetaDao.insertar(receta)
    }
    
    suspend fun actualizar(receta: Receta) {
        recetaDao.actualizar(receta)
    }
    
    suspend fun eliminar(receta: Receta) {
        recetaDao.eliminar(receta)
    }
    
    // ========== GESTIÓN DE INGREDIENTES EN RECETA (ESCANDALLO) ==========
    
    fun observarIngredientes(recetaId: Int): Flow<List<IngredienteEnReceta>> {
        return recetaDao.observarIngredientesDeReceta(recetaId)
    }
    
    suspend fun obtenerIngredientesStatic(recetaId: Int): List<IngredienteEnReceta> {
        return recetaDao.obtenerIngredientesStatic(recetaId)
    }
    
    suspend fun añadirIngrediente(recetaIngrediente: RecetaIngrediente) {
        recetaDao.asociarIngrediente(recetaIngrediente)
    }
    
    suspend fun eliminarIngrediente(recetaId: Int, ingredienteId: Int) {
        recetaDao.desasociarIngrediente(recetaId, ingredienteId)
    }
    
    suspend fun actualizarCantidadIngrediente(
        recetaId: Int,
        ingredienteId: Int,
        nuevaCantidad: Double
    ) {
        recetaDao.actualizarCantidadIngrediente(recetaId, ingredienteId, nuevaCantidad)
    }
    
    suspend fun eliminarTodosIngredientes(recetaId: Int) {
        recetaDao.eliminarTodosIngredientesDeReceta(recetaId)
    }
    
    suspend fun existeIngredienteEnReceta(recetaId: Int, ingredienteId: Int): Boolean {
        return recetaDao.existeIngredienteEnReceta(recetaId, ingredienteId) > 0
    }
}
