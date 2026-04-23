package es.chefcore.app.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecetaDao {
    @Insert
    suspend fun insertar(receta: Receta)

    @Update
    suspend fun actualizar(receta: Receta)

    @Delete
    suspend fun eliminar(receta: Receta)

    @Query("SELECT * FROM recetas WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: Int): Receta?

    @Query("SELECT * FROM recetas WHERE id = :id LIMIT 1")
    fun observarPorId(id: Int): Flow<Receta?>

    @Query("SELECT * FROM recetas WHERE nombre = :nombre LIMIT 1")
    suspend fun buscarPorNombre(nombre: String): Receta?

    @Query("SELECT * FROM recetas WHERE nombre = :nombre LIMIT 1")
    fun observarPorNombre(nombre: String): Flow<Receta?>

    @Query("SELECT * FROM recetas")
    fun obtenerTodas(): Flow<List<Receta>>

    @Query("SELECT * FROM recetas")
    suspend fun obtenerTodasStatic(): List<Receta>

    // ========== GESTIÓN DE INGREDIENTES EN RECETA ==========

    /**
     * Añade un ingrediente a una receta (crea la relación)
     */
    @Insert
    suspend fun asociarIngrediente(relacion: RecetaIngrediente)

    /**
     * Elimina un ingrediente de una receta
     */
    @Query("DELETE FROM receta_ingrediente WHERE recetaId = :recetaId AND ingredienteId = :ingredienteId")
    suspend fun desasociarIngrediente(recetaId: Int, ingredienteId: Int)

    /**
     * Elimina todos los ingredientes de una receta
     */
    @Query("DELETE FROM receta_ingrediente WHERE recetaId = :recetaId")
    suspend fun eliminarTodosIngredientesDeReceta(recetaId: Int)

    /**
     * Actualiza la cantidad necesaria de un ingrediente en una receta
     */
    @Query("UPDATE receta_ingrediente SET cantidadNecesaria = :nuevaCantidad WHERE recetaId = :recetaId AND ingredienteId = :ingredienteId")
    suspend fun actualizarCantidadIngrediente(recetaId: Int, ingredienteId: Int, nuevaCantidad: Double)

    /**
     * Obtiene los ingredientes de una receta (observable en tiempo real)
     */
    @Query("""
        SELECT i.id as ingredienteId, i.nombre, ri.cantidadNecesaria, i.unidad, i.precio as precioUnitario 
        FROM receta_ingrediente ri 
        INNER JOIN ingredientes i ON ri.ingredienteId = i.id 
        WHERE ri.recetaId = :recetaId
        ORDER BY i.nombre ASC
    """)
    fun observarIngredientesDeReceta(recetaId: Int): Flow<List<IngredienteEnReceta>>

    /**
     * Obtiene los ingredientes de una receta (snapshot estático)
     */
    @Query("""
        SELECT i.id as ingredienteId, i.nombre, ri.cantidadNecesaria, i.unidad, i.precio as precioUnitario 
        FROM receta_ingrediente ri 
        INNER JOIN ingredientes i ON ri.ingredienteId = i.id 
        WHERE ri.recetaId = :recetaId
        ORDER BY i.nombre ASC
    """)
    suspend fun obtenerIngredientesStatic(recetaId: Int): List<IngredienteEnReceta>

    /**
     * Verifica si un ingrediente ya está en la receta
     */
    @Query("SELECT COUNT(*) FROM receta_ingrediente WHERE recetaId = :recetaId AND ingredienteId = :ingredienteId")
    suspend fun existeIngredienteEnReceta(recetaId: Int, ingredienteId: Int): Int
}
