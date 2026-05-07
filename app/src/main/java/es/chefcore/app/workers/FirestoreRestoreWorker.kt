package es.chefcore.app.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import es.chefcore.app.data.database.ChefCoreDatabase
import es.chefcore.app.data.database.Ingrediente
import es.chefcore.app.data.database.Receta
import es.chefcore.app.data.database.RecetaIngrediente
import kotlinx.coroutines.tasks.await

/**
 * Worker que descarga los datos de Firestore y los restaura en Room local.
 * Orden importante: Ingredientes → Recetas → Escandallos (respeta ForeignKeys)
 */
class FirestoreRestoreWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "FirestoreRestoreWorker"
    }

    override suspend fun doWork(): Result {
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return Result.failure()

        val db = ChefCoreDatabase.getDatabase(applicationContext)
        val firestore = FirebaseFirestore.getInstance()
        val restauranteRef = firestore.collection("restaurantes").document(uid)

        return try {
            restaurarIngredientes(db, restauranteRef)
            restaurarRecetas(db, restauranteRef)
            restaurarEscandallos(db, restauranteRef)
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error en la restauración", e)
            Result.retry()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun restaurarIngredientes(
        db: ChefCoreDatabase,
        restauranteRef: com.google.firebase.firestore.DocumentReference
    ) {
        val doc = restauranteRef.collection("ingredientes")
            .document("snapshot").get().await()

        val items = doc.get("items") as? List<Map<String, Any>> ?: return

        items.forEach { map ->
            val ing = Ingrediente(
                id = (map["id"] as? Number)?.toInt() ?: 0,
                nombre = map["nombre"] as? String ?: "",
                cantidad = (map["cantidad"] as? Number)?.toDouble() ?: 0.0,
                unidad = map["unidad"] as? String ?: "kg",
                precio = (map["precio"] as? Number)?.toDouble() ?: 0.0,
                imagenUri = (map["imagenUri"] as? String)?.ifEmpty { null }
            )
            db.ingredienteDao().insertar(ing)
        }
        Log.d(TAG, "Restaurados ${items.size} ingredientes")
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun restaurarRecetas(
        db: ChefCoreDatabase,
        restauranteRef: com.google.firebase.firestore.DocumentReference
    ) {
        val doc = restauranteRef.collection("recetas")
            .document("snapshot").get().await()

        val items = doc.get("items") as? List<Map<String, Any>> ?: return

        items.forEach { map ->
            val receta = Receta(
                id = (map["id"] as? Number)?.toInt() ?: 0,
                nombre = map["nombre"] as? String ?: "",
                precioVenta = (map["precioVenta"] as? Number)?.toDouble() ?: 0.0,
                tiempoPreparacionMinutos = (map["tiempoPreparacionMinutos"] as? Number)?.toInt() ?: 30,
                instrucciones = map["instrucciones"] as? String ?: ""
            )
            db.recetaDao().insertarConReemplazo(receta)
        }
        Log.d(TAG, "Restauradas ${items.size} recetas")
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun restaurarEscandallos(
        db: ChefCoreDatabase,
        restauranteRef: com.google.firebase.firestore.DocumentReference
    ) {
        val doc = restauranteRef.collection("escandallos")
            .document("snapshot").get().await()

        val items = doc.get("items") as? List<Map<String, Any>> ?: return

        db.recetaDao().borrarTodosEscandallos()

        var insertados = 0
        items.forEach { map ->
            // FIX: as? Number en lugar de as? Long — Firestore puede devolver Int o Long
            val recetaId = (map["recetaId"] as? Number)?.toInt() ?: run {
                Log.w(TAG, "recetaId nulo o tipo inesperado en escandallo: $map")
                return@forEach
            }
            val ingredienteId = (map["ingredienteId"] as? Number)?.toInt() ?: run {
                Log.w(TAG, "ingredienteId nulo o tipo inesperado en escandallo: $map")
                return@forEach
            }
            val cantidad = (map["cantidadNecesaria"] as? Number)?.toDouble() ?: 0.0

            val rel = RecetaIngrediente(
                recetaId = recetaId,
                ingredienteId = ingredienteId,
                cantidadNecesaria = cantidad
            )
            db.recetaDao().asociarIngredienteConReemplazo(rel)
            insertados++
        }
        Log.d(TAG, "Restaurados $insertados/${items.size} escandallos")
    }
}