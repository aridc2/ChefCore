package es.chefcore.app.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import es.chefcore.app.ui.components.RecetaItem
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
    onNavigateToCreate: () -> Unit
    // onEditarReceta: Temporalmente desactivado - Fase 2
) {
    val recetasFiltradas by viewModel.recetasFiltradas.collectAsState()
    val rentabilidades by viewModel.rentabilidades.collectAsState()
    val recetaSeleccionada by viewModel.recetaSeleccionada.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    var isListening by remember { mutableStateOf(false) }
    var recognizedText by remember { mutableStateOf("") }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isListening = true
            // TODO: Iniciar reconocimiento de voz real con VoiceCommander
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(color = ChefCoreColors.BackgroundLight)
    ) {
        // Sidebar
        Sidebar(
            currentScreen = "Recipes",
            onSettingsClick = onSettingsClick,
            onInventoryClick = onInventoryClick,
            onRecipesClick = { },
            onScannerClick = onScannerClick,
            onPersonalClick = onPersonalClick
        )

        // Panel izquierdo: Búsqueda + lista + botones
        Column(
            modifier = Modifier
                .then(if (recetaSeleccionada == null) Modifier.weight(1f) else Modifier.width(350.dp))
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // BOTÓN DE VOZ
                FilledTonalIconButton(
                    onClick = {
                        if (isListening) {
                            isListening = false
                            // TODO: Procesar recognizedText con VoiceCommander
                            if (recognizedText.isNotEmpty()) {
                                // viewModel.ejecutarComandoVoz(recognizedText)
                            }
                        } else {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = if (isListening)
                            ChefCoreColors.ErrorRed
                        else
                            ChefCoreColors.AccentYellow
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
                    onClick = onNavigateToCreate, // ✅ CONECTADO
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ChefCoreColors.PrimaryGreen,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Crear",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Nueva Receta", style = MaterialTheme.typography.labelLarge)
                }
            }

            if (isListening) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = ChefCoreColors.AccentYellow.copy(alpha = 0.1f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = ChefCoreColors.AccentYellow,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (recognizedText.isEmpty()) "Escuchando..." else recognizedText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ChefCoreColors.TextDark
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
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = ChefCoreColors.PrimaryGreen,
                    unfocusedBorderColor = ChefCoreColors.SurfaceGray,
                    focusedTextColor = ChefCoreColors.TextDark,
                    unfocusedTextColor = ChefCoreColors.TextDark
                )
            )

            // Lista de recetas
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
                                .padding(4.dp)
                        ) {
                            RecetaItem(
                                receta = receta,
                                onClick = { viewModel.seleccionarReceta(receta.id) }
                            )
                        }
                    }
                }
            }
        }

        // Panel derecho: Detalle CON ESCANDALLO COMPLETO
        if (recetaSeleccionada != null) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                RecetaDetailScreen(
                    recetaId = recetaSeleccionada!!,
                    onVolver = { viewModel.seleccionarReceta(null) }
                    // onEditarReceta: Temporalmente desactivado - Fase 2
                )
            }
        }
    }
}