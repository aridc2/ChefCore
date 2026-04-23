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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import es.chefcore.app.logic.CocinaManager
import es.chefcore.app.logic.UnitConverter
import es.chefcore.app.ui.components.InventoryProductCard
import es.chefcore.app.ui.components.Sidebar
import es.chefcore.app.ui.theme.ChefCoreColors
import kotlinx.coroutines.launch

@Composable
fun InventoryScreen(
    iDao: IngredienteDao,
    cocinaManager: CocinaManager,
    onSettingsClick: () -> Unit,
    onRecipesClick: () -> Unit,
    onPersonalClick: () -> Unit,
    onScannerClick: () -> Unit
) {
    // Datos REALES desde Room
    val ingredientes by iDao.obtenerTodos().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var mostrarDialogo by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(color = ChefCoreColors.BackgroundLight)
    ) {
        // Sidebar
        Sidebar(
            currentScreen = "Inventory",
            onSettingsClick = onSettingsClick,
            onInventoryClick = { },
            onRecipesClick = onRecipesClick,
            onScannerClick = onScannerClick,
            onPersonalClick = onPersonalClick
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
                Text(
                    text = "Inventario",
                    style = MaterialTheme.typography.displaySmall,
                    color = ChefCoreColors.TextDark
                )
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
                    Text("Añadir Producto")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (ingredientes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("El inventario está vacío. Pulsa 'Añadir Producto' para empezar.",
                        style = MaterialTheme.typography.bodyLarge, color = ChefCoreColors.TextMedium)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(ingredientes) { ing ->
                        InventoryProductCard(
                            name = ing.nombre,
                            stock = "Stock: ${UnitConverter.formatearCantidad(ing.cantidad, ing.unidad)}",
                            onClick = { }
                        )
                    }
                }
            }
        }
    }

    // --- DIÁLOGO PARA AÑADIR PRODUCTO ---
    if (mostrarDialogo) {
        var nombre by remember { mutableStateOf("") }
        var cantidad by remember { mutableStateOf("") }
        var precioTotal by remember { mutableStateOf("") }
        var unidadSeleccionada by remember { mutableStateOf("kg") }
        var menuExpandido by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { mostrarDialogo = false },
            title = { Text("Añadir al Almacén") },
            text = {
                Column {
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text("Nombre del producto") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = cantidad,
                            onValueChange = { cantidad = it },
                            label = { Text("Cantidad") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Box(modifier = Modifier.weight(0.6f)) {
                            OutlinedButton(
                                onClick = { menuExpandido = true },
                                modifier = Modifier.padding(top = 8.dp)
                            ) { Text(unidadSeleccionada) }
                            DropdownMenu(
                                expanded = menuExpandido,
                                onDismissRequest = { menuExpandido = false }
                            ) {
                                UnitConverter.TODAS_LAS_UNIDADES.forEach { u ->
                                    DropdownMenuItem(
                                        text = { Text(u) },
                                        onClick = { unidadSeleccionada = u; menuExpandido = false }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = precioTotal,
                        onValueChange = { precioTotal = it },
                        label = { Text("Precio Total (€)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val cant = cantidad.replace(",", ".").toDoubleOrNull() ?: 0.0
                            val precio = precioTotal.replace(",", ".").toDoubleOrNull() ?: 0.0
                            if (nombre.isNotBlank() && cant > 0) {
                                cocinaManager.registrarEntradaStock(nombre, cant, unidadSeleccionada, precio)
                                mostrarDialogo = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ChefCoreColors.PrimaryGreen)
                ) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogo = false }) { Text("Cancelar") }
            }
        )
    }
}
