package es.chefcore.app.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import es.chefcore.app.data.database.ChefCoreDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

/**
 * Worker que sincroniza los datos locales (Room) con Firestore en segundo plano.
 * Solo se ejecuta cuando hay conexión a internet.
 * Cumple RF-05: Sincronización Híbrida Offline-First con WorkManager.
 */
class FirestoreSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "FirestoreSyncWorker"
    }

    override suspend fun doWork(): Result {
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return Result.failure()

        val db = ChefCoreDatabase.getDatabase(applicationContext)
        val firestore = FirebaseFirestore.getInstance()
        val restauranteRef = firestore.collection("restaurantes").document(uid)

        return try {
            val hayIngredientes = db.ingredienteDao().obtenerTodos().first().isNotEmpty()
            val hayRecetas      = db.recetaDao().obtenerTodasStatic().isNotEmpty()
            val hayUsuarios     = db.usuarioDao().obtenerTodos().first().isNotEmpty()

            if (!hayIngredientes && !hayRecetas && !hayUsuarios) {
                Log.w(TAG, "BD local vacía — sincronización abortada para proteger la copia en la nube")
                return Result.success()   // success para que WorkManager no reintente en bucle
            }

            sincronizarIngredientes(db, restauranteRef)
            sincronizarRecetas(db, restauranteRef)
            sincronizarEscandallos(db, restauranteRef)
            sincronizarUsuarios(db, restauranteRef)
            sincronizarAlbaranes(db, restauranteRef)
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error en la sincronización", e)
            Result.retry()
        }
    }

    private suspend fun sincronizarIngredientes(
        db: ChefCoreDatabase,
        restauranteRef: com.google.firebase.firestore.DocumentReference
    ) {
        val ingredientes = db.ingredienteDao().obtenerTodos().first()
        val data = ingredientes.map { ing ->
            mapOf(
                "id" to ing.id,
                "nombre" to ing.nombre,
                "cantidad" to ing.cantidad,
                "unidad" to ing.unidad,
                "precio" to ing.precio,
                "imagenUri" to (ing.imagenUri ?: "")
            )
        }
        restauranteRef.collection("ingredientes")
            .document("snapshot")
            .set(mapOf("items" to data, "syncedAt" to com.google.firebase.Timestamp.now()))
            .await()
    }

    private suspend fun sincronizarRecetas(
        db: ChefCoreDatabase,
        restauranteRef: com.google.firebase.firestore.DocumentReference
    ) {
        val recetas = db.recetaDao().obtenerTodasStatic()
        val data = recetas.map { receta ->
            mapOf(
                "id" to receta.id,
                "nombre" to receta.nombre,
                "precioVenta" to receta.precioVenta,
                "tiempoPreparacionMinutos" to receta.tiempoPreparacionMinutos,
                "instrucciones" to receta.instrucciones
            )
        }
        restauranteRef.collection("recetas")
            .document("snapshot")
            .set(mapOf("items" to data, "syncedAt" to com.google.firebase.Timestamp.now()))
            .await()
    }

    private suspend fun sincronizarUsuarios(
        db: ChefCoreDatabase,
        restauranteRef: com.google.firebase.firestore.DocumentReference
    ) {
        val usuarios = db.usuarioDao().obtenerTodos().first()
        val data = usuarios.map { u ->
            mapOf(
                "id" to u.id,
                "nombre" to u.nombre,
                "rol" to u.rol
            )
        }
        restauranteRef.collection("usuarios")
            .document("snapshot")
            .set(mapOf("items" to data, "syncedAt" to com.google.firebase.Timestamp.now()))
            .await()
    }

    private suspend fun sincronizarAlbaranes(
        db: ChefCoreDatabase,
        restauranteRef: com.google.firebase.firestore.DocumentReference
    ) {
        val albaranes = db.albaranDao().obtenerTodos().first()
        val data = albaranes.map { a ->
            mapOf(
                "id" to a.id,
                "fecha" to a.fecha,
                "proveedor" to a.proveedor,
                "totalEuros" to a.totalEuros,
                "idUsuario" to a.idUsuario
            )
        }
        restauranteRef.collection("albaranes")
            .document("snapshot")
            .set(mapOf("items" to data, "syncedAt" to com.google.firebase.Timestamp.now()))
            .await()
    }

    private suspend fun sincronizarEscandallos(
        db: ChefCoreDatabase,
        restauranteRef: com.google.firebase.firestore.DocumentReference
    ) {
        val recetas = db.recetaDao().obtenerTodasStatic()
        val data = recetas.flatMap { receta ->
            db.recetaDao().obtenerIngredientesStatic(receta.id).map { ing ->
                mapOf(
                    "recetaId"          to receta.id,
                    "ingredienteId"     to ing.ingredienteId,
                    "cantidadNecesaria" to ing.cantidadNecesaria
                )
            }
        }
        restauranteRef.collection("escandallos")
            .document("snapshot")
            .set(mapOf("items" to data, "syncedAt" to com.google.firebase.Timestamp.now()))
            .await()
    }
}