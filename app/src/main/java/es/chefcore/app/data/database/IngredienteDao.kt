package es.chefcore.app.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface IngredienteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(ingrediente: Ingrediente)

    @Update
    suspend fun actualizar(ingrediente: Ingrediente)

    @Delete
    suspend fun eliminar(ingrediente: Ingrediente)


    @Query("SELECT * FROM ingredientes WHERE nombre = :nombre LIMIT 1")
    suspend fun buscarPorNombre(nombre: String): Ingrediente?

    @Query("SELECT * FROM ingredientes WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: Int): Ingrediente?

    @Query("SELECT * FROM ingredientes")
    fun obtenerTodos(): Flow<List<Ingrediente>>

    @Query("DELETE FROM ingredientes")
    suspend fun borrarTodos()

    @Query("SELECT * FROM ingredientes WHERE LOWER(nombre) = LOWER(:nombre) LIMIT 1")
    suspend fun buscarPorNombreIgnorandoCase(nombre: String): Ingrediente?
}