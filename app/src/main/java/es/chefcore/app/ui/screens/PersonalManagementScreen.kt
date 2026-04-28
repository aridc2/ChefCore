package es.chefcore.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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

    var mostrarDialogo by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(color = ChefCoreColors.BackgroundLight)
    ) {
        // Sidebar
        Sidebar(
            currentScreen = "Personal",
            onSettingsClick = onSettingsClick,
            onInventoryClick = onInventoryClick,
            onRecipesClick = onRecipesClick,
            onScannerClick = onScannerClick,
            onPersonalClick = { }
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
                    onClick = { mostrarDialogo = true },
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
                            onEdit = { },
                            onDelete = { }
                        )
                    }
                }
            }
        }
    }

    // --- DIÁLOGO PARA AÑADIR EMPLEADO ---
    if (mostrarDialogo) {
        var nombre by remember { mutableStateOf("") }
        var rol by remember { mutableStateOf("") }
        var pin by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { mostrarDialogo = false },
            title = { Text("Nuevo Empleado") },
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
                        onValueChange = { if (it.length <= 4) pin = it },
                        label = { Text("PIN (4 dígitos)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.crearEmpleado(nombre, rol, pin)
                        mostrarDialogo = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ChefCoreColors.PrimaryGreen)
                ) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogo = false }) { Text("Cancelar") }
            }
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
