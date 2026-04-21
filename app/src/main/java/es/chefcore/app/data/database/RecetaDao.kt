package es.chefcore.app.data.database

import androidx.room.Dao
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

    @Query("SELECT * FROM recetas WHERE nombre = :nombre LIMIT 1")
    suspend fun buscarPorNombre(nombre: String): Receta?

    @Query("SELECT * FROM recetas WHERE nombre = :nombre LIMIT 1")
    fun observarPorNombre(nombre: String): Flow<Receta?>

    @Query("SELECT * FROM recetas")
    fun obtenerTodas(): Flow<List<Receta>>

    // --- NUEVAS FUNCIONES RELACIONALES ---

    // 1. Añadir un ingrediente a una receta
    @Insert
    suspend fun asociarIngrediente(relacion: RecetaIngrediente)

    // 2. Obtener los ingredientes de una receta para mostrarlos en la UI (En directo)
    @Query("""
        SELECT i.id as ingredienteId, i.nombre, ri.cantidadNecesaria, i.unidad, i.precio as precioUnitario 
        FROM receta_ingrediente ri 
        INNER JOIN ingredientes i ON ri.ingredienteId = i.id 
        WHERE ri.recetaId = :recetaId
    """)
    fun observarIngredientesDeReceta(recetaId: Int): Flow<List<IngredienteEnReceta>>

    // 3. Obtener los ingredientes de una receta (Para hacer el cálculo en segundo plano)
    @Query("""
        SELECT i.id as ingredienteId, i.nombre, ri.cantidadNecesaria, i.unidad, i.precio as precioUnitario 
        FROM receta_ingrediente ri 
        INNER JOIN ingredientes i ON ri.ingredienteId = i.id 
        WHERE ri.recetaId = :recetaId
    """)
    suspend fun obtenerIngredientesStatic(recetaId: Int): List<IngredienteEnReceta>
}