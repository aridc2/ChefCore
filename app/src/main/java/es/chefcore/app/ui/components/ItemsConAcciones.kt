package es.chefcore.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import es.chefcore.app.data.database.Ingrediente
import es.chefcore.app.data.database.Receta
import es.chefcore.app.logic.UnitConverter
import es.chefcore.app.ui.theme.ChefCoreColors


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecetaItemConAcciones(
    receta: Receta,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, ChefCoreColors.SurfaceGray)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(ChefCoreColors.SurfaceGray),
                contentAlignment = Alignment.Center
            ) {
                if (!receta.imagenUri.isNullOrEmpty()) {
                    AsyncImage(
                        model = receta.imagenUri,
                        contentDescription = receta.nombre,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = ChefCoreColors.TextMedium,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }


            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = receta.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ChefCoreColors.TextDark,
                    maxLines = 1
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = ChefCoreColors.TextMedium
                    )
                    Text(
                        text = "${receta.tiempoPreparacionMinutos} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = ChefCoreColors.TextMedium
                    )
                    if (receta.precioVenta > 0) {
                        Text("•", color = ChefCoreColors.TextMedium)
                        Text(
                            text = "${"%.2f".format(receta.precioVenta)}€",
                            style = MaterialTheme.typography.bodySmall,
                            color = ChefCoreColors.PrimaryGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = ChefCoreColors.AccentYellow,
                        modifier = Modifier.size(26.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = ChefCoreColors.ErrorRed,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredienteItemConAcciones(
    ingrediente: Ingrediente,
    esGerente: Boolean = true,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, ChefCoreColors.SurfaceGray)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(ChefCoreColors.PrimaryGreen.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = ChefCoreColors.PrimaryGreen,
                    modifier = Modifier.size(30.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ingrediente.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ChefCoreColors.TextDark,
                    maxLines = 1
                )
                Text(
                    text = if (esGerente)
                        "${"%.2f".format(ingrediente.cantidad)} ${ingrediente.unidad} • ${"%.2f".format(ingrediente.precio)}€/${ingrediente.unidad}"
                    else
                        "${"%.2f".format(ingrediente.cantidad)} ${ingrediente.unidad}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ChefCoreColors.TextMedium
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = ChefCoreColors.AccentYellow,
                        modifier = Modifier.size(26.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = ChefCoreColors.ErrorRed,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}


@Composable
fun ConfirmDeleteDialog(
    title: String,
    itemName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = ChefCoreColors.ErrorRed,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                "¿Estás seguro de que quieres eliminar \"$itemName\"?\n\nEsta acción no se puede deshacer.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ChefCoreColors.ErrorRed
                )
            ) {
                Text("Eliminar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarIngredienteDialog(
    ingrediente: Ingrediente,
    onDismiss: () -> Unit,
    onConfirm: (nombre: String, cantidad: Double, precio: Double, unidad: String) -> Unit
) {
    var nombre by remember { mutableStateOf(ingrediente.nombre) }
    var cantidad by remember { mutableStateOf(ingrediente.cantidad.toString()) }
    var precio by remember { mutableStateOf(ingrediente.precio.toString()) }
    var unidad by remember { mutableStateOf(ingrediente.unidad) }
    var expandedUnidad by remember { mutableStateOf(false) }

    val unidades = UnitConverter.obtenerUnidadesCompatibles(unidad)
        .ifEmpty { listOf("kg", "g", "L", "ml", "cl", "ud") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar ${ingrediente.nombre}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = cantidad,
                        onValueChange = { cantidad = it },
                        label = { Text("Cantidad") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenuBox(
                        expanded = expandedUnidad,
                        onExpandedChange = { expandedUnidad = !expandedUnidad },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = unidad,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Unidad") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedUnidad) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedUnidad,
                            onDismissRequest = { expandedUnidad = false }
                        ) {
                            unidades.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text(u) },
                                    onClick = { unidad = u; expandedUnidad = false }
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = precio,
                    onValueChange = { precio = it },
                    label = { Text("Precio por $unidad") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        nombre,
                        cantidad.replace(",", ".").toDoubleOrNull() ?: ingrediente.cantidad,
                        precio.replace(",", ".").toDoubleOrNull() ?: ingrediente.precio,
                        unidad
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = ChefCoreColors.PrimaryGreen)
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}