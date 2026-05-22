package es.chefcore.app.ui.components

import android.media.RingtoneManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.chefcore.app.data.database.IngredienteEnReceta
import es.chefcore.app.logic.UnitConverter
import es.chefcore.app.ui.theme.ChefCoreColors
import kotlinx.coroutines.delay


// INPUT DE INSTRUCCIONES CON PASOS NUMERADOS


@Composable
fun InstruccionesStepsInput(
    instrucciones: String,
    onInstruccionesChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val pasos = remember(instrucciones) {
        val lista = instrucciones.split("\n").toMutableList()
        if (lista.isEmpty() || (lista.size == 1 && lista[0].isBlank())) {
            mutableListOf("")
        } else {
            lista.toMutableList()
        }
    }.toMutableStateList()

    fun syncToString() { onInstruccionesChange(pasos.joinToString("\n")) }

    Column(modifier = modifier) {
        Text(
            text = "Instrucciones de preparación",
            style = MaterialTheme.typography.labelLarge,
            color = ChefCoreColors.TextMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        pasos.forEachIndexed { index, paso ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = if (paso.isBlank()) ChefCoreColors.SurfaceGray
                            else ChefCoreColors.PrimaryGreen,
                            shape = CircleShape
                        )
                ) {
                    Text(
                        text = "${index + 1}º",
                        color = if (paso.isBlank()) ChefCoreColors.TextMedium else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = paso,
                    onValueChange = { nuevoValor ->
                        if (nuevoValor.contains("\n")) {
                            pasos[index] = nuevoValor.substringBefore("\n")
                            pasos.add(index + 1, "")
                            syncToString()
                        } else {
                            pasos[index] = nuevoValor
                            syncToString()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Paso ${index + 1}...", color = ChefCoreColors.TextMedium) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    singleLine = false,
                    maxLines = 3,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ChefCoreColors.PrimaryGreen,
                        unfocusedBorderColor = ChefCoreColors.SurfaceGray
                    )
                )
                if (pasos.size > 1) {
                    IconButton(
                        onClick = { pasos.removeAt(index); syncToString() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Eliminar paso",
                            tint = ChefCoreColors.ErrorRed.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(36.dp))
                }
            }
        }
        TextButton(
            onClick = { pasos.add(""); syncToString() },
            modifier = Modifier.padding(start = 40.dp)
        ) {
            Text("+ Añadir paso", color = ChefCoreColors.PrimaryGreen, style = MaterialTheme.typography.bodyMedium)
        }
    }
}


// HELPER — FORMATO DE TIEMPO


/** Formatea segundos a MM:SS o H:MM:SS */
fun formatearTiempo(segundos: Int): String {
    val h = segundos / 3600
    val m = (segundos % 3600) / 60
    val s = segundos % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}


// WIDGET TEMPORIZADOR FLEXIBLE — selector directo de MM y SS


@Composable
fun TimerWidget(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var minutos           by remember { mutableIntStateOf(0) }
    var segundos          by remember { mutableIntStateOf(0) }
    var segundosRestantes by remember { mutableIntStateOf(0) }
    var corriendo         by remember { mutableStateOf(false) }
    val totalSegundos     = minutos * 60 + segundos
    val terminado         = !corriendo && segundosRestantes == 0 && totalSegundos > 0

    fun recalcular() { segundosRestantes = minutos * 60 + segundos }

    // Tick y sonido al terminar
    LaunchedEffect(corriendo) {
        if (corriendo) {
            while (segundosRestantes > 0) {
                delay(1000L)
                segundosRestantes--
            }
            corriendo = false

            // Sonido de alarma al terminar
            try {
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = RingtoneManager.getRingtone(context, uri)
                ringtone?.play()
                delay(4000L)
                ringtone?.stop()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val colorFondo = when {
        terminado -> ChefCoreColors.PrimaryGreen
        corriendo -> Color(0xFF1A3A2A)
        else      -> Color.White.copy(alpha = 0.08f)
    }

    Column(
        modifier = modifier
            .background(colorFondo, RoundedCornerShape(16.dp))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when {
            // TERMINADO
            terminado -> {
                Text("✓  ¡Listo!", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                IconButton(
                    onClick = { minutos = 0; segundos = 0; segundosRestantes = 0 },
                    modifier = Modifier.size(44.dp).background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(Icons.Default.Refresh, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }

            // CORRIENDO
            corriendo -> {
                Text(
                    formatearTiempo(segundosRestantes),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { corriendo = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Icon(Icons.Default.Pause, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Pausar", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    IconButton(
                        onClick = { corriendo = false; segundosRestantes = totalSegundos },
                        modifier = Modifier.size(44.dp).background(Color.White.copy(alpha = 0.12f), CircleShape)
                    ) {
                        Icon(Icons.Default.Refresh, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // CONFIGURACIÓN / PAUSADO
            else -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TimeUnitSelector(
                        label = "min",
                        value = minutos,
                        onDecrement = { if (minutos > 0) { minutos--; recalcular() } },
                        onIncrement = { if (minutos < 99) { minutos++; recalcular() } },
                        onValueChange = { minutos = it.coerceIn(0, 99); recalcular() }
                    )
                    Text(":", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    TimeUnitSelector(
                        label = "seg",
                        value = segundos,
                        onDecrement = { if (segundos > 0) { segundos--; recalcular() } },
                        onIncrement = { if (segundos < 59) { segundos++; recalcular() } },
                        onValueChange = { segundos = it.coerceIn(0, 59); recalcular() }
                    )
                }

                if (totalSegundos > 0) {
                    Button(
                        onClick = {
                            if (segundosRestantes == 0) segundosRestantes = totalSegundos
                            corriendo = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ChefCoreColors.PrimaryGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (segundosRestantes in 1 until totalSegundos) "Reanudar" else "Iniciar",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeUnitSelector(
    label: String,
    value: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    onValueChange: (Int) -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Fila: [−]  número  [+]
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(
                onClick = onDecrement,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
            ) {
                Text("−", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                text = "%02d".format(value),
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            IconButton(
                onClick = onIncrement,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
            ) {
                Text("+", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }

        Text(label, color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp)
    }
}


// PANEL DE INGREDIENTES CON RACIONES AJUSTADAS


@Composable
fun PanelIngredientes(
    ingredientes: List<IngredienteEnReceta>,
    raciones: Int,
    modifier: Modifier = Modifier
) {
    var expandido by remember { mutableStateOf(true) }

    Column(modifier = modifier) {
        TextButton(
            onClick = { expandido = !expandido },
            colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.75f))
        ) {
            Icon(
                imageVector = if (expandido) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Ingredientes para $raciones ración${if (raciones > 1) "es" else ""}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        AnimatedVisibility(visible = expandido) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(ingredientes) { ing ->
                    val cantidadAjustada = ing.cantidadNecesaria * raciones
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.12f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = ing.nombre,
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = UnitConverter.formatearCantidad(cantidadAjustada, ing.unidad),
                                color = ChefCoreColors.PrimaryGreen,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}


// MODO COCINA — PANTALLA COMPLETA


/**
 * Pantalla de Modo Cocina — pantalla completa, oscura, letra gigante.
 *
 * @param nombreReceta   Nombre del plato
 * @param instrucciones  Pasos separados por \n
 * @param raciones       Raciones seleccionadas al preparar (para ajustar cantidades)
 * @param ingredientes   Ingredientes de la receta (para mostrar panel con cantidades ajustadas)
 * @param onSalir        Cerrar modo cocina
 */
@Composable
fun ModoCocinaScreen(
    nombreReceta: String,
    instrucciones: String,
    raciones: Int = 1,
    ingredientes: List<IngredienteEnReceta> = emptyList(),
    onSalir: () -> Unit
) {
    val pasos = remember(instrucciones) {
        instrucciones.split("\n").map { it.trim() }.filter { it.isNotBlank() }
    }

    var pasoActual by remember { mutableIntStateOf(0) }
    val totalPasos = pasos.size

    if (totalPasos == 0) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFF1F2937)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text("Esta receta no tiene instrucciones.", color = Color.White, fontSize = 22.sp, textAlign = TextAlign.Center)
                Button(onClick = onSalir, colors = ButtonDefaults.buttonColors(containerColor = ChefCoreColors.PrimaryGreen)) {
                    Text("Volver")
                }
            }
        }
        return
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF1F2937))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // CABECERA
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = nombreReceta,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Paso ${pasoActual + 1} de $totalPasos" +
                                    if (raciones > 1) " · $raciones raciones" else "",
                            color = ChefCoreColors.PrimaryGreen,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = onSalir,
                        modifier = Modifier.size(52.dp).background(Color.White.copy(alpha = 0.12f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Salir", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }

                // Barra de progreso
                LinearProgressIndicator(
                    progress = { (pasoActual + 1).toFloat() / totalPasos.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = ChefCoreColors.PrimaryGreen,
                    trackColor = Color.White.copy(alpha = 0.15f)
                )

                // Panel de ingredientes ajustados
                if (ingredientes.isNotEmpty()) {
                    PanelIngredientes(
                        ingredientes = ingredientes,
                        raciones = raciones,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    )
                }
            }

            // TEXTO DEL PASO + TEMPORIZADOR
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    AnimatedContent(
                        targetState = pasoActual,
                        transitionSpec = {
                            if (targetState > initialState)
                                slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                            else
                                slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                        },
                        label = "paso_animation"
                    ) { paso ->
                        Text(
                            text = pasos[paso],
                            color = Color.White,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = 50.sp
                        )
                    }

                    // Temporizador — siempre visible, el usuario lo configura
                    TimerWidget(modifier = Modifier.fillMaxWidth())
                }
            }

            // BOTONES ANTERIOR / SIGUIENTE
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { if (pasoActual > 0) pasoActual-- },
                    enabled = pasoActual > 0,
                    modifier = Modifier.weight(1f).height(80.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.15f),
                        disabledContainerColor = Color.White.copy(alpha = 0.04f),
                        contentColor = Color.White,
                        disabledContentColor = Color.White.copy(alpha = 0.3f)
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.ArrowBack, null, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("ANTERIOR", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = { if (pasoActual < totalPasos - 1) pasoActual++ else onSalir() },
                    modifier = Modifier.weight(1f).height(80.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ChefCoreColors.PrimaryGreen, contentColor = Color.White)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Text(
                            text = if (pasoActual < totalPasos - 1) "SIGUIENTE" else "✓  LISTO",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (pasoActual < totalPasos - 1) {
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }
        }
    }
}