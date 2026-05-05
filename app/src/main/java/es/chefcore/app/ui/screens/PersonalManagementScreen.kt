package es.chefcore.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import es.chefcore.app.data.database.Usuario
import es.chefcore.app.ui.components.ConfirmDeleteDialog
import es.chefcore.app.ui.components.EmployeeCard
import es.chefcore.app.ui.components.Sidebar
import es.chefcore.app.ui.theme.ChefCoreColors
import es.chefcore.app.viewmodel.PersonalManagementViewModel

@Composable
fun PersonalManagementScreen(
    viewModel: PersonalManagementViewModel = viewModel(),
    onSettingsClick: () -> Unit,
    onInventoryClick: () -> Unit,
    onRecipesClick: () -> Unit,
    onScannerClick: () -> Unit
) {
    val usuarios by viewModel.usuarios.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val feedbackMessage by viewModel.feedbackMessage.collectAsState()

    // Control de ventanas emergentes (diálogos)
    var mostrarDialogoCrear by remember { mutableStateOf(false) }
    var empleadoAEditar by remember { mutableStateOf<Usuario?>(null) }
    var empleadoAEliminar by remember { mutableStateOf<Usuario?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
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
            // Sidebar
            Sidebar(
                currentScreen = "Personal",
                onSettingsClick = onSettingsClick,
                onInventoryClick = onInventoryClick,
                onRecipesClick = onRecipesClick,
                onScannerClick = onScannerClick,
                onPersonalClick = { },
                esGerente = true
            )

            // Contenido principal
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Gestión de Personal",
                            style = MaterialTheme.typography.displaySmall,
                            color = ChefCoreColors.TextDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${usuarios.size} empleados registrados",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ChefCoreColors.TextMedium
                        )
                    }
                    Button(
                        onClick = { mostrarDialogoCrear = true },
                        modifier = Modifier.height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ChefCoreColors.PrimaryGreen,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Añadir", modifier = Modifier.width(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Añadir Empleado")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (usuarios.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No hay empleados. Pulsa 'Añadir Empleado' para empezar.",
                            style = MaterialTheme.typography.bodyLarge, color = ChefCoreColors.TextMedium)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(usuarios) { usuario ->
                            EmployeeCard(
                                name = usuario.nombre,
                                role = usuario.rol,
                                onEdit = { empleadoAEditar = usuario },
                                onDelete = { empleadoAEliminar = usuario }
                            )
                        }
                    }
                }
            }
        }
    }

    // --- DIÁLOGO PARA AÑADIR EMPLEADO ---
    if (mostrarDialogoCrear) {
        DialogoEmpleado(
            titulo = "Nuevo Empleado",
            onDismiss = { mostrarDialogoCrear = false },
            onConfirm = { nombre, rol, pin ->
                viewModel.crearEmpleado(nombre, rol, pin)
                mostrarDialogoCrear = false
            }
        )
    }

    // --- DIÁLOGO PARA EDITAR EMPLEADO ---
    empleadoAEditar?.let { empleado ->
        DialogoEmpleado(
            titulo = "Editar a ${empleado.nombre}",
            nombreInicial = empleado.nombre,
            rolInicial = empleado.rol,
            pinInicial = empleado.pin,
            onDismiss = { empleadoAEditar = null },
            onConfirm = { nombre, rol, pin ->
                viewModel.actualizarEmpleado(empleado, nombre, rol, pin)
                empleadoAEditar = null
            }
        )
    }

    // --- DIÁLOGO PARA ELIMINAR EMPLEADO ---
    empleadoAEliminar?.let { empleado ->
        ConfirmDeleteDialog(
            title = "Eliminar Empleado",
            itemName = empleado.nombre,
            onConfirm = {
                viewModel.eliminarEmpleado(empleado)
                empleadoAEliminar = null
            },
            onDismiss = { empleadoAEliminar = null }
        )
    }

    // Mostrar error si existe
    errorMessage?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.clearMessages() },
            title = { Text("Error") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearMessages() }) {
                    Text("OK")
                }
            }
        )
    }
}

/**
 * Diálogo reutilizable para Crear y Editar Empleados
 */
@Composable
fun DialogoEmpleado(
    titulo: String,
    nombreInicial: String = "",
    rolInicial: String = "",
    pinInicial: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var nombre by remember { mutableStateOf(nombreInicial) }
    var rol by remember { mutableStateOf(rolInicial) }
    var pin by remember { mutableStateOf(pinInicial) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titulo) },
        text = {
            Column {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre completo") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = rol,
                    onValueChange = { rol = it },
                    label = { Text("Rol (Ej: Chef, Camarero)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) pin = it },
                    label = { Text("PIN de acceso (4 dígitos)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(nombre, rol, pin) },
                colors = ButtonDefaults.buttonColors(containerColor = ChefCoreColors.PrimaryGreen),
                enabled = nombre.isNotBlank() && rol.isNotBlank() && pin.length == 4
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}