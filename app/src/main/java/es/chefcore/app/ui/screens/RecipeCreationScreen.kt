package es.chefcore.app.ui.screens

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import es.chefcore.app.data.database.Ingrediente
import es.chefcore.app.logic.UnitConverter
import es.chefcore.app.ui.components.ImageUploadPicker
import es.chefcore.app.ui.components.InstruccionesStepsInput
import es.chefcore.app.ui.components.Sidebar
import es.chefcore.app.ui.theme.ChefCoreColors
import es.chefcore.app.viewmodel.RecipesViewModel
import es.chefcore.app.viewmodel.VoiceViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeCreationScreen(
    viewModel: RecipesViewModel,
    voiceViewModel: VoiceViewModel = viewModel(),
    recetaId: Int? = null,
    nombreInicial: String? = null,
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
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var name         by remember { mutableStateOf(nombreInicial ?: "") }
    var price        by remember { mutableStateOf("") }
    var tiempoMinutos by remember { mutableStateOf("30") }
    var mainImageUri by remember { mutableStateOf<Uri?>(null) }
    var instructions by remember { mutableStateOf("") }

    var ingredientesAnadidos  by remember { mutableStateOf<List<Pair<Ingrediente, Double>>>(emptyList()) }
    var expandedIngredientes  by remember { mutableStateOf(false) }
    var expandedUnidades      by remember { mutableStateOf(false) }
    var selectedIngrediente   by remember { mutableStateOf<Ingrediente?>(null) }
    var cantidadText          by remember { mutableStateOf("") }
    var unidadSeleccionada    by remember { mutableStateOf("") }
    var isLoading             by remember { mutableStateOf(recetaId != null) }

    // Activar contexto de receta en VoiceCommander cuando se llega con nombre inicial
    LaunchedEffect(nombreInicial) {
        if (nombreInicial != null) {
            voiceViewModel.procesarVoz("abrir receta $nombreInicial")
        }
    }

    // Handler de voz local: pasos van al campo instrucciones
    val voceFeedback by voiceViewModel.feedback.collectAsStateWithLifecycle()
    LaunchedEffect(voceFeedback) {
        voceFeedback?.let { msg ->
            when {
                msg.startsWith("Paso:") -> {
                    val paso = msg.substringAfter("Paso:").trim()
                    instructions = if (instructions.isEmpty()) paso else "$instructions\n$paso"
                    voiceViewModel.clearFeedback()
                }
                else -> {
                    snackbarHostState.showSnackbar(msg)
                    voiceViewModel.clearFeedback()
                }
            }
        }
    }

    // Cargar datos si es modo edición
    LaunchedEffect(recetaId) {
        if (recetaId != null) {
            try {
                val receta = viewModel.obtenerReceta(recetaId).firstOrNull()
                receta?.let {
                    name          = it.nombre
                    price         = it.precioVenta.toString()
                    tiempoMinutos = it.tiempoPreparacionMinutos.toString()
                    mainImageUri  = it.imagenUri?.let { uri -> Uri.parse(uri) }
                    instructions  = it.instrucciones ?: ""
                }
                var inventarioActual = viewModel.ingredientesDisponibles.first()
                if (inventarioActual.isEmpty()) {
                    delay(150)
                    inventarioActual = viewModel.ingredientesDisponibles.first()
                }
                val ingredientesReceta = viewModel.obtenerIngredientesDeReceta(recetaId).first()
                ingredientesAnadidos = ingredientesReceta.mapNotNull { ingReceta ->
                    inventarioActual.find { it.id == ingReceta.ingredienteId }
                        ?.let { ing -> Pair(ing, ingReceta.cantidadNecesaria) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    val esModoEdicion = recetaId != null

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { _ ->
        Row(modifier = Modifier.fillMaxSize().systemBarsPadding().background(ChefCoreColors.BackgroundLight)) {
            Sidebar(
                currentScreen  = "RecipeCreation",
                onSettingsClick = onSettingsClick,
                onInventoryClick = onInventoryClick,
                onRecipesClick  = onRecipesClick,
                onScannerClick  = onScannerClick,
                onPersonalClick = onPersonalClick
            )

            if (isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ChefCoreColors.PrimaryGreen)
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f).fillMaxHeight().background(Color.White)
                        .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
                        .padding(32.dp)
                        .verticalScroll(rememberScrollState())
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Cabecera
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
                                    val lista   = ingredientesAnadidos.map { Pair(it.first.id, it.second) }
                                    val tiempo  = tiempoMinutos.toIntOrNull() ?: 30
                                    val uriPerm = mainImageUri?.let { copiarImagenALocal(context, it) }
                                    onSaveRecipe(name, price.toDoubleOrNull() ?: 0.0, tiempo, uriPerm, instructions, lista)
                                },
                                colors  = ButtonDefaults.buttonColors(containerColor = ChefCoreColors.PrimaryGreen),
                                enabled = name.isNotBlank()
                            ) {
                                Text(if (esModoEdicion) "Actualizar Receta" else "Guardar Todo")
                            }
                        }
                    }

                    // Imagen + campos básicos
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Imagen del plato", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            ImageUploadPicker(imageUri = mainImageUri, onImageSelected = { mainImageUri = it })
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedTextField(
                                value = name, onValueChange = { name = it },
                                label = { Text("Nombre de la receta") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = price, onValueChange = { price = it },
                                    label = { Text("Precio") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                OutlinedTextField(
                                    value = tiempoMinutos,
                                    onValueChange = { if (it.all { c -> c.isDigit() }) tiempoMinutos = it },
                                    label = { Text("Tiempo (min)") },
                                    leadingIcon = { Icon(Icons.Default.Schedule, null, tint = ChefCoreColors.PrimaryGreen) },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                            InstruccionesStepsInput(
                                instrucciones = instructions,
                                onInstruccionesChange = { instructions = it },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    HorizontalDivider()

                    // Escandallo
                    Text("Composicion del Plato", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

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
                                onValueChange = {}, readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedIngredientes) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(expanded = expandedIngredientes, onDismissRequest = { expandedIngredientes = false }) {
                                if (inventario.isEmpty()) {
                                    DropdownMenuItem(text = { Text("Sin ingredientes en inventario") }, onClick = {})
                                } else {
                                    inventario.forEach { ing ->
                                        DropdownMenuItem(
                                            text = { Text("${ing.nombre} (Base: ${ing.unidad})") },
                                            onClick = { selectedIngrediente = ing; unidadSeleccionada = ing.unidad; expandedIngredientes = false }
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = cantidadText, onValueChange = { cantidadText = it },
                            label = { Text("Cant.") }, modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            shape = RoundedCornerShape(12.dp)
                        )

                        ExposedDropdownMenuBox(
                            expanded = expandedUnidades,
                            onExpandedChange = { if (selectedIngrediente != null) expandedUnidades = !expandedUnidades },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = unidadSeleccionada.ifEmpty { "--" }, onValueChange = {},
                                readOnly = true, enabled = selectedIngrediente != null,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedUnidades) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(expanded = expandedUnidades, onDismissRequest = { expandedUnidades = false }) {
                                selectedIngrediente?.let { ing ->
                                    UnitConverter.obtenerUnidadesCompatibles(ing.unidad).forEach { u ->
                                        DropdownMenuItem(text = { Text(u) }, onClick = { unidadSeleccionada = u; expandedUnidades = false })
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val cantVal = cantidadText.replace(",", ".").toDoubleOrNull() ?: 0.0
                                if (selectedIngrediente != null && cantVal > 0 && unidadSeleccionada.isNotEmpty()) {
                                    val normalizada = UnitConverter.convertir(cantVal, unidadSeleccionada, selectedIngrediente!!.unidad)
                                    ingredientesAnadidos = ingredientesAnadidos + Pair(selectedIngrediente!!, normalizada)
                                    cantidadText = ""; selectedIngrediente = null; unidadSeleccionada = ""
                                    focusManager.clearFocus()
                                }
                            },
                            modifier = Modifier.height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ChefCoreColors.AccentYellow, contentColor = ChefCoreColors.TextDark),
                            enabled = selectedIngrediente != null && cantidadText.isNotBlank() && unidadSeleccionada.isNotEmpty()
                        ) { Icon(Icons.Default.Add, null) }
                    }

                    // Lista ingredientes añadidos
                    if (ingredientesAnadidos.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = ChefCoreColors.SurfaceGray),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Ingredientes anadidos (${ingredientesAnadidos.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                HorizontalDivider()
                                ingredientesAnadidos.forEach { (ing, cant) ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("- ${ing.nombre}", fontWeight = FontWeight.SemiBold)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("${"%.2f".format(cant)} ${ing.unidad}", color = ChefCoreColors.TextMedium, style = MaterialTheme.typography.bodyMedium)
                                            IconButton(onClick = { ingredientesAnadidos = ingredientesAnadidos.filterNot { it.first == ing } }) {
                                                Icon(Icons.Default.Delete, null, tint = ChefCoreColors.ErrorRed, modifier = Modifier.size(20.dp))
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
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("Sin ingredientes anadidos", style = MaterialTheme.typography.bodyMedium, color = ChefCoreColors.TextMedium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    } // Scaffold
}

fun copiarImagenALocal(context: Context, uriOrigen: Uri): Uri? {
    if (uriOrigen.scheme == "file" || uriOrigen.toString().contains(context.packageName)) return uriOrigen
    return try {
        val input  = context.contentResolver.openInputStream(uriOrigen)
        val file   = File(context.filesDir, "receta_${System.currentTimeMillis()}.jpg")
        val output = FileOutputStream(file)
        input?.copyTo(output)
        input?.close(); output.close()
        Uri.fromFile(file)
    } catch (e: Exception) { e.printStackTrace(); null }
}