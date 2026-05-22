package es.chefcore.app.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import es.chefcore.app.ui.components.Sidebar
import es.chefcore.app.ui.theme.ChefCoreColors
import es.chefcore.app.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    esGerente: Boolean = true,
    onInventoryClick: () -> Unit,
    onPersonalClick: () -> Unit,
    onRecipesClick: () -> Unit,
    onScannerClick: () -> Unit,
    onLogout: () -> Unit = {},
    onCambiarPin: (pinActual: String, pinNuevo: String) -> Unit = { _, _ -> },
    onCheckCurrentPin: (String) -> Boolean = { false }
) {
    val currency by viewModel.currency.collectAsState()
    val ivaPercentage by viewModel.ivaPercentage.collectAsState()
    val cameraPermissionGranted by viewModel.cameraPermissionGranted.collectAsState()

    var showCurrencyDropdown by remember { mutableStateOf(false) }
    var mostrarDialogoPin  by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.updateCameraPermissionStatus(isGranted)
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(color = ChefCoreColors.BackgroundLight)
    ) {
        Sidebar(
            currentScreen = "Settings",
            onSettingsClick = { },
            onInventoryClick = onInventoryClick,
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
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Configuración",
                style = MaterialTheme.typography.displaySmall,
                color = ChefCoreColors.TextDark
            )

            if (esGerente) {
                SettingsSection(
                    title = "Negocio",
                    icon = Icons.Default.Store,
                    content = {
                        // Opción 1: Moneda
                        SettingRow(
                            label = "Moneda",
                            icon = Icons.Default.AttachMoney
                        ) {
                            Box {
                                Button(
                                    onClick = { showCurrencyDropdown = !showCurrencyDropdown },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ChefCoreColors.SurfaceGray,
                                        contentColor = ChefCoreColors.TextDark
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .height(40.dp)
                                        .width(100.dp)
                                ) {
                                    Text(currency, style = MaterialTheme.typography.labelLarge)
                                }
                                DropdownMenu(
                                    expanded = showCurrencyDropdown,
                                    onDismissRequest = { showCurrencyDropdown = false }
                                ) {
                                    listOf("EUR €", "USD $", "GBP £", "JPY ¥").forEach { curr ->
                                        DropdownMenuItem(
                                            text = { Text(curr) },
                                            onClick = {
                                                viewModel.setCurrency(curr.split(" ")[0])
                                                showCurrencyDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Opción 2: IVA
                        SettingRow(
                            label = "IVA: ${ivaPercentage.toInt()}%",
                            icon = Icons.Default.Percent
                        ) {
                            Slider(
                                value = ivaPercentage,
                                onValueChange = { viewModel.setIva(it) },
                                valueRange = 0f..50f,
                                steps = 24,
                                modifier = Modifier.width(150.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = ChefCoreColors.PrimaryGreen,
                                    activeTrackColor = ChefCoreColors.PrimaryGreen
                                )
                            )
                        }
                    }
                )
            }

            SettingsSection(
                title = "Permisos",
                icon = Icons.Default.Security,
                content = {
                    SettingRow(
                        label = "Cámara (Escaneo de albaranes)",
                        icon = Icons.Default.CameraAlt
                    ) {
                        if (cameraPermissionGranted) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Concedido",
                                    tint = ChefCoreColors.PrimaryGreen
                                )
                                Text(
                                    "Concedido",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ChefCoreColors.PrimaryGreen
                                )
                            }
                        } else {
                            Button(
                                onClick = {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ChefCoreColors.AccentYellow,
                                    contentColor = ChefCoreColors.TextDark
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(40.dp)
                            ) {
                                Text("Solicitar permiso")
                            }
                        }
                    }

                    if (!cameraPermissionGranted) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    ChefCoreColors.AccentYellow.copy(alpha = 0.1f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = ChefCoreColors.AccentYellow
                                )
                                Text(
                                    "La cámara permite escanear albaranes de proveedores para añadir stock automáticamente.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ChefCoreColors.TextDark
                                )
                            }
                        }
                    }
                }
            )

            SettingsSection(
                title = "Información",
                icon = Icons.Default.Info,
                content = {
                    SettingRow(
                        label = "Versión de la app",
                        icon = Icons.Default.AppSettingsAlt
                    ) {
                        Text(
                            viewModel.getAppVersion(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = ChefCoreColors.TextMedium
                        )
                    }
                }
            )

            // Sincronización manual con Firestore — solo Gerente
            if (esGerente) {
                SettingsSection(
                    title = "Nube",
                    icon = Icons.Default.Cloud,
                    content = {
                        val context = androidx.compose.ui.platform.LocalContext.current
                        var sincronizando by remember { mutableStateOf(false) }
                        var restaurando by remember { mutableStateOf(false) }

                        SettingRow(
                            label = "Copia de seguridad en la nube",
                            icon = Icons.Default.CloudUpload
                        ) {}

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                sincronizando = true
                                es.chefcore.app.workers.SyncManager.syncNow(context)
                                android.os.Handler(android.os.Looper.getMainLooper())
                                    .postDelayed({ sincronizando = false }, 2000)
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ChefCoreColors.PrimaryGreen,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (sincronizando) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sincronizando...")
                            } else {
                                Icon(
                                    Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Subir a la nube", style = MaterialTheme.typography.labelLarge)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                restaurando = true
                                es.chefcore.app.workers.SyncManager.restoreNow(context)
                                android.os.Handler(android.os.Looper.getMainLooper())
                                    .postDelayed({ restaurando = false }, 2000)
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = ChefCoreColors.PrimaryGreen
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (restaurando) {
                                CircularProgressIndicator(
                                    color = ChefCoreColors.PrimaryGreen,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Restaurando...")
                            } else {
                                Icon(
                                    Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Restaurar desde la nube", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                )
            }

            // Cambiar PIN (todos los usuarios)
            OutlinedButton(
                onClick = { mostrarDialogoPin = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ChefCoreColors.PrimaryGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Cambiar PIN",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cambiar mi PIN", style = MaterialTheme.typography.labelLarge)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ChefCoreColors.TextMedium,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = "Cerrar sesión",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cerrar sesión", style = MaterialTheme.typography.labelLarge)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Diálogo cambio de PIN
    if (mostrarDialogoPin) {
        CambiarPinDialog(
            onCheckCurrentPin = onCheckCurrentPin,
            onDismiss = { mostrarDialogoPin = false },
            onConfirm = { pinActual, pinNuevo ->
                onCambiarPin(pinActual, pinNuevo)
                mostrarDialogoPin = false
            }
        )
    }
}

/**
 * Componente reutilizable para una sección de configuración
 */
@Composable
fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = ChefCoreColors.SurfaceGray,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.width(24.dp),
                tint = ChefCoreColors.PrimaryGreen
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = ChefCoreColors.TextDark
            )
        }
        content()
    }
}

/**
 * Componente reutilizable para una fila de configuración
 */
@Composable
fun SettingRow(
    label: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color.White,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.width(20.dp),
                tint = ChefCoreColors.TextMedium
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = ChefCoreColors.TextDark
            )
        }
        content()
    }
}

@Composable
private fun CambiarPinDialog(
    onCheckCurrentPin: (String) -> Boolean,
    onDismiss: () -> Unit,
    onConfirm: (pinActual: String, pinNuevo: String) -> Unit
) {
    var pinActual  by remember { mutableStateOf("") }
    var pinNuevo   by remember { mutableStateOf("") }
    var pinConfirm by remember { mutableStateOf("") }
    var error      by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cambiar PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = pinActual,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) { pinActual = it; error = null } },
                    label = { Text("PIN actual") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                    )
                )
                OutlinedTextField(
                    value = pinNuevo,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) { pinNuevo = it; error = null } },
                    label = { Text("Nuevo PIN") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                    )
                )
                OutlinedTextField(
                    value = pinConfirm,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) { pinConfirm = it; error = null } },
                    label = { Text("Confirmar nuevo PIN") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                    )
                )
                // Error inline — siempre visible dentro del diálogo
                if (error != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                ChefCoreColors.ErrorRed.copy(alpha = 0.08f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(10.dp)
                    ) {
                        Icon(Icons.Default.Error, null,
                            tint = ChefCoreColors.ErrorRed, modifier = Modifier.size(16.dp))
                        Text(error!!, color = ChefCoreColors.ErrorRed,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    error = when {
                        pinActual.isEmpty()          -> "Introduce tu PIN actual"
                        !onCheckCurrentPin(pinActual)-> "El PIN actual no es correcto"
                        pinNuevo.length != 4         -> "El nuevo PIN debe tener 4 dígitos"
                        pinNuevo != pinConfirm        -> "Los PINs nuevos no coinciden"
                        pinNuevo == pinActual         -> "El nuevo PIN debe ser diferente"
                        else -> null
                    }
                    if (error == null) onConfirm(pinActual, pinNuevo)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ChefCoreColors.PrimaryGreen)
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}