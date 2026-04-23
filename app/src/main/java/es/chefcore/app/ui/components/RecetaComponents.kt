package es.chefcore.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import es.chefcore.app.data.database.Ingrediente
import es.chefcore.app.data.database.IngredienteEnReceta
import es.chefcore.app.logic.UnitConverter
import es.chefcore.app.ui.theme.ChefCoreColors

// ============================================================================
// SELECTOR DE INGREDIENTE PARA AÑADIR A RECETA
// ============================================================================

/**
 * Diálogo para buscar y seleccionar un ingrediente para añadir a la receta
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredienteSelectorDialog(
    ingredientesDisponibles: List<Ingrediente>,
    onDismiss: () -> Unit,
    onIngredienteSelected: (Ingrediente) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredIngredientes = remember(ingredientesDisponibles, searchQuery) {
        if (searchQuery.isBlank()) {
            ingredientesDisponibles
        } else {
            ingredientesDisponibles.filter {
                it.nombre.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Añadir ingrediente",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Buscador
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Buscar ingrediente...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ChefCoreColors.PrimaryGreen,
                        unfocusedBorderColor = ChefCoreColors.SurfaceGray
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Lista de ingredientes
                if (filteredIngredientes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isBlank()) {
                                "No hay ingredientes disponibles"
                            } else {
                                "No se encontraron ingredientes"
                            },
                            color = ChefCoreColors.TextMedium
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(filteredIngredientes) { ingrediente ->
                            IngredienteSearchItem(
                                ingrediente = ingrediente,
                                onClick = { onIngredienteSelected(ingrediente) }
                            )
                            Divider()
                        }
                    }
                }
            }
        }
    }
}

/**
 * Item de ingrediente en la lista de búsqueda
 */
@Composable
private fun IngredienteSearchItem(
    ingrediente: Ingrediente,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = ingrediente.nombre,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Stock: ${UnitConverter.formatearCantidad(ingrediente.cantidad, ingrediente.unidad)}",
                style = MaterialTheme.typography.bodySmall,
                color = ChefCoreColors.TextMedium
            )
        }
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Añadir",
            tint = ChefCoreColors.PrimaryGreen
        )
    }
}

// ============================================================================
// DIÁLOGO PARA CONFIGURAR CANTIDAD DE INGREDIENTE EN RECETA
// ============================================================================

/**
 * Diálogo para configurar la cantidad necesaria de un ingrediente en la receta
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigurarCantidadDialog(
    ingrediente: Ingrediente,
    cantidadActual: Double = 0.0,
    unidadActual: String = "",
    onDismiss: () -> Unit,
    onConfirm: (cantidad: Double, unidad: String) -> Unit
) {
    var cantidad by remember {
        mutableStateOf(if (cantidadActual > 0) cantidadActual.toString() else "")
    }
    var unidadSeleccionada by remember {
        mutableStateOf(
            if (unidadActual.isNotBlank()) unidadActual else ingrediente.unidad
        )
    }
    var expandedUnidades by remember { mutableStateOf(false) }

    // Unidades compatibles
    val unidadesDisponibles = UnitConverter.obtenerUnidadesCompatibles(ingrediente.unidad)

    // Calcular coste en tiempo real
    val costeCalculado = remember(cantidad, unidadSeleccionada) {
        val cant = cantidad.toDoubleOrNull() ?: 0.0
        if (cant > 0) {
            try {
                UnitConverter.calcularCoste(
                    cantidadNecesaria = cant,
                    unidadReceta = unidadSeleccionada,
                    precioUnitarioBase = ingrediente.precio,
                    unidadBase = ingrediente.unidad
                )
            } catch (e: Exception) {
                0.0
            }
        } else {
            0.0
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Header
                Text(
                    text = ingrediente.nombre,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Stock disponible: ${UnitConverter.formatearCantidad(ingrediente.cantidad, ingrediente.unidad)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ChefCoreColors.TextMedium
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Cantidad
                OutlinedTextField(
                    value = cantidad,
                    onValueChange = { cantidad = it },
                    label = { Text("Cantidad necesaria") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ChefCoreColors.PrimaryGreen
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Selector de unidad
                ExposedDropdownMenuBox(
                    expanded = expandedUnidades,
                    onExpandedChange = { expandedUnidades = !expandedUnidades }
                ) {
                    OutlinedTextField(
                        value = unidadSeleccionada,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Unidad") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedUnidades)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ChefCoreColors.PrimaryGreen
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expandedUnidades,
                        onDismissRequest = { expandedUnidades = false }
                    ) {
                        unidadesDisponibles.forEach { unidad ->
                            DropdownMenuItem(
                                text = { Text(unidad) },
                                onClick = {
                                    unidadSeleccionada = unidad
                                    expandedUnidades = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Vista previa del coste
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = ChefCoreColors.PrimaryGreen.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Coste estimado:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "€${"%.2f".format(costeCalculado)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ChefCoreColors.PrimaryGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Botones
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = {
                            val cant = cantidad.toDoubleOrNull()
                            if (cant != null && cant > 0) {
                                onConfirm(cant, unidadSeleccionada)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = cantidad.toDoubleOrNull()?.let { it > 0 } ?: false,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ChefCoreColors.PrimaryGreen
                        )
                    ) {
                        Text("Añadir")
                    }
                }
            }
        }
    }
}

// ============================================================================
// CARD DE INGREDIENTE EN RECETA (ESCANDALLO)
// ============================================================================

/**
 * Card que muestra un ingrediente ya añadido a la receta con su cantidad
 */
@Composable
fun IngredienteEnRecetaCard(
    ingredienteEnReceta: IngredienteEnReceta,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Info del ingrediente
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ingredienteEnReceta.nombre,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = UnitConverter.formatearCantidad(
                            ingredienteEnReceta.cantidadNecesaria,
                            ingredienteEnReceta.unidad
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ChefCoreColors.TextMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• €${"%.2f".format(ingredienteEnReceta.costeTotal)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ChefCoreColors.PrimaryGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Botones de acción
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar cantidad",
                        tint = ChefCoreColors.AccentYellow
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = ChefCoreColors.ErrorRed
                    )
                }
            }
        }
    }
}

// ============================================================================
// RESUMEN DE COSTES DE RECETA
// ============================================================================

/**
 * Card que muestra el resumen de costes de la receta
 */
@Composable
fun RecetaCosteSummary(
    costeTotalProduccion: Double,
    precioVenta: Double,
    onPrecioVentaChange: (Double) -> Unit
) {
    var precioVentaText by remember { mutableStateOf(precioVenta.toString()) }
    var editingPrecio by remember { mutableStateOf(false) }

    val margen = precioVenta - costeTotalProduccion
    val porcentajeMargen = if (precioVenta > 0) {
        (margen / precioVenta) * 100
    } else {
        0.0
    }
    val esRentable = porcentajeMargen >= 20.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (esRentable) {
                ChefCoreColors.PrimaryGreen.copy(alpha = 0.1f)
            } else {
                ChefCoreColors.AccentYellow.copy(alpha = 0.1f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Resumen económico",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Coste de producción
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Coste de producción:", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "€${"%.2f".format(costeTotalProduccion)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))

            // Precio de venta (editable)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Precio de venta:", style = MaterialTheme.typography.bodyMedium)

                if (editingPrecio) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = precioVentaText,
                            onValueChange = { precioVentaText = it },
                            modifier = Modifier.width(100.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            prefix = { Text("€") }
                        )
                        IconButton(onClick = {
                            precioVentaText.toDoubleOrNull()?.let { onPrecioVentaChange(it) }
                            editingPrecio = false
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "Confirmar")
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "€${"%.2f".format(precioVenta)}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = ChefCoreColors.PrimaryGreen
                        )
                        IconButton(onClick = { editingPrecio = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = ChefCoreColors.AccentYellow)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))

            // Margen
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Ganancia:", style = MaterialTheme.typography.bodyMedium)
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "€${"%.2f".format(margen)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (margen > 0) ChefCoreColors.PrimaryGreen else ChefCoreColors.ErrorRed
                    )
                    Text(
                        text = "${"%.1f".format(porcentajeMargen)}% margen",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (esRentable) ChefCoreColors.PrimaryGreen else ChefCoreColors.AccentYellow
                    )
                }
            }

            // Alerta si no es rentable
            if (!esRentable) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ChefCoreColors.AccentYellow.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = ChefCoreColors.AccentYellow,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "⚠️ Margen bajo. Se recomienda mínimo 20%",
                        style = MaterialTheme.typography.bodySmall,
                        color = ChefCoreColors.TextDark
                    )
                }
            }
        }
    }
}
