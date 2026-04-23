package es.chefcore.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import es.chefcore.app.ui.components.*
import es.chefcore.app.ui.theme.ChefCoreColors
import es.chefcore.app.viewmodel.RecetaDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecetaDetailScreen(
    recetaId: Int, // <-- Ahora recibe el ID de la receta
    onVolver: () -> Unit, // <-- Usamos tu sistema de navegación original
    viewModel: RecetaDetailViewModel = viewModel()
) {
    // State
    val receta by viewModel.receta.collectAsState()
    val ingredientesEnReceta by viewModel.ingredientesEnReceta.collectAsState()
    val ingredientesDisponibles by viewModel.ingredientesDisponibles.collectAsState()
    val costeTotalProduccion by viewModel.costeTotalProduccion.collectAsState()

    var showSelectorDialog by remember { mutableStateOf(false) }
    var showCantidadDialog by remember { mutableStateOf(false) }
    var ingredienteSeleccionado by remember { mutableStateOf<es.chefcore.app.data.database.Ingrediente?>(null) }
    var ingredienteEnRecetaEditando by remember { mutableStateOf<es.chefcore.app.data.database.IngredienteEnReceta?>(null) }

    // Cargar receta
    LaunchedEffect(recetaId) {
        viewModel.cargarReceta(recetaId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = receta?.nombre ?: "Receta",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) { // <-- Aplicado tu onVolver
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ChefCoreColors.PrimaryGreen,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
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

            // Sección: Información básica
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
                        Text("Tiempo: ${receta?.tiempoPreparacionMinutos ?: 0} minutos")
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
                            // Buscar el ingrediente completo
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

            // Instrucciones
            item {
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
                                text = "Instrucciones",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { /* TODO: Editar instrucciones */ }) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Editar",
                                    tint = ChefCoreColors.AccentYellow
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = receta?.instrucciones?.takeIf { it.isNotBlank() }
                                ?: "Sin instrucciones",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (receta?.instrucciones?.isBlank() != false) {
                                ChefCoreColors.TextMedium
                            } else {
                                ChefCoreColors.TextDark
                            }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) } // Espacio para FAB
        }
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