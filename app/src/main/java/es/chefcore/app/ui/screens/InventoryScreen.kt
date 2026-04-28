package es.chefcore.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import es.chefcore.app.logic.UnitConverter
import es.chefcore.app.ui.components.InventoryProductCard
import es.chefcore.app.ui.components.Sidebar
import es.chefcore.app.ui.theme.ChefCoreColors
import es.chefcore.app.viewmodel.InventoryViewModel
import kotlinx.coroutines.delay


@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel = viewModel(),
    onSettingsClick: () -> Unit,
    onRecipesClick: () -> Unit,
    onPersonalClick: () -> Unit,
    onScannerClick: () -> Unit
) {
    // ========== OBSERVAR ESTADOS DEL VIEWMODEL ==========
    val ingredientes by viewModel.ingredientes.collectAsState()
    val feedbackMessage by viewModel.feedbackMessage.collectAsState()
    val errorIncompatible by viewModel.errorIncompatible.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Estado local solo para UI
    var mostrarDialogo by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(color = ChefCoreColors.BackgroundLight)
        ) {
            // Sidebar
            Sidebar(
                currentScreen = "Inventory",
                onSettingsClick = onSettingsClick,
                onInventoryClick = { },
                onRecipesClick = onRecipesClick,
                onScannerClick = onScannerClick,
                onPersonalClick = onPersonalClick
            )

            // Contenido principal
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Inventario",
                            style = MaterialTheme.typography.displaySmall,
                            color = ChefCoreColors.TextDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${ingredientes.size} productos",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ChefCoreColors.TextMedium
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                        // 1. Escanear Albarán (Navega a la cámara)
                        FilledTonalIconButton(
                            onClick = onScannerClick,
                            modifier = Modifier.size(80.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = ChefCoreColors.SurfaceGray
                            )
                        ) {
                            Icon(
                                Icons.Default.ReceiptLong,
                                contentDescription = "Escanear Albarán",
                                tint = ChefCoreColors.TextDark,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        // 2. Añadir por Voz
                        FilledTonalIconButton(
                            onClick = {
                                /* TODO: Lanzar VoiceViewModel para escuchar ingrediente */
                            },
                            modifier = Modifier.size(80.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = ChefCoreColors.AccentYellow
                            )
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = "Voz",
                                tint = ChefCoreColors.TextDark,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        // 3. Añadir Manual (Abre el diálogo actual)
                        Button(
                            onClick = { mostrarDialogo = true },
                            modifier = Modifier
                                .size(120.dp),
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ChefCoreColors.PrimaryGreen,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Añadir manual",
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (ingredientes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "El inventario está vacío. Pulsa 'Añadir Producto' para empezar.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = ChefCoreColors.TextMedium
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(ingredientes) { ing ->
                            InventoryProductCard(
                                name = ing.nombre,
                                stock = "Stock: ${UnitConverter.formatearCantidad(ing.cantidad, ing.unidad)}",
                                onClick = { /* TODO: Navegar a detalle */ }
                            )
                        }
                    }
                }
            }
        }

        // Snackbar host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }

    // ========== DIÁLOGO PARA AÑADIR PRODUCTO ==========
    if (mostrarDialogo) {
        AñadirProductoDialog(
            onDismiss = { mostrarDialogo = false },
            onConfirm = { nombre, cantidad, unidad, precio ->
                viewModel.añadirStock(nombre, cantidad, unidad, precio)
                mostrarDialogo = false
            }
        )
    }

    // ========== MOSTRAR FEEDBACK MESSAGE ==========
    feedbackMessage?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            delay(3000)
            viewModel.clearFeedback()
        }
    }

    // ========== DIÁLOGO DE ERROR DE UNIDADES INCOMPATIBLES ==========
    errorIncompatible?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.clearFeedback() },
            title = { Text("Unidades incompatibles") },
            text = {
                Column {
                    Text(error.mensaje)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "El ingrediente '${error.ingredienteExistente.nombre}' usa '${error.ingredienteExistente.unidad}'. " +
                                "No puedes añadir '${error.unidadIntentada}'.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ChefCoreColors.TextMedium
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearFeedback() }) {
                    Text("Entendido")
                }
            }
        )
    }
}

/**
 * Diálogo reutilizable para añadir productos
 */
@Composable
private fun AñadirProductoDialog(
    onDismiss: () -> Unit,
    onConfirm: (nombre: String, cantidad: Double, unidad: String, precio: Double) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var cantidad by remember { mutableStateOf("") }
    var precioTotal by remember { mutableStateOf("") }
    var unidadSeleccionada by remember { mutableStateOf("kg") }
    var menuExpandido by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir al Almacén") },
        text = {
            Column {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre del producto") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = cantidad,
                        onValueChange = { cantidad = it },
                        label = { Text("Cantidad") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    Box(modifier = Modifier.weight(0.6f)) {
                        OutlinedButton(
                            onClick = { menuExpandido = true },
                            modifier = Modifier.padding(top = 8.dp)
                        ) { Text(unidadSeleccionada) }
                        DropdownMenu(
                            expanded = menuExpandido,
                            onDismissRequest = { menuExpandido = false }
                        ) {
                            UnitConverter.TODAS_LAS_UNIDADES.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text(u) },
                                    onClick = {
                                        unidadSeleccionada = u
                                        menuExpandido = false
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = precioTotal,
                    onValueChange = { precioTotal = it },
                    label = { Text("Precio Total (€)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cant = cantidad.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val precio = precioTotal.replace(",", ".").toDoubleOrNull() ?: 0.0
                    if (nombre.isNotBlank() && cant > 0) {
                        onConfirm(nombre, cant, unidadSeleccionada, precio)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ChefCoreColors.PrimaryGreen)
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
