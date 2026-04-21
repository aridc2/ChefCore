package es.chefcore.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.chefcore.app.data.database.IngredienteDao
import es.chefcore.app.data.database.RecetaDao
import es.chefcore.app.logic.CocinaManager

@Composable
fun RecetaDetailScreen(
    nombreReceta: String,
    rDao: RecetaDao,
    iDao: IngredienteDao,
    cocinaManager: CocinaManager,
    onVolver: () -> Unit
) {
    val receta by rDao.observarPorNombre(nombreReceta).collectAsState(initial = null)
    val ingredientesReceta by remember(receta?.id) {
        if (receta != null) rDao.observarIngredientesDeReceta(receta!!.id)
        else kotlinx.coroutines.flow.flowOf(emptyList())
    }.collectAsState(initial = emptyList())

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF3F4F6))) {
        Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {

            // --- CABECERA ESTILO FIGMA ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onVolver) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color(0xFF1F2937))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = receta?.nombre ?: "Cargando...",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- TARJETA DE RENTABILIDAD (BANNER SUPERIOR) ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)) // Verde muy clarito
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Rentabilidad Sugerida:",
                        fontSize = 18.sp,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "82%", // Aquí iría tu lógica de CocinaManager
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- CUERPO PRINCIPAL (LISTA + BOTÓN) ---
            Row(modifier = Modifier.fillMaxSize()) {
                // Columna Izquierda: Ingredientes
                Column(modifier = Modifier.weight(1f)) {
                    Text("Ingredientes", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF1F2937))
                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn {
                        items(ingredientesReceta) { ing ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(ing.nombre, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text("${ing.cantidadNecesaria} ${ing.unidad}", color = Color(0xFF6B7280), fontSize = 14.sp)
                                    }
                                    Text(
                                        text = "${"%.2f".format(ing.costeTotal)} €",
                                        color = Color(0xFFDC2626), // Rojo del Figma
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(32.dp))

                // Columna Derecha: Acciones
                Column(modifier = Modifier.width(280.dp)) {
                    Button(
                        onClick = { /* Lógica de cocinar */ },
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("¡COCINAR!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Tarjeta de resumen de costes
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Resumen Económico", fontWeight = FontWeight.Bold, color = Color(0xFF6B7280))
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Coste Total")
                                Text("4.20 €", fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("PVP")
                                Text("${receta?.precioVenta ?: 0.0} €", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}