package es.chefcore.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import es.chefcore.app.ui.components.*
import es.chefcore.app.ui.theme.ChefCoreColors
import es.chefcore.app.viewmodel.RecetaDetailViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecetaDetailScreen(
    recetaId: Int,
    onVolver: () -> Unit,
    onEditarReceta: ((Int) -> Unit)? = null,
    viewModel: RecetaDetailViewModel = viewModel()
) {
    // Estados del ViewModel
    val receta by viewModel.receta.collectAsState()
    val ingredientesEnReceta by viewModel.ingredientesEnReceta.collectAsState()
    val ingredientesDisponibles by viewModel.ingredientesDisponibles.collectAsState()
    val costeTotalProduccion by viewModel.costeTotalProduccion.collectAsState()

    // Estados locales
    var showSelectorDialog by remember { mutableStateOf(false) }
    var showCantidadDialog by remember { mutableStateOf(false) }
    var ingredienteSeleccionado by remember { mutableStateOf<es.chefcore.app.data.database.Ingrediente?>(null) }
    var ingredienteEnRecetaEditando by remember { mutableStateOf<es.chefcore.app.data.database.IngredienteEnReceta?>(null) }

    // Estados Modo Cocina
    var modoCocinaActivo by remember { mutableStateOf(false) }
    var showRacionesDialog by remember { mutableStateOf(false) }
    var raciones by remember { mutableIntStateOf(1) }

    // Snackbar para feedback de prepararPlato
    val snackbarHostState = remember { SnackbarHostState() }
    val feedbackMessage by viewModel.feedbackMessage.collectAsState()
    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedback()
        }
    }

    // Cargar receta
    LaunchedEffect(recetaId) {
        viewModel.cargarReceta(recetaId)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = receta?.nombre ?: "Receta",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    // Botón Preparar Plato — siempre visible, grande
                    Button(
                        onClick = { showRacionesDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = ChefCoreColors.PrimaryGreen
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .height(40.dp)
                    ) {
                        Text(
                            text = "🍳  Preparar",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    if (onEditarReceta != null) {
                        IconButton(onClick = { onEditarReceta(recetaId) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ChefCoreColors.PrimaryGreen,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showSelectorDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Añadir ingrediente") },
                containerColor = ChefCoreColors.AccentYellow,
                contentColor = ChefCoreColors.TextDark
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                receta?.imagenUri?.takeIf { it.isNotEmpty() }?.let { imageUri ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = receta?.nombre,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                } ?: run {
                    // Placeholder si no hay imagen
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = ChefCoreColors.SurfaceGray
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = ChefCoreColors.TextMedium
                                )
                                Text(
                                    "Sin imagen",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ChefCoreColors.TextMedium
                                )
                            }
                        }
                    }
                }
            }

            // Información básica
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Información",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Tiempo
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        ChefCoreColors.SurfaceGray,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                Text("⏱️", style = MaterialTheme.typography.headlineSmall)
                                Text(
                                    "${receta?.tiempoPreparacionMinutos ?: 0} min",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            // Precio de venta
                            if ((receta?.precioVenta ?: 0.0) > 0) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            ChefCoreColors.SurfaceGray,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(12.dp)
                                ) {
                                    Text("💰", style = MaterialTheme.typography.headlineSmall)
                                    Text(
                                        "${"%.2f".format(receta?.precioVenta)}€",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ChefCoreColors.PrimaryGreen
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Sección: Escandallo (ingredientes)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Escandallo",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${ingredientesEnReceta.size} ingredientes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ChefCoreColors.TextMedium
                    )
                }
            }

            // Lista de ingredientes
            if (ingredientesEnReceta.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = ChefCoreColors.SurfaceGray
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = ChefCoreColors.TextMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Sin ingredientes",
                                style = MaterialTheme.typography.bodyLarge,
                                color = ChefCoreColors.TextMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Pulsa el botón + para añadir",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ChefCoreColors.TextMedium
                            )
                        }
                    }
                }
            } else {
                items(ingredientesEnReceta) { ingrediente ->
                    IngredienteEnRecetaCard(
                        ingredienteEnReceta = ingrediente,
                        onEdit = {
                            ingredienteEnRecetaEditando = ingrediente
                            val ing = ingredientesDisponibles.find {
                                it.id == ingrediente.ingredienteId
                            }
                            ingredienteSeleccionado = ing
                            showCantidadDialog = true
                        },
                        onDelete = {
                            viewModel.eliminarIngrediente(ingrediente.ingredienteId)
                        }
                    )
                }
            }

            // Resumen de costes
            if (ingredientesEnReceta.isNotEmpty()) {
                item {
                    RecetaCosteSummary(
                        costeTotalProduccion = costeTotalProduccion,
                        precioVenta = receta?.precioVenta ?: 0.0,
                        onPrecioVentaChange = { nuevoPrecio ->
                            receta?.let { viewModel.actualizarPrecioVenta(it, nuevoPrecio) }
                        }
                    )
                }
            }

            // Instrucciones como pasos numerados + botón Preparar Plato
            item {
                val pasos = remember(receta?.instrucciones) {
                    receta?.instrucciones
                        ?.split("\n")
                        ?.map { it.trim() }
                        ?.filter { it.isNotBlank() }
                        ?: emptyList()
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Instrucciones",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (pasos.isNotEmpty()) {
                                Text(
                                    "${pasos.size} pasos",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ChefCoreColors.TextMedium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (pasos.isEmpty()) {
                            Text(
                                "Sin instrucciones",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ChefCoreColors.TextMedium
                            )
                        } else {
                            pasos.forEachIndexed { index, paso ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(
                                                ChefCoreColors.PrimaryGreen,
                                                CircleShape
                                            )
                                    ) {
                                        Text(
                                            "${index + 1}",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        paso,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ChefCoreColors.TextDark,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (index < pasos.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 40.dp),
                                        color = ChefCoreColors.SurfaceGray
                                    )
                                }
                            }
                        }

                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    // ── DIÁLOGO: ¿Cuántas raciones? ──────────────────────────────────────────
    if (showRacionesDialog) {
        AlertDialog(
            onDismissRequest = { showRacionesDialog = false },
            title = { Text("¿Cuántas raciones?") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Los ingredientes se descontarán del inventario.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ChefCoreColors.TextMedium
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(32.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = { if (raciones > 1) raciones-- },
                            modifier = Modifier.size(52.dp)
                        ) {
                            Text("−", style = MaterialTheme.typography.headlineSmall)
                        }
                        Text(
                            text = "$raciones",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = ChefCoreColors.PrimaryGreen
                        )
                        FilledTonalIconButton(
                            onClick = { raciones++ },
                            modifier = Modifier.size(52.dp)
                        ) {
                            Text("+", style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.prepararPlato(recetaId, raciones)
                        showRacionesDialog = false
                        modoCocinaActivo = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ChefCoreColors.PrimaryGreen
                    )
                ) {
                    Text("Empezar a cocinar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRacionesDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // ── MODO COCINA: se renderiza encima de todo ──────────────────────────────
    if (modoCocinaActivo) {
        ModoCocinaScreen(
            nombreReceta = receta?.nombre ?: "",
            instrucciones = receta?.instrucciones ?: "",
            raciones = raciones,
            ingredientes = ingredientesEnReceta,
            onSalir = { modoCocinaActivo = false }
        )
    }

    // Diálogo selector de ingredientes
    if (showSelectorDialog) {
        IngredienteSelectorDialog(
            ingredientesDisponibles = ingredientesDisponibles,
            onDismiss = { showSelectorDialog = false },
            onIngredienteSelected = { ingrediente ->
                ingredienteSeleccionado = ingrediente
                ingredienteEnRecetaEditando = null
                showSelectorDialog = false
                showCantidadDialog = true
            }
        )
    }

    // Diálogo configurar cantidad
    if (showCantidadDialog && ingredienteSeleccionado != null) {
        ConfigurarCantidadDialog(
            ingrediente = ingredienteSeleccionado!!,
            cantidadActual = ingredienteEnRecetaEditando?.cantidadNecesaria ?: 0.0,
            unidadActual = ingredienteEnRecetaEditando?.unidad ?: "",
            onDismiss = {
                showCantidadDialog = false
                ingredienteSeleccionado = null
                ingredienteEnRecetaEditando = null
            },
            onConfirm = { cantidad, unidad ->
                if (ingredienteEnRecetaEditando != null) {
                    viewModel.actualizarCantidadIngrediente(
                        ingredienteId = ingredienteSeleccionado!!.id,
                        nuevaCantidad = cantidad,
                        nuevaUnidad = unidad
                    )
                } else {
                    viewModel.añadirIngrediente(
                        ingredienteId = ingredienteSeleccionado!!.id,
                        cantidad = cantidad,
                        unidad = unidad
                    )
                }
                showCantidadDialog = false
                ingredienteSeleccionado = null
                ingredienteEnRecetaEditando = null
            }
        )
    }
}