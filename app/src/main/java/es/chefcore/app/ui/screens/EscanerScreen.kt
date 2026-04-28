package es.chefcore.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import es.chefcore.app.ui.components.Sidebar
import es.chefcore.app.ui.theme.ChefCoreColors

@Composable
fun EscanerScreen(
    onSettingsClick: () -> Unit,
    onInventoryClick: () -> Unit,
    onRecipesClick: () -> Unit,
    onPersonalClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Controlador de la cámara (CameraX)
    val cameraController = remember { LifecycleCameraController(context) }

    // Estado para saber si tenemos permiso
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Ventana emergente para pedir permiso
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    // Nada más entrar a la pantalla, pedimos permiso si no lo tenemos
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Row(modifier = Modifier.fillMaxSize().background(ChefCoreColors.BackgroundLight)) {

        // Sidebar para mantener la navegación unificada
        Sidebar(
            currentScreen = "Scanner",
            onSettingsClick = onSettingsClick,
            onInventoryClick = onInventoryClick,
            onRecipesClick = onRecipesClick,
            onScannerClick = { }, // Ya estamos aquí
            onPersonalClick = onPersonalClick
        )

        // Contenido del Escáner
        Column(modifier = Modifier.padding(32.dp).weight(1f)) {
            Text("Escanear Albarán", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = ChefCoreColors.TextDark)
            Text("Enfoca el ticket o factura para extraer los ingredientes mediante IA", color = ChefCoreColors.TextMedium)

            Spacer(modifier = Modifier.height(24.dp))

            // Visor de Cámara
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (hasCameraPermission) {
                    // Si hay permiso, conectamos la cámara de Android con Compose
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).apply {
                                this.controller = cameraController
                                cameraController.bindToLifecycle(lifecycleOwner)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text("Se requiere permiso de cámara para escanear", color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    /* TODO: Aquí capturaremos la imagen y se la pasaremos al ML Kit */
                },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                enabled = hasCameraPermission,
                colors = ButtonDefaults.buttonColors(containerColor = ChefCoreColors.PrimaryGreen)
            ) {
                Text("ANALIZAR ALBARÁN", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}