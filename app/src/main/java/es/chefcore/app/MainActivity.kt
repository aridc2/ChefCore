package es.chefcore.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import es.chefcore.app.data.database.ChefCoreDatabase
import es.chefcore.app.data.database.Ingrediente
import es.chefcore.app.data.database.Receta
import es.chefcore.app.data.database.Usuario


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PruebaBaseDatosScreen()
                }
            }
        }
    }
}

@Composable
fun PruebaBaseDatosScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { ChefCoreDatabase.getDatabase(context) }

    val iDao = db.ingredienteDao()
    val uDao = db.usuarioDao()
    val rDao = db.recetaDao()
    val aDao = db.albaranDao()

    val ingredientes by iDao.obtenerTodos().collectAsState(initial = emptyList())
    val usuarios by uDao.obtenerTodos().collectAsState(initial = emptyList())
    val recetas by rDao.obtenerTodos().collectAsState(initial = emptyList())
    val albaranes by aDao.obtenerTodos().collectAsState(initial = emptyList())

    LazyColumn(modifier = Modifier.padding(16.dp)) {
        item {
            Text("PANEL DE CONTROL DE BASE DE DATOS", style = MaterialTheme.typography.headlineMedium)

            Row {
                Button(onClick = { scope.launch { iDao.insertar(Ingrediente(nombre = "Harina", cantidad = 10.0, unidad = "kg")) } }) { Text("+Ingr") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { scope.launch { uDao.insertar(
                    Usuario(
                        nombre = "Chef Pepe",
                        pin = "1234",
                        rol = "Admin"
                    )
                ) } }) { Text("+User") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { scope.launch { rDao.insertar(
                    Receta(
                        nombre = "Tortilla",
                        tiempoPreparacionMinutos = 15,
                        instrucciones = "Batir huevos..."
                    )
                ) } }) { Text("+Receta") }
            }

            Spacer(Modifier.height(20.dp))
        }

        // SECCIÓN INGREDIENTES
        item { Text("INGREDIENTES (${ingredientes.size})", style = MaterialTheme.typography.titleLarge) }
        items(ingredientes) { Text("- ${it.nombre}: ${it.cantidad} ${it.unidad}") }

        // SECCIÓN USUARIOS
        item { Spacer(Modifier.height(10.dp)); Text("USUARIOS (${usuarios.size})", style = MaterialTheme.typography.titleLarge) }
        items(usuarios) { Text("- ${it.nombre} [${it.rol}]") }

        // SECCIÓN RECETAS
        item { Spacer(Modifier.height(10.dp)); Text("RECETAS (${recetas.size})", style = MaterialTheme.typography.titleLarge) }
        items(recetas) { Text("- ${it.nombre} (${it.tiempoPreparacionMinutos} min)") }
    }
}