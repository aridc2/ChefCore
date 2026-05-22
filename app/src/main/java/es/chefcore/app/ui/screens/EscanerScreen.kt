package es.chefcore.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.lifecycle.viewmodel.compose.viewModel
import es.chefcore.app.ui.components.Sidebar
import es.chefcore.app.ui.theme.ChefCoreColors
import es.chefcore.app.viewmodel.EstadoOcr
import es.chefcore.app.viewmodel.OcrViewModel

@Composable
fun EscanerScreen(
    onSettingsClick: () -> Unit,
    onInventoryClick: () -> Unit,
    onRecipesClick: () -> Unit,
    onPersonalClick: () -> Unit,
    onAlbaranesClick: () -> Unit = {},
    onResultadoListo: () -> Unit = {},
    esGerente: Boolean = true,
    ocrViewModel: OcrViewModel = viewModel()
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val estado by ocrViewModel.estado.collectAsState()

    val cameraController = remember { LifecycleCameraController(context) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasCameraPermission = it }

    // Galería: elige foto ya tomada
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        try {
            val bitmap: Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
            ocrViewModel.analizarPagina(bitmap)
        } catch (e: Exception) { /* bitmap inválido — ignorar */ }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    fun capturarFoto() {
        cameraController.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap: Bitmap = image.toBitmap()
                    image.close()
                    ocrViewModel.analizarPagina(bitmap)
                }
                override fun onError(e: ImageCaptureException) { /* error silencioso */ }
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(ChefCoreColors.BackgroundLight)
    ) {
        Sidebar(
            currentScreen    = "Scanner",
            onSettingsClick  = onSettingsClick,
            onInventoryClick = onInventoryClick,
            onRecipesClick   = onRecipesClick,
            onScannerClick   = { },
            onPersonalClick  = onPersonalClick,
            esGerente        = esGerente
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(32.dp)
        ) {
            Text(
                "Escanear albarán",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = ChefCoreColors.TextDark
            )
            Text(
                "Escanea cada folio por separado. El total puedes introducirlo a mano.",
                color = ChefCoreColors.TextMedium,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(16.dp))

            // Estado de páginas escaneadas
            if (estado is EstadoOcr.PaginaLista) {
                val p = estado as EstadoOcr.PaginaLista
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = ChefCoreColors.PrimaryGreen.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, null,
                                tint = ChefCoreColors.PrimaryGreen, modifier = Modifier.size(22.dp))
                            Column {
                                Text(
                                    "${p.paginas} folio(s) escaneado(s)",
                                    fontWeight = FontWeight.Bold,
                                    color = ChefCoreColors.PrimaryGreen
                                )
                                Text(
                                    "${p.resultado.items.size} ingrediente(s) detectado(s)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ChefCoreColors.TextMedium
                                )
                            }
                        }
                        // Botón de limpiar y empezar de nuevo
                        TextButton(onClick = { ocrViewModel.resetear() }) {
                            Text("Limpiar", color = ChefCoreColors.ErrorRed)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Visor de cámara
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (hasCameraPermission) {
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Se requiere permiso de cámara", color = Color.White)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                            Text("Conceder permiso")
                        }
                    }
                }

                // Overlay de carga
                if (estado is EstadoOcr.Procesando) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.65f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = ChefCoreColors.PrimaryGreen)
                            Spacer(Modifier.height(12.dp))
                            Text("Analizando...", color = Color.White, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // Error
            if (estado is EstadoOcr.Error) {
                Spacer(Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = ChefCoreColors.ErrorRed.copy(alpha = 0.08f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        (estado as EstadoOcr.Error).mensaje,
                        modifier = Modifier.padding(12.dp),
                        color = ChefCoreColors.ErrorRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            val procesando = estado is EstadoOcr.Procesando

            // Fila de botones principales
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Escanear folio con cámara
                Button(
                    onClick = { ::capturarFoto.invoke() },
                    modifier = Modifier.weight(1f).height(60.dp),
                    enabled = hasCameraPermission && !procesando,
                    colors = ButtonDefaults.buttonColors(containerColor = ChefCoreColors.PrimaryGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (estado is EstadoOcr.PaginaLista) "Escanear folio siguiente"
                        else "Escanear folio",
                        fontWeight = FontWeight.Bold
                    )
                }

                // Elegir de galería
                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.height(60.dp),
                    enabled = !procesando,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ChefCoreColors.PrimaryGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Galería")
                }
            }

            Spacer(Modifier.height(10.dp))

            // Ver resultado (solo si hay páginas escaneadas)
            if (estado is EstadoOcr.PaginaLista) {
                Button(
                    onClick = onResultadoListo,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ChefCoreColors.AccentYellow,
                        contentColor   = ChefCoreColors.TextDark
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.FactCheck, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Revisar y guardar resultado", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(6.dp))
            }

            // Ver historial
            OutlinedButton(
                onClick = onAlbaranesClick,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ChefCoreColors.PrimaryGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Receipt, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Ver historial de albaranes")
            }
        }
    }
}