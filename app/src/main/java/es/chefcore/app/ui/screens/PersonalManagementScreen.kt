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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import es.chefcore.app.data.database.Usuario
import es.chefcore.app.ui.components.ConfirmDeleteDialog
import es.chefcore.app.ui.components.EmployeeCard
import es.chefcore.app.ui.components.Sidebar
import es.chefcore.app.ui.theme.ChefCoreColors
import es.chefcore.app.viewmodel.PersonalManagementViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalManagementScreen(
    viewModel: PersonalManagementViewModel = viewModel(),
    onSettingsClick: () -> Unit,
    onInventoryClick: () -> Unit,
    onRecipesClick: () -> Unit,
    onScannerClick: () -> Unit
) {
    val usuarios      by viewModel.usuarios.collectAsStateWithLifecycle()
    val errorMessage  by viewModel.errorMessage.collectAsStateWithLifecycle()
    val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()

    var mostrarDialogoCrear  by remember { mutableStateOf(false) }
    var usuarioAEditar       by remember { mutableStateOf<Usuario?>(null) }
    var usuarioAEliminar     by remember { mutableStateOf<Usuario?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Mostrar feedback y errores como Snackbar
    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
    }
    LaunchedEffect(errorMessage) {
        errorMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(ChefCoreColors.BackgroundLight)
        ) {
            Sidebar(
                currentScreen = "Personal",
                onSettingsClick  = onSettingsClick,
                onInventoryClick = onInventoryClick,
                onRecipesClick   = onRecipesClick,
                onScannerClick   = onScannerClick,
                onPersonalClick  = { },
                esGerente = true
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Cabecera
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
                            contentColor   = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Añadir", modifier = Modifier.width(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Añadir Empleado")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Lista de empleados
                if (usuarios.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "No hay empleados. Pulsa 'Añadir Empleado' para empezar.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = ChefCoreColors.TextMedium
                        )
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(usuarios, key = { it.id }) { usuario ->
                            EmployeeCard(
                                name   = usuario.nombre,
                                role   = usuario.rol,
                                onEdit = { usuarioAEditar = usuario },
                                onDelete = { usuarioAEliminar = usuario }
                            )
                        }
                    }
                }
            }
        }
    }

    // Diálogo CREAR
    if (mostrarDialogoCrear) {
        EmpleadoDialog(
            titulo = "Nuevo Empleado",
            nombreInicial = "",
            rolInicial = "Empleado",
            pinInicial = "",
            onDismiss = { mostrarDialogoCrear = false },
            onConfirm = { nombre, rol, pin ->
                viewModel.crearEmpleado(nombre, rol, pin)
                // Solo cierra si no hay error (el error llega como Snackbar)
                mostrarDialogoCrear = false
            }
        )
    }

    // Diálogo EDITAR
    usuarioAEditar?.let { usuario ->
        EmpleadoDialog(
            titulo = "Editar ${usuario.nombre}",
            nombreInicial = usuario.nombre,
            rolInicial = usuario.rol,
            pinInicial = usuario.pin,
            onDismiss = { usuarioAEditar = null },
            onConfirm = { nombre, rol, pin ->
                viewModel.actualizarEmpleado(usuario, nombre, rol, pin)
                usuarioAEditar = null
            }
        )
    }

    // Diálogo ELIMINAR
    usuarioAEliminar?.let { usuario ->
        ConfirmDeleteDialog(
            title = "Eliminar empleado",
            itemName = usuario.nombre,
            onConfirm = {
                viewModel.eliminarEmpleado(usuario)
                usuarioAEliminar = null
            },
            onDismiss = { usuarioAEliminar = null }
        )
    }
}


// Diálogo reutilizable para crear/editar un empleado

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmpleadoDialog(
    titulo: String,
    nombreInicial: String,
    rolInicial: String,
    pinInicial: String,
    onDismiss: () -> Unit,
    onConfirm: (nombre: String, rol: String, pin: String) -> Unit
) {
    var nombre      by remember { mutableStateOf(nombreInicial) }
    var rol         by remember { mutableStateOf(rolInicial) }
    var pin         by remember { mutableStateOf(pinInicial) }
    var expandedRol by remember { mutableStateOf(false) }

    val roles = listOf("Gerente", "Empleado")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titulo) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                // Nombre
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre completo") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Rol — desplegable
                ExposedDropdownMenuBox(
                    expanded = expandedRol,
                    onExpandedChange = { expandedRol = !expandedRol }
                ) {
                    OutlinedTextField(
                        value = rol,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Rol") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRol)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedRol,
                        onDismissRequest = { expandedRol = false }
                    ) {
                        roles.forEach { opcion ->
                            DropdownMenuItem(
                                text = { Text(opcion) },
                                onClick = { rol = opcion; expandedRol = false }
                            )
                        }
                    }
                }

                // PIN
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pin = it },
                    label = { Text("PIN (4 dígitos)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(nombre, rol, pin) },
                colors = ButtonDefaults.buttonColors(containerColor = ChefCoreColors.PrimaryGreen)
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}