package es.chefcore.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Si en el futuro añades "Receta" o "Albaran", los pondrás aquí dentro de 'entities'
@Database(
    entities = [
        Ingrediente::class,
        Usuario::class,
        Receta::class,
        Albaran::class
    ],
    version = 2,
    exportSchema = false
)
abstract class ChefCoreDatabase : RoomDatabase() {

    abstract fun ingredienteDao(): IngredienteDao
    abstract fun usuarioDao(): UsuarioDao
    abstract fun recetaDao(): RecetaDao
    abstract fun albaranDao(): AlbaranDao

    companion object {
        @Volatile
        private var INSTANCE: ChefCoreDatabase? = null

        fun getDatabase(context: Context): ChefCoreDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChefCoreDatabase::class.java,
                    "chefcore_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}