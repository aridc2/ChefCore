package es.chefcore.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import es.chefcore.app.data.database.Ingrediente
import es.chefcore.app.ui.components.ConfirmDeleteDialog
import es.chefcore.app.ui.components.EditarIngredienteDialog
import es.chefcore.app.ui.components.IngredienteItemConAcciones
import es.chefcore.app.ui.components.Sidebar
import es.chefcore.app.ui.theme.ChefCoreColors
import es.chefcore.app.viewmodel.InventoryViewModel


@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel = viewModel(),
    onSettingsClick: () -> Unit,
    onRecipesClick: () -> Unit,
    onPersonalClick: () -> Unit,
    onScannerClick: () -> Unit
) {
    val ingredientes by viewModel.ingredientes.collectAsState()
    val feedbackMessage by viewModel.feedbackMessage.collectAsState()

    // Estados locales
    var searchQuery by remember { mutableStateOf("") }
    var mostrarDialogoAñadir by remember { mutableStateOf(false) }
    var ingredienteAEditar by remember { mutableStateOf<Ingrediente?>(null) }
    var ingredienteAEliminar by remember { mutableStateOf<Ingrediente?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedback()
        }
    }

    val ingredientesFiltrados = remember(ingredientes, searchQuery) {
        if (searchQuery.isBlank()) ingredientes
        else ingredientes.filter { it.nombre.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(color = ChefCoreColors.BackgroundLight)
        ) {
            Sidebar(
                currentScreen = "Inventory",
                onSettingsClick = onSettingsClick,
                onInventoryClick = { },
                onRecipesClick = onRecipesClick,
                onScannerClick = onScannerClick,
                onPersonalClick = onPersonalClick
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(color = Color.White)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Inventario",
                    style = MaterialTheme.typography.headlineMedium,
                    color = ChefCoreColors.TextDark
                )

                // Botón añadir
                Button(
                    onClick = { mostrarDialogoAñadir = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ChefCoreColors.PrimaryGreen,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Añadir")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Añadir Ingrediente")
                }

                // Búsqueda
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Buscar ingrediente...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (ingredientesFiltrados.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No hay ingredientes",
                            style = MaterialTheme.typography.bodyLarge,
                            color = ChefCoreColors.TextMedium
                        )
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ingredientesFiltrados) { ingrediente ->
                            IngredienteItemConAcciones(
                                ingrediente = ingrediente,
                                onEdit = { ingredienteAEditar = ingrediente },
                                onDelete = { ingredienteAEliminar = ingrediente }
                            )
                        }
                    }
                }
            }
        }
    }

    if (mostrarDialogoAñadir) {
        AñadirStockDialog(
            onDismiss = { mostrarDialogoAñadir = false },
            onConfirm = { nombre, cantidad, unidad, precio ->
                viewModel.añadirStock(nombre, cantidad, unidad, precio)
                mostrarDialogoAñadir = false
            }
        )
    }

    ingredienteAEditar?.let { ing ->
        EditarIngredienteDialog(
            ingrediente = ing,
            onDismiss = { ingredienteAEditar = null },
            onConfirm = { nombre, cantidad, precio, unidad ->
                viewModel.actualizarIngrediente(ing, nombre, cantidad, precio, unidad)
                ingredienteAEditar = null
            }
        )
    }

    ingredienteAEliminar?.let { ing ->
        ConfirmDeleteDialog(
            title = "Eliminar Ingrediente",
            itemName = ing.nombre,
            onConfirm = {
                viewModel.eliminarIngrediente(ing)
                ingredienteAEliminar = null
            },
            onDismiss = { ingredienteAEliminar = null }
        )
    }
}

/**
 * Diálogo para añadir stock
 */
@Composable
private fun AñadirStockDialog(
    onDismiss: () -> Unit,
    onConfirm: (nombre: String, cantidad: Double, unidad: String, precio: Double) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var cantidad by remember { mutableStateOf("") }
    var unidad by remember { mutableStateOf("kg") }
    var precio by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir Stock") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre del ingrediente") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = cantidad,
                        onValueChange = { cantidad = it },
                        label = { Text("Cantidad") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = unidad,
                        onValueChange = { unidad = it },
                        label = { Text("Unidad") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                OutlinedTextField(
                    value = precio,
                    onValueChange = { precio = it },
                    label = { Text("Precio total (€)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nombre.isNotBlank()) {
                        val cant = cantidad.replace(",", ".").toDoubleOrNull() ?: 0.0
                        val prec = precio.replace(",", ".").toDoubleOrNull() ?: 0.0
                        onConfirm(nombre, cant, unidad, prec)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ChefCoreColors.PrimaryGreen)
            ) {
                Text("Añadir")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}