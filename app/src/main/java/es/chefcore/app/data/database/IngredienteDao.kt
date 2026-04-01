package es.chefcore.app.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface IngredienteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(ingrediente: Ingrediente)

    @Query("SELECT * FROM ingredientes")
    fun obtenerTodos(): Flow<List<Ingrediente>>

    @Query("DELETE FROM ingredientes")
    suspend fun borrarTodos()
}