package es.chefcore.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Ingrediente::class,
        Usuario::class,
        Receta::class,
        Albaran::class,
        RecetaIngrediente::class // <--- NUEVA TABLA AÑADIDA AQUÍ
    ],
    version = 3, // <--- SUBIMOS LA VERSIÓN A 3
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
                    .fallbackToDestructiveMigration() // Al subir la versión, esto borrará los datos antiguos para aplicar la nueva estructura limpia
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}