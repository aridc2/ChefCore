package es.chefcore.app.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(usuario: Usuario)

    @Update
    suspend fun actualizar(usuario: Usuario)

    @Delete
    suspend fun eliminar(usuario: Usuario)

    @Query("SELECT * FROM usuarios")
    fun obtenerTodos(): Flow<List<Usuario>>
}