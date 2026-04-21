package es.chefcore.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import es.chefcore.app.data.database.Albaran
import es.chefcore.app.data.database.AlbaranDao
import es.chefcore.app.logic.CocinaManager
import es.chefcore.app.logic.UnitConverter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AlbaranesScreen(aDao: AlbaranDao, cocinaManager: CocinaManager) {
    val albaranes by aDao.obtenerTodos().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var mostrarDialogo by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { mostrarDialogo = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Añadir Albarán")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Text("📄 Historial de Albaranes", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))

            if (albaranes.isEmpty()) {
                Text("No hay albaranes. Pulsa + para registrar una factura y sumar stock.")
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(albaranes) { albaran ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(albaran.proveedor, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    Text("${"%.2f".format(albaran.totalEuros)}€", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                }
                                Text("Fecha: ${albaran.fecha}", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }

    if (mostrarDialogo) {
        var proveedor by remember { mutableStateOf("") }
        var totalEuros by remember { mutableStateOf("") }
        val fechaHoy = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        var fecha by remember { mutableStateOf(fechaHoy) }

        // Datos del ingrediente que viene en el albarán
        var nombreIng by remember { mutableStateOf("") }
        var cantIng by remember { mutableStateOf("") }
        var unidadSeleccionada by remember { mutableStateOf("kg") }
        var menuExpandido by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { mostrarDialogo = false },
            title = { Text("Nuevo Albarán y Entrada de Stock") },
            text = {
                Column {
                    OutlinedTextField(value = proveedor, onValueChange = { proveedor = it }, label = { Text("Proveedor (Ej: Makro)") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Detalles del Producto:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(value = nombreIng, onValueChange = { nombreIng = it }, label = { Text("Ingrediente") }, modifier = Modifier.fillMaxWidth())

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = cantIng,
                            onValueChange = { cantIng = it },
                            label = { Text("Cant.") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Box(modifier = Modifier.weight(0.7f)) {
                            OutlinedButton(onClick = { menuExpandido = true }, modifier = Modifier.padding(top = 8.dp)) {
                                Text(unidadSeleccionada)
                            }
                            DropdownMenu(expanded = menuExpandido, onDismissRequest = { menuExpandido = false }) {
                                UnitConverter.TODAS_LAS_UNIDADES.forEach { u ->
                                    DropdownMenuItem(text = { Text(u) }, onClick = { unidadSeleccionada = u; menuExpandido = false })
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = totalEuros,
                        onValueChange = { totalEuros = it },
                        label = { Text("Total Factura (€)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        val total = totalEuros.replace(",", ".").toDoubleOrNull() ?: 0.0
                        val cantidad = cantIng.replace(",", ".").toDoubleOrNull() ?: 0.0

                        // 1. Registramos en el inventario (Lógica centralizada)
                        val exito = cocinaManager.registrarEntradaStock(nombreIng, cantidad, unidadSeleccionada, total)

                        if (exito) {
                            // 2. Registramos el albarán
                            aDao.insertar(Albaran(proveedor = proveedor.trim(), fecha = fecha, totalEuros = total))
                            mostrarDialogo = false
                        }
                    }
                }) { Text("Guardar Todo") }
            },
            dismissButton = { TextButton(onClick = { mostrarDialogo = false }) { Text("Cancelar") } }
        )
    }
}