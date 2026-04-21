package es.chefcore.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import es.chefcore.app.data.database.Receta
import es.chefcore.app.data.database.RecetaDao
import es.chefcore.app.ui.navigation.Ruta
import kotlinx.coroutines.launch

@Composable
fun RecetasScreen(rDao: RecetaDao, navController: NavController) {
    val recetas by rDao.obtenerTodas().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var mostrarDialogo by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { mostrarDialogo = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Crear Receta")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Text("📖 Mis Recetas", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))

            if (recetas.isEmpty()) {
                Text("No tienes recetas. Pulsa el botón + para crear una.")
            } else {
                LazyColumn {
                    items(recetas) { receta ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    // AL HACER CLIC, VIAJAMOS AL DETALLE DE LA RECETA
                                    navController.navigate(Ruta.DetalleReceta.crearRuta(receta.nombre))
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(receta.nombre, style = MaterialTheme.typography.titleLarge)
                                Text("Precio de venta: ${receta.precioVenta}€", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }

    // FORMULARIO MODAL PARA CREAR RECETA
    if (mostrarDialogo) {
        var nombre by remember { mutableStateOf("") }
        var precioVenta by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { mostrarDialogo = false },
            title = { Text("Nueva Receta") },
            text = {
                Column {
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text("Nombre del plato") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = precioVenta,
                        onValueChange = { precioVenta = it },
                        label = { Text("Precio de Venta al cliente (€)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        val nuevaReceta = Receta(
                            nombre = nombre.trim().replaceFirstChar { it.uppercase() },
                            precioVenta = precioVenta.replace(",", ".").toDoubleOrNull() ?: 0.0
                        )
                        rDao.insertar(nuevaReceta)
                        mostrarDialogo = false
                    }
                }) { Text("Crear") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogo = false }) { Text("Cancelar") }
            }
        )
    }
}