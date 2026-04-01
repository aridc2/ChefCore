package es.chefcore.app.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbaranDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(albaran: Albaran)

    @Query("SELECT * FROM albaranes")
    fun obtenerTodos(): Flow<List<Albaran>>
}