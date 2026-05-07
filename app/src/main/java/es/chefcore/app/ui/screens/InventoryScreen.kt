package es.chefcore.app.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import es.chefcore.app.data.database.Ingrediente
import es.chefcore.app.logic.UnitConverter
import es.chefcore.app.ui.components.ConfirmDeleteDialog
import es.chefcore.app.ui.components.EditarIngredienteDialog
import es.chefcore.app.ui.components.IngredienteItemConAcciones
import es.chefcore.app.ui.components.Sidebar
import es.chefcore.app.ui.theme.ChefCoreColors
import es.chefcore.app.viewmodel.InventoryViewModel


@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel = viewModel(),
    esGerente: Boolean = true,
    onSettingsClick: () -> Unit,
    onRecipesClick: () -> Unit,
    onPersonalClick: () -> Unit,
    onScannerClick: () -> Unit
) {
    val ingredientes by viewModel.ingredientes.collectAsState()
    val feedbackMessage by viewModel.feedbackMessage.collectAsState()

    val focusManager = LocalFocusManager.current
    var searchQuery by remember { mutableStateOf("") }
    var mostrarDialogoAñadir by remember { mutableStateOf(false) }
    var ingredienteAEditar by remember { mutableStateOf<Ingrediente?>(null) }
    var ingredienteAEliminar by remember { mutableStateOf<Ingrediente?>(null) }

    var isListening by remember { mutableStateOf(false) }
    var recognizedText by remember { mutableStateOf("") }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) isListening = true
    }

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
                onPersonalClick = onPersonalClick,
                esGerente = esGerente
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(color = Color.White)
                    .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Inventario",
                    style = MaterialTheme.typography.headlineMedium,
                    color = ChefCoreColors.TextDark
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalIconButton(
                        onClick = {
                            if (isListening) {
                                isListening = false
                            } else {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        modifier = Modifier.size(72.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (isListening) ChefCoreColors.ErrorRed
                            else ChefCoreColors.AccentYellow
                        )
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (isListening) "Detener" else "Comandos de voz",
                            tint = if (isListening) Color.White else ChefCoreColors.TextDark,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Button(
                        onClick = { mostrarDialogoAñadir = true },
                        modifier = Modifier.weight(1f).height(72.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ChefCoreColors.PrimaryGreen,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Añadir", modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Añadir Ingrediente", style = MaterialTheme.typography.titleLarge)
                    }
                }

                if (isListening) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = ChefCoreColors.AccentYellow.copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = ChefCoreColors.AccentYellow)
                            Text(
                                text = if (recognizedText.isEmpty()) "Escuchando..." else recognizedText,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

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
                                esGerente = esGerente,
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
            esGerente = esGerente,
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
            onConfirm = { nombre, cantidad, precio, unidad, imagenUri ->
                viewModel.actualizarIngrediente(ing, nombre, cantidad, precio, unidad, imagenUri)
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AñadirStockDialog(
    esGerente: Boolean = true,
    onDismiss: () -> Unit,
    onConfirm: (nombre: String, cantidad: Double, unidad: String, precio: Double) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var cantidad by remember { mutableStateOf("") }
    var unidad by remember { mutableStateOf("kg") }
    var precio by remember { mutableStateOf("") }
    var expandedUnidad by remember { mutableStateOf(false) }

    val unidades = listOf("kg", "g", "L", "ml", "cl", "ud")

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
                    ExposedDropdownMenuBox(
                        expanded = expandedUnidad,
                        onExpandedChange = { expandedUnidad = !expandedUnidad },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = unidad,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Unidad") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedUnidad) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedUnidad,
                            onDismissRequest = { expandedUnidad = false }
                        ) {
                            unidades.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text(u) },
                                    onClick = { unidad = u; expandedUnidad = false }
                                )
                            }
                        }
                    }
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