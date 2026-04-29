package es.chefcore.app.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import es.chefcore.app.data.database.Receta
import es.chefcore.app.ui.components.ConfirmDeleteDialog
import es.chefcore.app.ui.components.RecetaItemConAcciones
import es.chefcore.app.ui.components.Sidebar
import es.chefcore.app.ui.theme.ChefCoreColors
import es.chefcore.app.viewmodel.RecipesViewModel

@Composable
fun RecipesScreen(
    viewModel: RecipesViewModel = viewModel(),
    onSettingsClick: () -> Unit,
    onInventoryClick: () -> Unit,
    onPersonalClick: () -> Unit,
    onScannerClick: () -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToEdit: (Int) -> Unit
) {
    val recetasFiltradas by viewModel.recetasFiltradas.collectAsState()
    val recetaSeleccionada by viewModel.recetaSeleccionada.collectAsState()
    val feedbackMessage by viewModel.feedbackMessage.collectAsState()

    // Estados locales
    var searchQuery by remember { mutableStateOf("") }
    var recetaAEliminar by remember { mutableStateOf<Receta?>(null) }

    // Estados de voz
    var isListening by remember { mutableStateOf(false) }
    var recognizedText by remember { mutableStateOf("") }

    // Snackbar para feedback
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedback()
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isListening = true
        }
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
                currentScreen = "Recipes",
                onSettingsClick = onSettingsClick,
                onInventoryClick = onInventoryClick,
                onRecipesClick = { },
                onScannerClick = onScannerClick,
                onPersonalClick = onPersonalClick
            )

            // Panel izquierdo
            Column(
                modifier = Modifier
                    .then(if (recetaSeleccionada == null) Modifier.weight(1f) else Modifier.width(400.dp))
                    .fillMaxHeight()
                    .background(color = Color.White)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Mis Recetas",
                    style = MaterialTheme.typography.headlineMedium,
                    color = ChefCoreColors.TextDark
                )

                // Botones: Voz + Crear
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (isListening) ChefCoreColors.ErrorRed
                            else ChefCoreColors.AccentYellow
                        )
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (isListening) "Detener" else "Comandos de voz",
                            tint = if (isListening) Color.White else ChefCoreColors.TextDark,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Button(
                        onClick = onNavigateToCreate,
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ChefCoreColors.PrimaryGreen,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Crear")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Nueva Receta")
                    }
                }

                // Card de voz
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
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = null,
                                tint = ChefCoreColors.AccentYellow
                            )
                            Text(
                                text = if (recognizedText.isEmpty()) "Escuchando..." else recognizedText,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Búsqueda
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        viewModel.actualizarBusqueda(it)
                    },
                    label = { Text("Buscar plato...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (recetasFiltradas.isEmpty()) {
                    Text(
                        "No hay recetas.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ChefCoreColors.TextMedium
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(recetasFiltradas) { receta ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = if (receta.id == recetaSeleccionada)
                                            ChefCoreColors.PrimaryGreenLight.copy(alpha = 0.3f)
                                        else
                                            Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                            ) {
                                RecetaItemConAcciones(
                                    receta = receta,
                                    onClick = { viewModel.seleccionarReceta(receta.id) },
                                    onEdit = { onNavigateToEdit(receta.id) }, // ✅ Editar
                                    onDelete = { recetaAEliminar = receta }   // ✅ Eliminar
                                )
                            }
                        }
                    }
                }
            }

            // Panel derecho: Detalle
            if (recetaSeleccionada != null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    RecetaDetailScreen(
                        recetaId = recetaSeleccionada!!,
                        onVolver = { viewModel.seleccionarReceta(null) }
                    )
                }
            }
        }
    }

    recetaAEliminar?.let { receta ->
        ConfirmDeleteDialog(
            title = "Eliminar Receta",
            itemName = receta.nombre,
            onConfirm = {
                viewModel.eliminarReceta(receta)
                recetaAEliminar = null
            },
            onDismiss = { recetaAEliminar = null }
        )
    }
}