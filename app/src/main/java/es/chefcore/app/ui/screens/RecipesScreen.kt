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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import es.chefcore.app.data.database.Receta
import es.chefcore.app.ui.components.ConfirmDeleteDialog
import es.chefcore.app.ui.components.RecetaItemConAcciones
import es.chefcore.app.ui.components.Sidebar
import es.chefcore.app.ui.theme.ChefCoreColors
import es.chefcore.app.viewmodel.RecipesViewModel
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import es.chefcore.app.viewmodel.VoiceViewModel

@Composable
fun RecipesScreen(
    viewModel: RecipesViewModel = viewModel(),
    voiceViewModel: VoiceViewModel = viewModel(),
    esGerente: Boolean = true,
    onSettingsClick: () -> Unit,
    onInventoryClick: () -> Unit,
    onPersonalClick: () -> Unit,
    onScannerClick: () -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToCreateVoz: (String) -> Unit = {},
    onNavigateToEdit: (Int) -> Unit
) {
    val recetasFiltradas by viewModel.recetasFiltradas.collectAsState()
    val recetaSeleccionada by viewModel.recetaSeleccionada.collectAsState()
    val feedbackMessage by viewModel.feedbackMessage.collectAsState()
    val rentabilidades by viewModel.rentabilidades.collectAsState()

    val focusManager = LocalFocusManager.current
    var searchQuery by remember { mutableStateOf("") }
    var recetaAEliminar by remember { mutableStateOf<Receta?>(null) }

    var isListening by remember { mutableStateOf(false) }
    var recognizedText by remember { mutableStateOf("") }

    val context = androidx.compose.ui.platform.LocalContext.current
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val speechIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            putExtra("android.speech.extra.PREFER_OFFLINE", true)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
    }
    DisposableEffect(Unit) {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(p: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(v: Float) {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(r: Bundle?) {
                recognizedText = r?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
            }
            override fun onResults(r: Bundle?) {
                val texto = r?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                if (texto.isNotBlank()) { recognizedText = texto; voiceViewModel.procesarVoz(texto) }
                isListening = false
            }
            override fun onError(err: Int) { isListening = false; recognizedText = "" }
            override fun onEvent(t: Int, p: Bundle?) {}
        })
        onDispose { speechRecognizer.destroy() }
    }
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            recognizedText = ""
            isListening = true
            speechRecognizer.startListening(speechIntent)
        }
    }

    val navSenal by voiceViewModel.navSenal.collectAsStateWithLifecycle()
    LaunchedEffect(navSenal) {
        navSenal?.let { senal ->
            when {
                senal.startsWith("CREAR|")   -> onNavigateToCreateVoz(senal.substringAfter("CREAR|"))
                senal.startsWith("DETALLE|") -> viewModel.seleccionarRecetaPorNombre(senal.substringAfter("DETALLE|"))
                senal.startsWith("EDITAR|")  -> onNavigateToEdit(senal.substringAfter("EDITAR|").toIntOrNull() ?: 0)
            }
            voiceViewModel.clearNavSenal()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val voceFeedback    by voiceViewModel.feedback.collectAsStateWithLifecycle()
    val recetaActiva    by voiceViewModel.recetaActiva.collectAsStateWithLifecycle()
    LaunchedEffect(voceFeedback) {
        voceFeedback?.let {
            if (it.startsWith("Receta '") && it.endsWith("' cerrada")) {
                viewModel.seleccionarReceta(null)
            }
            snackbarHostState.showSnackbar(it)
            voiceViewModel.clearFeedback()
        }
    }

    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedback()
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
                onPersonalClick = onPersonalClick,
                esGerente = esGerente
            )

            Column(
                modifier = Modifier
                    .then(if (recetaSeleccionada == null) Modifier.weight(1f) else Modifier.width(400.dp))
                    .fillMaxHeight()
                    .background(color = Color.White)
                    .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalIconButton(
                        onClick = {
                            if (isListening) {
                                speechRecognizer.cancel()
                                isListening = false
                            } else {
                                val yaPermitido = androidx.core.content.ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.RECORD_AUDIO
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                if (yaPermitido) {
                                    recognizedText = ""
                                    isListening = true
                                    speechRecognizer.startListening(speechIntent)
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
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
                        onClick = onNavigateToCreate,
                        modifier = Modifier.weight(1f).height(72.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ChefCoreColors.PrimaryGreen,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Crear", modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Nueva Receta", style = MaterialTheme.typography.titleLarge)
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

                // Banner de receta activa en contexto de voz
                if (recetaActiva.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = ChefCoreColors.PrimaryGreen.copy(alpha = 0.1f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ChefCoreColors.PrimaryGreen.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = ChefCoreColors.PrimaryGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Receta activa: $recetaActiva",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ChefCoreColors.PrimaryGreen
                                )
                            }
                            Text(
                                text = "Di 'cerrar receta' para salir",
                                style = MaterialTheme.typography.labelSmall,
                                color = ChefCoreColors.TextMedium
                            )
                        }
                    }
                }

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
                                    esGerente = esGerente,
                                    esRentable = rentabilidades[receta.id]?.esRentable ?: true,
                                    onClick = { viewModel.seleccionarReceta(receta.id) },
                                    onEdit = { onNavigateToEdit(receta.id) },
                                    onDelete = { recetaAEliminar = receta }
                                )
                            }
                        }
                    }
                }
            }

            if (recetaSeleccionada != null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    RecetaDetailScreen(
                        recetaId = recetaSeleccionada!!,
                        esGerente = esGerente,
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