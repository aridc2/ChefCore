package es.chefcore.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import es.chefcore.app.R
import es.chefcore.app.data.database.Ingrediente
import es.chefcore.app.data.database.Receta
import es.chefcore.app.logic.RentabilidadReceta
import es.chefcore.app.logic.UnitConverter
import es.chefcore.app.ui.theme.ChefCoreColors

// ============================================================================
// SIDEBAR
// ============================================================================

/**
 * Sidebar completo con distribución vertical correcta
 * BOTONES GRANDES para uso en cocina (48dp icons, 120dp wide)
 */
@Composable
fun Sidebar(
    currentScreen: String,
    onSettingsClick: () -> Unit,
    onInventoryClick: () -> Unit,
    onRecipesClick: () -> Unit,
    onScannerClick: () -> Unit,
    onPersonalClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(140.dp)  // Más ancho para botones grandes
            .fillMaxHeight()
            .background(color = Color.White)
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Ajustes arriba
        SidebarButton(
            icon = Icons.Default.Settings,
            label = "Ajustes",
            onClick = onSettingsClick
        )

        // Espaciador para empujar los iconos principales hacia el centro
        Spacer(modifier = Modifier.weight(1f))

        // Iconos principales agrupados con más espacio
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)  // Más espacio entre botones
        ) {
            SidebarButton(
                icon = painterResource(id = R.drawable.ic_inventory),
                label = "Inventario",
                onClick = onInventoryClick,
                isActive = currentScreen == "Inventory"
            )
            SidebarButton(
                icon = painterResource(id = R.drawable.ic_recipes),
                label = "Recetas",
                onClick = onRecipesClick,
                isActive = currentScreen == "Recipes"
            )
            SidebarButton(
                icon = painterResource(id = R.drawable.ic_scanner),
                label = "Escáner",
                onClick = onScannerClick,
                isActive = currentScreen == "Scanner"
            )
            SidebarButton(
                icon = painterResource(id = R.drawable.ic_personal),
                label = "Personal",
                onClick = onPersonalClick,
                isActive = currentScreen == "Personal"
            )
        }

        // Espaciador inferior
        Spacer(modifier = Modifier.weight(1f))
    }
}

/**
 * Botón de la barra lateral (con ImageVector)
 * TAMAÑO GRANDE para uso en cocina
 */
@Composable
fun SidebarButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    isActive: Boolean = false
) {
    Column(
        modifier = Modifier
            .width(120.dp)  // Más ancho
            .background(
                color = if (isActive) ChefCoreColors.PrimaryGreen else Color.Transparent,
                shape = RoundedCornerShape(16.dp)  // Bordes más redondeados
            )
            .clickable(onClick = onClick)
            .padding(12.dp),  // Más padding
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(48.dp),  // Iconos mucho más grandes
            tint = if (isActive) Color.White else ChefCoreColors.TextDark
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,  // Texto un poco más grande
            color = if (isActive) Color.White else ChefCoreColors.TextDark
        )
    }
}

/**
 * Botón de la barra lateral (con Painter para iconos personalizados)
 * TAMAÑO GRANDE para uso en cocina
 */
@Composable
fun SidebarButton(
    icon: Painter,
    label: String,
    onClick: () -> Unit,
    isActive: Boolean = false
) {
    Column(
        modifier = Modifier
            .width(120.dp)  // Más ancho
            .background(
                color = if (isActive) ChefCoreColors.PrimaryGreen else Color.Transparent,
                shape = RoundedCornerShape(16.dp)  // Bordes más redondeados
            )
            .clickable(onClick = onClick)
            .padding(12.dp),  // Más padding
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = icon,
            contentDescription = label,
            modifier = Modifier.size(48.dp),  // Iconos mucho más grandes
            tint = if (isActive) Color.White else ChefCoreColors.TextDark
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,  // Texto un poco más grande
            color = if (isActive) Color.White else ChefCoreColors.TextDark
        )
    }
}

// ============================================================================
// BOTONES PRINCIPALES
// ============================================================================

/**
 * Botón principal verde
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = ChefCoreColors.PrimaryGreen,
            contentColor = Color.White,
            disabledContainerColor = ChefCoreColors.SurfaceGray,
            disabledContentColor = ChefCoreColors.TextMedium
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium)
    }
}

/**
 * Botón secundario amarillo
 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = ChefCoreColors.AccentYellow,
            contentColor = ChefCoreColors.TextDark
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium)
    }
}

// ============================================================================
// CAMPOS DE TEXTO
// ============================================================================

/**
 * Campo de texto personalizado
 */
@Composable
fun ChefCoreTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = ChefCoreColors.SurfaceGray,
            focusedBorderColor = ChefCoreColors.PrimaryGreen,
            unfocusedBorderColor = ChefCoreColors.SurfaceGray,
            focusedTextColor = ChefCoreColors.TextDark,
            unfocusedTextColor = ChefCoreColors.TextDark
        )
    )
}

// ============================================================================
// CARDS DE INGREDIENTE
// ============================================================================

/**
 * Card de ingrediente COMPLETA con stock, unidad y precio
 * Muestra:
 * - Nombre del ingrediente
 * - Stock con unidad formateada (usando UnitConverter)
 * - Precio unitario (SOLO para Gerente)
 */
@Composable
fun IngredienteCard(
    ingrediente: Ingrediente,
    userRole: String, // "Gerente" o "Empleado"
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Columna izquierda: Info del ingrediente
            Column(modifier = Modifier.weight(1f)) {
                // Nombre
                Text(
                    text = ingrediente.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    color = ChefCoreColors.TextDark,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Stock con UNIDAD
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_inventory),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = ChefCoreColors.TextMedium
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Stock: ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ChefCoreColors.TextMedium
                    )
                    Text(
                        text = UnitConverter.formatearCantidad(
                            ingrediente.cantidad,
                            ingrediente.unidad
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ChefCoreColors.TextDark,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                // PRECIO - Solo visible para Gerente
                if (userRole == "Gerente") {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "💰 ",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "€${"%.2f".format(ingrediente.precio)}/${ingrediente.unidad}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ChefCoreColors.PrimaryGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            
            // Indicador visual de stock bajo (si tienes campo stock_minimo)
            // Descomenta si añades este campo a la entidad
            /*
            if (ingrediente.cantidad < (ingrediente.stock_minimo ?: 0.0)) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Stock bajo",
                    tint = ChefCoreColors.ErrorRed,
                    modifier = Modifier.size(24.dp)
                )
            }
            */
        }
    }
}

/**
 * Card de ingrediente SIMPLE (versión anterior para compatibilidad)
 */
@Composable
fun InventoryProductCard(
    name: String,
    stock: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .size(width = 160.dp, height = 120.dp)
            .background(color = Color.White, shape = RoundedCornerShape(12.dp))
            .border(width = 1.dp, color = ChefCoreColors.SurfaceGray, shape = RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            color = ChefCoreColors.TextDark
        )
        Text(
            text = stock,
            style = MaterialTheme.typography.bodySmall,
            color = ChefCoreColors.TextMedium
        )
    }
}

// ============================================================================
// CARDS DE RECETA
// ============================================================================

/**
 * Card de receta COMPLETA con costes, PVP y margen
 * Muestra:
 * - Nombre y descripción de la receta
 * - Tabla financiera (Coste | PVP | Ganancia) SOLO para Gerente
 * - Alerta visual si margen < 20%
 */
@Composable
fun RecetaCardCompleta(
    receta: Receta,
    rentabilidad: RentabilidadReceta?, // Puede ser null si aún no se calculó
    userRole: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Nombre de la receta
            Text(
                text = receta.nombre,
                style = MaterialTheme.typography.titleLarge,
                color = ChefCoreColors.TextDark,
                fontWeight = FontWeight.Bold
            )
            
            // Descripción (si existe)
            if (!receta.instrucciones.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = receta.instrucciones,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ChefCoreColors.TextMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // INFORMACIÓN FINANCIERA - Solo para Gerente
            if (userRole == "Gerente" && rentabilidad != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = ChefCoreColors.SurfaceGray)
                Spacer(modifier = Modifier.height(12.dp))
                
                // Tabla de 3 columnas: Coste | PVP | Ganancia
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // COSTE
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Coste",
                            style = MaterialTheme.typography.labelSmall,
                            color = ChefCoreColors.TextMedium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "€${"%.2f".format(rentabilidad.coste)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = ChefCoreColors.TextDark,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // PVP
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "PVP",
                            style = MaterialTheme.typography.labelSmall,
                            color = ChefCoreColors.TextMedium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "€${"%.2f".format(rentabilidad.pvp)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = ChefCoreColors.PrimaryGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // GANANCIA
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Ganancia",
                            style = MaterialTheme.typography.labelSmall,
                            color = ChefCoreColors.TextMedium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "€${"%.2f".format(rentabilidad.margen)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (rentabilidad.margen > 0) ChefCoreColors.PrimaryGreen else ChefCoreColors.ErrorRed,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${"%.1f".format(rentabilidad.porcentajeMargen)}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (rentabilidad.esRentable) ChefCoreColors.PrimaryGreen else ChefCoreColors.AccentYellow
                        )
                    }
                }
                
                // ALERTA si margen bajo
                if (!rentabilidad.esRentable) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = ChefCoreColors.AccentYellow.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = ChefCoreColors.AccentYellow,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Margen bajo (< 20%)",
                            style = MaterialTheme.typography.labelSmall,
                            color = ChefCoreColors.TextDark
                        )
                    }
                }
            }
        }
    }
}

/**
 * Card de receta SIMPLE (versión anterior para compatibilidad)
 */
@Composable
fun RecipeCard(
    name: String,
    cost: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = ChefCoreColors.SurfaceGray, shape = RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Text(text = name, style = MaterialTheme.typography.titleMedium, color = ChefCoreColors.TextDark)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = cost, style = MaterialTheme.typography.bodySmall, color = ChefCoreColors.PrimaryGreen)
    }
}

// ============================================================================
// CARDS DE EMPLEADO
// ============================================================================

/**
 * Tarjeta de empleado
 */
@Composable
fun EmployeeCard(
    name: String,
    role: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color.White, shape = RoundedCornerShape(12.dp))
            .border(width = 1.dp, color = ChefCoreColors.SurfaceGray, shape = RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, style = MaterialTheme.typography.titleMedium, color = ChefCoreColors.TextDark)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = role, style = MaterialTheme.typography.bodySmall, color = ChefCoreColors.TextMedium)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onEdit,
                colors = ButtonDefaults.buttonColors(containerColor = ChefCoreColors.AccentYellow, contentColor = ChefCoreColors.TextDark),
                shape = RoundedCornerShape(8.dp)
            ) { Text("Editar") }
            Button(
                onClick = onDelete,
                colors = ButtonDefaults.buttonColors(containerColor = ChefCoreColors.ErrorRed, contentColor = Color.White),
                shape = RoundedCornerShape(8.dp)
            ) { Text("Borrar") }
        }
    }
}

// ============================================================================
// COMPONENTES PIN
// ============================================================================

/**
 * Indicador de PIN
 */
@Composable
fun PinIndicator(
    pinLength: Int,
    maxLength: Int = 4
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(maxLength) { index ->
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(
                        color = if (index < pinLength) ChefCoreColors.PrimaryGreen else ChefCoreColors.SurfaceGray,
                        shape = RoundedCornerShape(8.dp)
                    )
            )
        }
    }
}

/**
 * Botón numérico para el teclado PIN
 */
@Composable
fun NumericButton(
    number: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .size(80.dp)
            .padding(4.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = ChefCoreColors.TextDark
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(text = number, style = MaterialTheme.typography.headlineMedium)
    }
}
