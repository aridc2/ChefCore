package es.chefcore.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.chefcore.app.data.database.*

@Composable
fun IngredienteItem(item: Ingrediente) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(item.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${item.precio} €/unidad", style = MaterialTheme.typography.bodySmall)
            }
            Text("${item.cantidad} ${item.unidad}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

// ✅ AQUÍ ESTÁ EL CAMBIO: Ahora recibe 'receta' y 'onClick', y es 'clickable'
@Composable
fun RecetaItem(receta: Receta, onClick: () -> Unit) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() } // Esto hace que la tarjeta reaccione al toque
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(receta.nombre, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
            Text("Tiempo: ${receta.tiempoPreparacionMinutos} min", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun AlbaranItem(item: Albaran) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(item.proveedor, fontWeight = FontWeight.Bold)
                Text(item.fecha, style = MaterialTheme.typography.bodySmall)
            }
            Text("${item.totalEuros} €", fontWeight = FontWeight.Black)
        }
    }
}