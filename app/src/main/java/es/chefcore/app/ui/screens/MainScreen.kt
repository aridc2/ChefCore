package es.chefcore.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import es.chefcore.app.data.database.*
import es.chefcore.app.logic.CocinaManager
import es.chefcore.app.ui.components.*

@Composable
fun MainScreen(
    ingredientes: List<Ingrediente>,
    recetas: List<Receta>,
    albaranes: List<Albaran>,
    textoVoz: String,
    rDao: RecetaDao,
    iDao: IngredienteDao,
    cocinaManager: CocinaManager
) {
    var pantallaActual by remember { mutableStateOf("LISTA") }
    var recetaSeleccionadaId by remember { mutableStateOf(0) }

    LaunchedEffect(textoVoz) {
        if (textoVoz.startsWith("NAV_DETALLE|")) {
            val idStr = textoVoz.substringAfter("|")
            recetaSeleccionadaId = idStr.toIntOrNull() ?: 1
            pantallaActual = "DETALLE"
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Surface(
            tonalElevation = 8.dp,
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            val textoMostrar = if (textoVoz.startsWith("NAV_DETALLE")) "Abriendo receta..." else textoVoz
            Text(textoMostrar, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (pantallaActual == "LISTA") {
            LazyColumn(modifier = Modifier.weight(1f)) {
                item { SectionTitle("📦 Inventario Actual") }
                items(ingredientes) { IngredienteItem(it) }

                item { SectionTitle("📖 Recetas") }

                items(recetas) { receta ->
                    RecetaItem(
                        receta = receta,
                        onClick = {
                            recetaSeleccionadaId = receta.id
                            pantallaActual = "DETALLE"
                        }
                    )
                }

                item { SectionTitle("📄 Últimos Albaranes") }
                items(albaranes) { AlbaranItem(it) }
            }
        } else {
            RecetaDetailScreen(
                recetaId = recetaSeleccionadaId,
                onVolver = { pantallaActual = "LISTA" }
            )
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(vertical = 12.dp),
        color = MaterialTheme.colorScheme.primary
    )
}