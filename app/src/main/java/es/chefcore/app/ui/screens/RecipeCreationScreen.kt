package es.chefcore.app.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import es.chefcore.app.data.database.Ingrediente
import es.chefcore.app.logic.UnitConverter
import es.chefcore.app.ui.components.ImageUploadPicker
import es.chefcore.app.ui.components.Sidebar
import es.chefcore.app.ui.theme.ChefCoreColors
import es.chefcore.app.viewmodel.RecipesViewModel
import kotlinx.coroutines.flow.first


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeCreationScreen(
    viewModel: RecipesViewModel,
    recetaId: Int? = null, // ✅ NUEVO: si no es null, modo edición
    onSaveRecipe: (String, Double, Int, Uri?, String, List<Pair<Int, Double>>) -> Unit,
    onCancel: () -> Unit,
    onSettingsClick: () -> Unit,
    onInventoryClick: () -> Unit,
    onRecipesClick: () -> Unit,
    onPersonalClick: () -> Unit,
    onScannerClick: () -> Unit
) {
    val inventario by viewModel.ingredientesDisponibles.collectAsState()
    val focusManager = LocalFocusManager.current

    // Estados básicos
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var tiempoMinutos by remember { mutableStateOf("30") }
    var mainImageUri by remember { mutableStateOf<Uri?>(null) }
    var instructions by remember { mutableStateOf("") }

    // Estados para el Escandallo
    var ingredientesAñadidos by remember { mutableStateOf<List<Pair<Ingrediente, Double>>>(emptyList()) }
    var expandedIngredientes by remember { mutableStateOf(false) }
    var expandedUnidades by remember { mutableStateOf(false) }
    var selectedIngrediente by remember { mutableStateOf<Ingrediente?>(null) }
    var cantidadText by remember { mutableStateOf("") }
    var unidadSeleccionada by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(recetaId) {
        if (recetaId != null) {
            isLoading = true

            try {
                val receta = viewModel.obtenerReceta(recetaId).first()
                receta?.let {
                    name = it.nombre
                    price = it.precioVenta.toString()
                    tiempoMinutos = it.tiempoPreparacionMinutos.toString()
                    mainImageUri = it.imagenUri?.let { uri -> Uri.parse(uri) }
                    instructions = it.instrucciones ?: ""
                }

                val ingredientesReceta = viewModel.obtenerIngredientesDeReceta(recetaId).first()
                ingredientesAñadidos = ingredientesReceta.mapNotNull { ingReceta ->
                    inventario.find { it.id == ingReceta.ingredienteId }?.let { ingrediente ->
                        Pair(ingrediente, ingReceta.cantidadNecesaria)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    val esModoEdicion = recetaId != null

    Row(modifier = Modifier.fillMaxSize().background(ChefCoreColors.BackgroundLight)) {
        Sidebar(
            currentScreen = "RecipeCreation",
            onSettingsClick = onSettingsClick,
            onInventoryClick = onInventoryClick,
            onRecipesClick = onRecipesClick,
            onScannerClick = onScannerClick,
            onPersonalClick = onPersonalClick
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ChefCoreColors.PrimaryGreen)
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color.White)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { focusManager.clearFocus() })
                    }
                    .padding(32.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // === CABECERA ===
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (esModoEdicion) "Editar Receta" else "Nueva Receta con Escandallo",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = ChefCoreColors.TextDark
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = onCancel) { Text("Cancelar") }
                        Button(
                            onClick = {
                                val listaParaDb = ingredientesAñadidos.map { Pair(it.first.id, it.second) }
                                val tiempo = tiempoMinutos.toIntOrNull() ?: 30
                                onSaveRecipe(
                                    name,
                                    price.toDoubleOrNull() ?: 0.0,
                                    tiempo,
                                    mainImageUri,
                                    instructions,
                                    listaParaDb
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ChefCoreColors.PrimaryGreen),
                            enabled = name.isNotBlank()
                        ) {
                            Text(if (esModoEdicion) "Actualizar Receta" else "Guardar Todo")
                        }
                    }
                }

                // === DATOS GENERALES ===
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Imagen del plato", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        ImageUploadPicker(imageUri = mainImageUri, onImageSelected = { mainImageUri = it })
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Nombre de la receta") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = price,
                                onValueChange = { price = it },
                                label = { Text("Precio (€)") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )

                            OutlinedTextField(
                                value = tiempoMinutos,
                                onValueChange = {
                                    if (it.all { c -> c.isDigit() }) tiempoMinutos = it
                                },
                                label = { Text("Tiempo (min)") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = ChefCoreColors.PrimaryGreen
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        OutlinedTextField(
                            value = instructions,
                            onValueChange = { instructions = it },
                            label = { Text("Instrucciones de preparación") },
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            shape = RoundedCornerShape(12.dp),
                            maxLines = 5
                        )
                    }
                }

                HorizontalDivider()

                // === SECCIÓN DE ESCANDALLO ===
                Text("Composición del Plato", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExposedDropdownMenuBox(
                        expanded = expandedIngredientes,
                        onExpandedChange = { expandedIngredientes = !expandedIngredientes },
                        modifier = Modifier.weight(2f)
                    ) {
                        OutlinedTextField(
                            value = selectedIngrediente?.nombre ?: "Seleccionar ingrediente...",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedIngredientes) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedIngredientes,
                            onDismissRequest = { expandedIngredientes = false }
                        ) {
                            if (inventario.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Sin ingredientes en inventario") },
                                    onClick = {}
                                )
                            } else {
                                inventario.forEach { ing ->
                                    DropdownMenuItem(
                                        text = { Text("${ing.nombre} (Base: ${ing.unidad})") },
                                        onClick = {
                                            selectedIngrediente = ing
                                            unidadSeleccionada = ing.unidad
                                            expandedIngredientes = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = cantidadText,
                        onValueChange = { cantidadText = it },
                        label = { Text("Cant.") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenuBox(
                        expanded = expandedUnidades,
                        onExpandedChange = {
                            if (selectedIngrediente != null) {
                                expandedUnidades = !expandedUnidades
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = if (unidadSeleccionada.isEmpty()) "--" else unidadSeleccionada,
                            onValueChange = {},
                            readOnly = true,
                            enabled = selectedIngrediente != null,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedUnidades) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = if (selectedIngrediente == null) Color.Gray else ChefCoreColors.PrimaryGreen,
                                disabledBorderColor = Color.LightGray,
                                disabledTrailingIconColor = Color.Gray
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = expandedUnidades,
                            onDismissRequest = { expandedUnidades = false }
                        ) {
                            selectedIngrediente?.let { ing ->
                                val opciones = UnitConverter.obtenerUnidadesCompatibles(ing.unidad)
                                opciones.forEach { u ->
                                    DropdownMenuItem(
                                        text = { Text(u) },
                                        onClick = {
                                            unidadSeleccionada = u
                                            expandedUnidades = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val cantVal = cantidadText.replace(",", ".").toDoubleOrNull() ?: 0.0
                            if (selectedIngrediente != null && cantVal > 0 && unidadSeleccionada.isNotEmpty()) {
                                val normalizada = UnitConverter.convertir(
                                    cantidad = cantVal,
                                    unidadOrigen = unidadSeleccionada,
                                    unidadDestino = selectedIngrediente!!.unidad
                                )

                                ingredientesAñadidos = ingredientesAñadidos + Pair(selectedIngrediente!!, normalizada)

                                cantidadText = ""
                                selectedIngrediente = null
                                unidadSeleccionada = ""
                                focusManager.clearFocus()
                            }
                        },
                        modifier = Modifier.height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ChefCoreColors.AccentYellow,
                            contentColor = ChefCoreColors.TextDark
                        ),
                        enabled = selectedIngrediente != null && cantidadText.isNotBlank() && unidadSeleccionada.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Añadir")
                    }
                }

                // === LISTA DE INGREDIENTES ===
                if (ingredientesAñadidos.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = ChefCoreColors.SurfaceGray),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Ingredientes añadidos (${ingredientesAñadidos.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = ChefCoreColors.TextDark
                            )
                            HorizontalDivider()
                            ingredientesAñadidos.forEach { (ing, cant) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("• ${ing.nombre}", fontWeight = FontWeight.SemiBold)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${"%.2f".format(cant)} ${ing.unidad}",
                                            color = ChefCoreColors.TextMedium,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        IconButton(onClick = {
                                            ingredientesAñadidos = ingredientesAñadidos.filterNot { it.first == ing }
                                        }) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = null,
                                                tint = ChefCoreColors.ErrorRed,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = ChefCoreColors.SurfaceGray.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Sin ingredientes añadidos",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ChefCoreColors.TextMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}