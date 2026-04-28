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
    onInventoryClick: () -> Unit,
    onPersonalClick: () -> Unit,
    onRecipesClick: () -> Unit,
    onScannerClick: () -> Unit
) {
    // Observar estados del ViewModel
    val currency by viewModel.currency.collectAsState()
    val ivaPercentage by viewModel.ivaPercentage.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val voiceEnabled by viewModel.voiceEnabled.collectAsState()
    val voiceLanguage by viewModel.voiceLanguage.collectAsState()
    val cameraPermissionGranted by viewModel.cameraPermissionGranted.collectAsState()
    val shouldShowRationale by viewModel.shouldShowRationale.collectAsState()
    val showDeleteConfirmation by viewModel.showDeleteConfirmation.collectAsState()

    // Estados locales para UI
    var showCurrencyDropdown by remember { mutableStateOf(false) }
    var showLanguageDropdown by remember { mutableStateOf(false) }

    // Launcher para permisos de cámara
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.updateCameraPermissionStatus(isGranted)
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(color = ChefCoreColors.BackgroundLight)
    ) {
        // Sidebar
        Sidebar(
            currentScreen = "Settings",
            onSettingsClick = { },
            onInventoryClick = onInventoryClick,
            onRecipesClick = onRecipesClick,
            onScannerClick = onScannerClick,
            onPersonalClick = onPersonalClick
        )

        // Contenido principal
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(color = Color.White)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Título
            Text(
                text = "Configuración",
                style = MaterialTheme.typography.displaySmall,
                color = ChefCoreColors.TextDark
            )

            // ==================== SECCIÓN 1: NEGOCIO ====================
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

            // ==================== SECCIÓN 2: INTERFAZ ====================
            SettingsSection(
                title = "Interfaz",
                icon = Icons.Default.Palette,
                content = {
                    // Modo oscuro
                    SettingRow(
                        label = "Modo oscuro",
                        icon = Icons.Default.DarkMode
                    ) {
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { viewModel.setDarkMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ChefCoreColors.PrimaryGreen,
                                checkedTrackColor = ChefCoreColors.PrimaryGreen.copy(alpha = 0.5f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Tamaño de fuente
                    SettingRow(
                        label = "Tamaño texto: ${(fontSize * 100).toInt()}%",
                        icon = Icons.Default.FormatSize
                    ) {
                        Slider(
                            value = fontSize,
                            onValueChange = { viewModel.setFontSize(it) },
                            valueRange = 0.8f..1.5f,
                            steps = 6,
                            modifier = Modifier.width(150.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = ChefCoreColors.AccentYellow,
                                activeTrackColor = ChefCoreColors.AccentYellow
                            )
                        )
                    }
                }
            )

            // ==================== SECCIÓN 3: COMANDOS DE VOZ ====================
            SettingsSection(
                title = "Comandos de Voz",
                icon = Icons.Default.Mic,
                content = {
                    // Activar comandos de voz
                    SettingRow(
                        label = "Comandos de voz",
                        icon = Icons.Default.RecordVoiceOver
                    ) {
                        Switch(
                            checked = voiceEnabled,
                            onCheckedChange = { viewModel.setVoiceEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ChefCoreColors.PrimaryGreen,
                                checkedTrackColor = ChefCoreColors.PrimaryGreen.copy(alpha = 0.5f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Idioma de voz
                    SettingRow(
                        label = "Idioma de voz",
                        icon = Icons.Default.Language
                    ) {
                        Box {
                            Button(
                                onClick = { showLanguageDropdown = !showLanguageDropdown },
                                enabled = voiceEnabled,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ChefCoreColors.SurfaceGray,
                                    contentColor = ChefCoreColors.TextDark
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .height(40.dp)
                                    .width(120.dp)
                            ) {
                                Text(voiceLanguage, style = MaterialTheme.typography.labelLarge)
                            }
                            DropdownMenu(
                                expanded = showLanguageDropdown,
                                onDismissRequest = { showLanguageDropdown = false }
                            ) {
                                listOf("Español", "English", "Français").forEach { lang ->
                                    DropdownMenuItem(
                                        text = { Text(lang) },
                                        onClick = {
                                            viewModel.setVoiceLanguage(lang)
                                            showLanguageDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            )

            // ==================== SECCIÓN 4: PERMISOS (CÁMARA) ====================
            SettingsSection(
                title = "Permisos",
                icon = Icons.Default.Security,
                content = {
                    // Permiso de cámara para escaneo de albaranes
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

                    // Explicación del permiso
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

            // ==================== SECCIÓN 5: DATOS ====================
            SettingsSection(
                title = "Datos",
                icon = Icons.Default.Delete,
                content = {
                    // Botón de Borrar Base de Datos
                    Button(
                        onClick = { viewModel.setShowDeleteConfirmation(true) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ChefCoreColors.ErrorRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Borrar base de datos",
                            modifier = Modifier.width(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Borrar Base de Datos", style = MaterialTheme.typography.labelLarge)
                    }

                    if (showDeleteConfirmation) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = ChefCoreColors.ErrorRed.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "⚠️ ¿Estás seguro? Esta acción no se puede deshacer.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ChefCoreColors.ErrorRed
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.setShowDeleteConfirmation(false) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ChefCoreColors.SurfaceGray,
                                            contentColor = ChefCoreColors.TextDark
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Cancelar")
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.deleteAllData {
                                                // Feedback al usuario
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ChefCoreColors.ErrorRed,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Borrar")
                                    }
                                }
                            }
                        }
                    }
                }
            )

            // ==================== SECCIÓN 6: INFORMACIÓN ====================
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

            Spacer(modifier = Modifier.height(24.dp))
        }
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
