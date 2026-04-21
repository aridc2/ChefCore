package es.chefcore.app.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface IngredienteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(ingrediente: Ingrediente)

    @Update // <--- Necesario para actualizar el stock
    suspend fun actualizar(ingrediente: Ingrediente)

    @Query("SELECT * FROM ingredientes WHERE nombre = :nombre LIMIT 1") // <--- Busca si ya existe
    suspend fun buscarPorNombre(nombre: String): Ingrediente?

    @Query("SELECT * FROM ingredientes")
    fun obtenerTodos(): Flow<List<Ingrediente>>

    @Query("DELETE FROM ingredientes")
    suspend fun borrarTodos()
}