package es.chefcore.app

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import es.chefcore.app.data.database.ChefCoreDatabase
import es.chefcore.app.logic.VoiceCommander
import es.chefcore.app.ui.screens.MainScreen
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChefCoreApp()
        }
    }
}

@Composable
fun ChefCoreApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            // Permiso denegado
        }
    }

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    val db = remember { ChefCoreDatabase.getDatabase(context) }
    val iDao = db.ingredienteDao()
    val rDao = db.recetaDao()
    val aDao = db.albaranDao()
    val uDao = db.usuarioDao()

    val cocinaManager = remember { es.chefcore.app.logic.CocinaManager(iDao, rDao) }

    val commander = remember { VoiceCommander(iDao, rDao, aDao, scope) }

    val ingredientes by iDao.obtenerTodos().collectAsState(initial = emptyList())
    // OJO: Si en tu DAO se llama obtenerTodos() déjalo como estaba, pero aquí uso obtenerTodas() que es lo ideal
    val recetas by rDao.obtenerTodas().collectAsState(initial = emptyList())
    val albaranes by aDao.obtenerTodos().collectAsState(initial = emptyList())

    var estadoVoz by remember { mutableStateOf("Pulsa el micro para hablar") }

    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val speechIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
        }
    }

    DisposableEffect(Unit) {
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(p0: Bundle?) { estadoVoz = "Escuchando..." }
            override fun onEndOfSpeech() { estadoVoz = "Procesando..." }
            override fun onError(p0: Int) {
                estadoVoz = if (p0 == 9) "Error: Falta permiso de micro" else "Error: $p0"
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val texto = matches[0]
                    commander.ejecutarComando(texto) { feedback ->
                        estadoVoz = feedback
                    }
                }
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(p0: Float) {}
            override fun onBufferReceived(p0: ByteArray?) {}
            override fun onPartialResults(p0: Bundle?) {}
            override fun onEvent(p0: Int, p1: Bundle?) {}
        }
        speechRecognizer.setRecognitionListener(listener)
        onDispose { speechRecognizer.destroy() }
    }

    MaterialTheme {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context, android.Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasPermission) {
                            speechRecognizer.startListening(speechIntent)
                        } else {
                            permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Filled.Mic, contentDescription = "Hablar")
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                es.chefcore.app.ui.navigation.ChefCoreAppNavigation(
                    iDao = iDao,
                    rDao = rDao,
                    aDao = aDao,
                    uDao = uDao,
                    cocinaManager = cocinaManager,
                    textoVoz = estadoVoz
                )
            }
        }
    }
}