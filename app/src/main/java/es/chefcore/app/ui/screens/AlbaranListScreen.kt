package es.chefcore.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import es.chefcore.app.data.database.Albaran
import es.chefcore.app.ui.components.ConfirmDeleteDialog
import es.chefcore.app.ui.components.Sidebar
import es.chefcore.app.ui.theme.ChefCoreColors
import es.chefcore.app.viewmodel.AlbaranViewModel

@Composable
fun AlbaranListScreen(
    esGerente: Boolean = true,
    onSettingsClick: () -> Unit,
    onInventoryClick: () -> Unit,
    onRecipesClick: () -> Unit,
    onScannerClick: () -> Unit,
    onPersonalClick: () -> Unit,
    viewModel: AlbaranViewModel = viewModel()
) {
    val albaranes by viewModel.albaranes.collectAsStateWithLifecycle()
    val usuarios  by viewModel.usuarios.collectAsStateWithLifecycle()

    var albaranAEliminar by remember { mutableStateOf<Albaran?>(null) }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(ChefCoreColors.BackgroundLight)
    ) {
        Sidebar(
            currentScreen = "Albaranes",
            onSettingsClick = onSettingsClick,
            onInventoryClick = onInventoryClick,
            onRecipesClick = onRecipesClick,
            onScannerClick = onScannerClick,
            onPersonalClick = onPersonalClick,
            esGerente = esGerente
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(Color.White)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Albaranes",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = ChefCoreColors.TextDark
                    )
                    Text(
                        text = "${albaranes.size} documentos registrados",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ChefCoreColors.TextMedium
                    )
                }
            }

            if (albaranes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = ChefCoreColors.SurfaceGray
                        )
                        Text(
                            "No hay albaranes registrados.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = ChefCoreColors.TextMedium
                        )
                        Text(
                            "Escanea un albarán desde la pantalla del Escáner.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ChefCoreColors.TextMedium
                        )
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(albaranes) { albaran ->
                        AlbaranCard(
                            albaran = albaran,
                            nombreUsuario = usuarios.find { it.id == albaran.idUsuario }?.nombre ?: "Desconocido",
                            esGerente = esGerente,
                            onDelete = { albaranAEliminar = albaran }
                        )
                    }
                }
            }
        }
    }

    albaranAEliminar?.let { albaran ->
        ConfirmDeleteDialog(
            title = "Eliminar Albarán",
            itemName = "${albaran.proveedor} - ${albaran.fecha}",
            onConfirm = {
                viewModel.eliminar(albaran)
                albaranAEliminar = null
            },
            onDismiss = { albaranAEliminar = null }
        )
    }
}

@Composable
private fun AlbaranCard(
    albaran: Albaran,
    nombreUsuario: String,
    esGerente: Boolean,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        ChefCoreColors.PrimaryGreen.copy(alpha = 0.1f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Receipt,
                    contentDescription = null,
                    tint = ChefCoreColors.PrimaryGreen,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = albaran.proveedor.ifBlank { "Proveedor desconocido" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ChefCoreColors.TextDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = albaran.fecha,
                        style = MaterialTheme.typography.bodySmall,
                        color = ChefCoreColors.TextMedium
                    )
                    Text(
                        text = "Escaneado por: $nombreUsuario",
                        style = MaterialTheme.typography.bodySmall,
                        color = ChefCoreColors.TextMedium
                    )
                }
            }

            if (esGerente) {
                Text(
                    text = "${"%.2f".format(albaran.totalEuros)}€",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = ChefCoreColors.PrimaryGreen
                )
                Spacer(modifier = Modifier.width(16.dp))
            }

            if (esGerente) {
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