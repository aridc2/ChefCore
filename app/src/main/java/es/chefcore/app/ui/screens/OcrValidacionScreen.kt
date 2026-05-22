package es.chefcore.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import es.chefcore.app.data.database.Ingrediente
import es.chefcore.app.logic.ItemAlbaran
import es.chefcore.app.logic.ResultadoOcr
import es.chefcore.app.ui.theme.ChefCoreColors
import es.chefcore.app.viewmodel.OcrViewModel

private val UNIDADES = listOf("kg", "g", "L", "ml", "cl", "ud")

/** Levenshtein normalizado: 1.0 = identicos, 0.0 = completamente distintos */
private fun calcularSimilitud(a: String, b: String): Double {
    val x = a.lowercase(); val y = b.lowercase()
    val maxLen = maxOf(x.length, y.length)
    if (maxLen == 0) return 1.0
    val dp = Array(x.length + 1) { IntArray(y.length + 1) }
    for (i in 0..x.length) dp[i][0] = i
    for (j in 0..y.length) dp[0][j] = j
    for (i in 1..x.length) for (j in 1..y.length) {
        dp[i][j] = if (x[i - 1] == y[j - 1]) dp[i - 1][j - 1]
        else 1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
    }
    return 1.0 - dp[x.length][y.length].toDouble() / maxLen
}

/** Estado mutable por ítem en la UI */
private data class ItemUI(
    val descripcion: String,
    val cantidadStr: String,
    val unidad: String,
    val precioStr: String,
    val seleccionado: Boolean = true
)

private fun ItemAlbaran.toUI() = ItemUI(
    descripcion  = descripcion,
    cantidadStr  = if (cantidad > 0) cantidad.toString() else "1",
    unidad       = unidad,
    precioStr    = if (precioUnitario > 0) "%.2f".format(precioUnitario) else ""
)
private fun ItemUI.toItemAlbaran() = ItemAlbaran(
    descripcion    = descripcion,
    cantidad       = cantidadStr.replace(",", ".").toDoubleOrNull() ?: 1.0,
    unidad         = unidad,
    precioUnitario = precioStr.replace(",", ".").toDoubleOrNull() ?: 0.0
)

/**
 * RF-02 — Pantalla de validación post-OCR.
 * Izquierda: campos del albarán editables.
 * Derecha: lista de ítems detectados con checkbox + selector de unidad.
 * Al guardar → albarán en Room + CocinaManager actualiza inventario y precios.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrValidacionScreen(
    resultado: ResultadoOcr,
    onGuardar: (proveedor: String, fecha: String, total: Double, items: List<ItemAlbaran>) -> Unit,
    onCancelar: () -> Unit
) {
    var proveedor by remember { mutableStateOf(resultado.proveedor) }
    var fecha     by remember { mutableStateOf(resultado.fecha) }
    var total     by remember { mutableStateOf(resultado.totalEuros.toString()) }
    var itemsUI   by remember { mutableStateOf(resultado.items.map { it.toUI() }) }

    val seleccionados = itemsUI.count { it.seleccionado }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(ChefCoreColors.BackgroundLight)
            .systemBarsPadding()
    ) {
        // Columna izquierda: formulario
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(Color.White)
                .padding(32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.FactCheck, null,
                    tint = ChefCoreColors.PrimaryGreen, modifier = Modifier.size(32.dp))
                Column {
                    Text("Validar albarán",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold, color = ChefCoreColors.TextDark)
                    Text("Revisa y corrige antes de guardar",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ChefCoreColors.TextMedium)
                }
            }

            HorizontalDivider()

            OutlinedTextField(
                value = proveedor, onValueChange = { proveedor = it },
                label = { Text("Proveedor") },
                leadingIcon = { Icon(Icons.Default.Store, null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp), singleLine = true
            )
            OutlinedTextField(
                value = fecha, onValueChange = { fecha = it },
                label = { Text("Fecha (DD/MM/AAAA)") },
                leadingIcon = { Icon(Icons.Default.CalendarToday, null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp), singleLine = true
            )
            OutlinedTextField(
                value = total, onValueChange = { total = it },
                label = { Text("Total (€)") },
                leadingIcon = { Icon(Icons.Default.EuroSymbol, null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp), singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            // Resumen de ítems seleccionados
            if (itemsUI.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = ChefCoreColors.PrimaryGreen.copy(alpha = 0.08f)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Inventory2, null,
                            tint = ChefCoreColors.PrimaryGreen, modifier = Modifier.size(20.dp))
                        Text(
                            "$seleccionados de ${itemsUI.size} ingredientes se actualizarán en el inventario",
                            style = MaterialTheme.typography.bodySmall,
                            color = ChefCoreColors.PrimaryGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val totalDouble = total.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val seleccion = itemsUI.filter { it.seleccionado }.map { it.toItemAlbaran() }
                    onGuardar(proveedor, fecha, totalDouble, seleccion)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ChefCoreColors.PrimaryGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Guardar y actualizar inventario",
                    style = MaterialTheme.typography.labelLarge)
            }
            OutlinedButton(
                onClick = onCancelar,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Cancelar") }
        }

        // Columna derecha: ítems interactivos
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Ingredientes detectados",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold, color = ChefCoreColors.TextDark)
                    Text("Marca los que quieres actualizar en inventario",
                        style = MaterialTheme.typography.bodySmall,
                        color = ChefCoreColors.TextMedium)
                }
                // Seleccionar/deseleccionar todos
                TextButton(onClick = {
                    val todosOn = itemsUI.all { it.seleccionado }
                    itemsUI = itemsUI.map { it.copy(seleccionado = !todosOn) }
                }) {
                    Text(if (itemsUI.all { it.seleccionado }) "Quitar todos" else "Todos",
                        color = ChefCoreColors.PrimaryGreen)
                }
            }

            Spacer(Modifier.height(12.dp))

            if (itemsUI.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                ) {
                    Spacer(Modifier.height(16.dp))
                    Icon(Icons.Default.SearchOff, null,
                        modifier = Modifier.size(48.dp), tint = ChefCoreColors.TextMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("No se identificaron líneas de producto.",
                        color = ChefCoreColors.TextMedium,
                        style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                    Text("Texto leído por OCR (para diagnóstico):",
                        style = MaterialTheme.typography.labelSmall,
                        color = ChefCoreColors.TextMedium)
                    Spacer(Modifier.height(4.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = ChefCoreColors.BackgroundLight),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            resultado.textoRaw.take(1000),
                            modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = ChefCoreColors.TextDark
                        )
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(itemsUI) { idx, item ->
                        ItemEditableCard(
                            item = item,
                            onToggle = {
                                itemsUI = itemsUI.toMutableList().also {
                                    it[idx] = it[idx].copy(seleccionado = !it[idx].seleccionado)
                                }
                            },
                            onDescripcionChange = { nuevaDesc ->
                                itemsUI = itemsUI.toMutableList().also {
                                    it[idx] = it[idx].copy(descripcion = nuevaDesc)
                                }
                            },
                            onUnidadChange = { nuevaUnidad ->
                                itemsUI = itemsUI.toMutableList().also {
                                    it[idx] = it[idx].copy(unidad = nuevaUnidad)
                                }
                            },
                            onPrecioChange = { nuevoPrecio ->
                                itemsUI = itemsUI.toMutableList().also {
                                    it[idx] = it[idx].copy(precioStr = nuevoPrecio)
                                }
                            },
                            onCantidadChange = { nuevaCant ->
                                itemsUI = itemsUI.toMutableList().also {
                                    it[idx] = it[idx].copy(cantidadStr = nuevaCant)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ItemEditableCard(
    item: ItemUI,
    onToggle: () -> Unit,
    onDescripcionChange: (String) -> Unit = {},
    onUnidadChange: (String) -> Unit,
    onPrecioChange: (String) -> Unit = {},
    onCantidadChange: (String) -> Unit = {}
) {
    var expandedUnidad by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.seleccionado) Color.White else ChefCoreColors.BackgroundLight
        ),
        elevation = CardDefaults.cardElevation(if (item.seleccionado) 2.dp else 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Checkbox(
                    checked = item.seleccionado,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(checkedColor = ChefCoreColors.PrimaryGreen)
                )
                // Nombre del ingrediente — editable para corregir errores del OCR
                OutlinedTextField(
                    value = item.descripcion,
                    onValueChange = onDescripcionChange,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = if (item.seleccionado) ChefCoreColors.TextDark else ChefCoreColors.TextMedium
                    ),
                    singleLine = true,
                    enabled = item.seleccionado
                )
            }

            if (item.seleccionado) {
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cantidad
                    OutlinedTextField(
                        value = item.cantidadStr,
                        onValueChange = onCantidadChange,
                        label = { Text("Cant.", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        textStyle = MaterialTheme.typography.bodySmall,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    // Unidad
                    ExposedDropdownMenuBox(
                        expanded = expandedUnidad,
                        onExpandedChange = { expandedUnidad = !expandedUnidad },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = item.unidad,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Unidad", style = MaterialTheme.typography.labelSmall) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedUnidad) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            textStyle = MaterialTheme.typography.bodySmall,
                            singleLine = true
                        )
                        ExposedDropdownMenu(expanded = expandedUnidad, onDismissRequest = { expandedUnidad = false }) {
                            UNIDADES.forEach { u ->
                                DropdownMenuItem(text = { Text(u) }, onClick = { onUnidadChange(u); expandedUnidad = false })
                            }
                        }
                    }
                    // Precio por unidad
                    OutlinedTextField(
                        value = item.precioStr,
                        onValueChange = onPrecioChange,
                        label = { Text("€/ud", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        textStyle = MaterialTheme.typography.bodySmall,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        placeholder = { Text("0.00", style = MaterialTheme.typography.bodySmall,
                            color = ChefCoreColors.TextMedium) }
                    )
                }
            }
        }
    }
}