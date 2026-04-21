package es.chefcore.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.chefcore.app.data.database.IngredienteDao
import es.chefcore.app.logic.CocinaManager
import es.chefcore.app.logic.UnitConverter
import kotlinx.coroutines.launch

@Composable
fun InventarioScreen(iDao: IngredienteDao, cocinaManager: CocinaManager) {
    val ingredientes by iDao.obtenerTodos().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var mostrarDialogo by remember { mutableStateOf(false) }

    // Fondo gris claro general de la app según Figma
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF3F4F6))) {
        Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {

            // --- CABECERA ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Espacio vacío a la izquierda para centrar el título
                Spacer(modifier = Modifier.width(160.dp))

                Text(
                    text = "Inventario",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937) // Gris oscuro corporativo [cite: 534]
                )

                Button(
                    onClick = { mostrarDialogo = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Añadir")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Añadir Producto", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            // --- CUADRÍCULA DE PRODUCTOS (GRID) ---
            if (ingredientes.isEmpty()) {
                Text("El inventario está vacío.", modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 220.dp), // Se adapta al tamaño de la tablet
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    items(ingredientes) { ing ->
                        // TARJETA DE PRODUCTO ESTILO FIGMA
                        Card(
                            modifier = Modifier.height(200.dp).fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Mitad superior gris (Placeholder foto)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .background(Color(0xFFE5E7EB))
                                )

                                // Mitad inferior blanca (Textos)
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = ing.nombre,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1F2937),
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Stock: ${UnitConverter.formatearCantidad(ing.cantidad, ing.unidad)}",
                                        fontSize = 14.sp,
                                        color = Color(0xFF6B7280) // Gris medio [cite: 534]
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIÁLOGO PARA AÑADIR (Se mantiene la lógica que ya teníamos) ---
    if (mostrarDialogo) {
        var nombre by remember { mutableStateOf("") }
        var cantidad by remember { mutableStateOf("") }
        var precioTotal by remember { mutableStateOf("") }
        var unidadSeleccionada by remember { mutableStateOf("kg") }
        var menuExpandido by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { mostrarDialogo = false },
            title = { Text("Añadir al Almacén") },
            text = {
                Column {
                    OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = cantidad,
                            onValueChange = { cantidad = it },
                            label = { Text("Cantidad") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Box(modifier = Modifier.weight(0.6f)) {
                            OutlinedButton(onClick = { menuExpandido = true }, modifier = Modifier.padding(top = 8.dp)) { Text(unidadSeleccionada) }
                            DropdownMenu(expanded = menuExpandido, onDismissRequest = { menuExpandido = false }) {
                                UnitConverter.TODAS_LAS_UNIDADES.forEach { u ->
                                    DropdownMenuItem(text = { Text(u) }, onClick = { unidadSeleccionada = u; menuExpandido = false })
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = precioTotal,
                        onValueChange = { precioTotal = it },
                        label = { Text("Precio Total Factura (€)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        val cant = cantidad.replace(",", ".").toDoubleOrNull() ?: 0.0
                        val precio = precioTotal.replace(",", ".").toDoubleOrNull() ?: 0.0

                        // USAMOS LA LÓGICA CENTRALIZADA DEL MANAGER
                        cocinaManager.registrarEntradaStock(nombre, cant, unidadSeleccionada, precio)
                        mostrarDialogo = false
                    }
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { mostrarDialogo = false }) { Text("Cancelar") } }
        )
    }
}