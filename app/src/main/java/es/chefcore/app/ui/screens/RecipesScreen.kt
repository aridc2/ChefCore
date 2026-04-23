package es.chefcore.app.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import es.chefcore.app.data.database.IngredienteDao
import es.chefcore.app.data.database.Receta
import es.chefcore.app.data.database.RecetaDao
import es.chefcore.app.logic.CocinaManager
import es.chefcore.app.ui.components.RecetaItem
import es.chefcore.app.ui.components.Sidebar
import es.chefcore.app.ui.theme.ChefCoreColors
import kotlinx.coroutines.launch

@Composable
fun RecipesScreen(
    rDao: RecetaDao,
    iDao: IngredienteDao,
    cocinaManager: CocinaManager,
    onSettingsClick: () -> Unit,
    onInventoryClick: () -> Unit,
    onPersonalClick: () -> Unit,
    onScannerClick: () -> Unit
) {
    val recetas by rDao.obtenerTodas().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedRecetaId by remember { mutableStateOf<Int?>(null) }
    var mostrarDialogo by remember { mutableStateOf(false) }

    val filteredRecetas = recetas.filter { it.nombre.contains(searchQuery, ignoreCase = true) }

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

        // Panel izquierdo: Búsqueda + lista + botón crear
        Column(
            modifier = Modifier
                // ✅ MAGIA DE DISEÑO: Ocupa todo el espacio si no hay receta seleccionada, o 350dp si la hay.
                .then(if (selectedRecetaId == null) Modifier.weight(1f) else Modifier.width(350.dp))
                .fillMaxHeight()
                .background(color = Color.White)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Mis Recetas", style = MaterialTheme.typography.headlineMedium, color = ChefCoreColors.TextDark)
                Button(
                    onClick = { mostrarDialogo = true },
                    colors = ButtonDefaults.buttonColors(containerColor = ChefCoreColors.PrimaryGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Crear")
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
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

            if (filteredRecetas.isEmpty()) {
                Text("No hay recetas.", style = MaterialTheme.typography.bodyMedium, color = ChefCoreColors.TextMedium)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredRecetas) { receta ->

                        // Envolvemos TU componente en una caja para darle un fondo cuando esté seleccionado
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = if (receta.id == selectedRecetaId)
                                        ChefCoreColors.PrimaryGreenLight.copy(alpha = 0.3f)
                                    else
                                        Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(4.dp)
                        ) {
                            // ✅ AQUÍ USAMOS TU COMPONENTE ORIGINAL (RecetaItem)
                            RecetaItem(
                                receta = receta,
                                onClick = { selectedRecetaId = receta.id }
                            )
                        }

                    }
                }
            }
        }

        // Panel derecho: Detalle de receta seleccionada
        if (selectedRecetaId != null) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                // El fondo blanco y padding extra ya lo maneja la pantalla interior, así queda más limpio
            ) {
                // ✅ LLAMAMOS A LA PANTALLA PROFESIONAL CON LOS BOTONES
                RecetaDetailScreen(
                    recetaId = selectedRecetaId!!,
                    onVolver = { selectedRecetaId = null } // Al darle a volver, se cierra el panel derecho
                )
            }
        }
    }

    // --- DIÁLOGO PARA CREAR RECETA ---
    if (mostrarDialogo) {
        var nombre by remember { mutableStateOf("") }
        var precioVenta by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { mostrarDialogo = false },
            title = { Text("Nueva Receta") },
            text = {
                Column {
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text("Nombre del plato") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = precioVenta,
                        onValueChange = { precioVenta = it },
                        label = { Text("Precio de venta (€)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            if (nombre.isNotBlank()) {
                                rDao.insertar(Receta(
                                    nombre = nombre.trim().replaceFirstChar { it.uppercase() },
                                    precioVenta = precioVenta.replace(",", ".").toDoubleOrNull() ?: 0.0
                                ))
                                mostrarDialogo = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ChefCoreColors.PrimaryGreen)
                ) { Text("Crear") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogo = false }) { Text("Cancelar") }
            }
        )
    }
}