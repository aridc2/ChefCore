package es.chefcore.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.chefcore.app.data.database.Usuario
import es.chefcore.app.ui.components.ChefCoreTextField
import es.chefcore.app.ui.components.NumericButton
import es.chefcore.app.ui.components.PinIndicator
import es.chefcore.app.ui.components.PrimaryButton
import es.chefcore.app.ui.theme.ChefCoreColors
import es.chefcore.app.viewmodel.AuthViewModel

/**
 * Pantalla de selección de perfil.
 * Estado 1: Grid de usuarios → elige quién eres
 * Estado 2: Teclado PIN → introduce tu PIN
 */
@Composable
fun SeleccionUsuarioScreen(
    usuarios: List<Usuario>,
    nombreRestaurante: String,
    viewModel: AuthViewModel
) {
    var usuarioSeleccionado by remember { mutableStateOf<Usuario?>(null) }
    var mostrarRecuperarAcceso by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ChefCoreColors.BackgroundLight)
            .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
    ) {
        AnimatedContent(
            targetState = usuarioSeleccionado,
            label = "auth_transition"
        ) { usuario ->
            if (usuario == null) {
                // ── Estado 1: Selección de perfil ─────────────────────────
                PantallaSeleccion(
                    usuarios = usuarios,
                    nombreRestaurante = nombreRestaurante,
                    mostrarRecuperarAcceso = mostrarRecuperarAcceso,
                    viewModel = viewModel,
                    onUsuarioSeleccionado = { usuarioSeleccionado = it },
                    onToggleRecuperarAcceso = { mostrarRecuperarAcceso = !mostrarRecuperarAcceso },
                    focusManager = focusManager
                )
            } else {
                // ── Estado 2: Introducir PIN ───────────────────────────────
                PantallaPin(
                    usuario = usuario,
                    viewModel = viewModel,
                    onVolver = { usuarioSeleccionado = null }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ESTADO 1: Grid de perfiles
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PantallaSeleccion(
    usuarios: List<Usuario>,
    nombreRestaurante: String,
    mostrarRecuperarAcceso: Boolean,
    viewModel: AuthViewModel,
    onUsuarioSeleccionado: (Usuario) -> Unit,
    onToggleRecuperarAcceso: () -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = nombreRestaurante.ifBlank { "ChefCore" },
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = ChefCoreColors.PrimaryGreen,
            textAlign = TextAlign.Center
        )

        Text(
            text = "¿Quién eres?",
            style = MaterialTheme.typography.headlineSmall,
            color = ChefCoreColors.TextDark
        )

        if (usuarios.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No hay usuarios registrados.\nEl gerente debe crear los perfiles desde Ajustes.",
                    color = ChefCoreColors.TextMedium,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(usuarios) { usuario ->
                    TarjetaUsuario(
                        usuario = usuario,
                        onClick = { onUsuarioSeleccionado(usuario) }
                    )
                }
            }
        }

        TextButton(onClick = onToggleRecuperarAcceso) {
            Text(
                text = if (mostrarRecuperarAcceso) "Cancelar" else "Gerente: recuperar acceso con correo",
                color = ChefCoreColors.PrimaryGreen.copy(alpha = 0.75f),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        AnimatedVisibility(visible = mostrarRecuperarAcceso) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Acceso con correo electrónico",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ChefCoreColors.TextDark
                    )

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = ChefCoreColors.ErrorRed,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    ChefCoreTextField(
                        value = email,
                        onValueChange = { email = it; viewModel.clearError() },
                        label = "Correo electrónico",
                        keyboardType = KeyboardType.Email
                    )
                    ChefCoreTextField(
                        value = password,
                        onValueChange = { password = it; viewModel.clearError() },
                        label = "Contraseña",
                        isPassword = true
                    )

                    if (isLoading) {
                        CircularProgressIndicator(
                            color = ChefCoreColors.PrimaryGreen,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    } else {
                        PrimaryButton(
                            text = "Acceder",
                            onClick = {
                                focusManager.clearFocus()
                                viewModel.loginConEmail(email, password)
                            },
                            enabled = email.isNotBlank() && password.isNotBlank()
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Tarjeta de usuario en el grid
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TarjetaUsuario(
    usuario: Usuario,
    onClick: () -> Unit
) {
    val colorRol = ChefCoreColors.PrimaryGreen

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(colorRol.copy(alpha = 0.15f))
                    .border(2.dp, colorRol, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = usuario.nombre.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorRol
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = usuario.nombre,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ChefCoreColors.TextDark,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Badge de rol
            Box(
                modifier = Modifier
                    .background(colorRol.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = usuario.rol,
                    style = MaterialTheme.typography.labelMedium,
                    color = colorRol,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ESTADO 2: Teclado PIN para el usuario seleccionado
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PantallaPin(
    usuario: Usuario,
    viewModel: AuthViewModel,
    onVolver: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    LaunchedEffect(pin) {
        if (pin.length == 4) {
            viewModel.validarPin(pin, listOf(usuario))
        }
    }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null && pin.length == 4) {
            kotlinx.coroutines.delay(500L)
            pin = ""
            viewModel.clearError()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChefCoreColors.PrimaryGreen)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = { pin = ""; viewModel.clearError(); onVolver() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f))
                .border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = usuario.nombre.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Text(
            text = "Hola, ${usuario.nombre}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Text(
            text = "Introduce tu PIN",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.8f)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(
                            if (index < pin.length) Color.White
                            else Color.White.copy(alpha = 0.35f)
                        )
                )
            }
        }

        AnimatedVisibility(visible = errorMessage != null) {
            Text(
                text = errorMessage ?: "",
                color = ChefCoreColors.AccentYellow,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            for (rowStart in listOf(1, 4, 7)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    repeat(3) { index ->
                        val num = rowStart + index
                        PinButton(
                            label = num.toString(),
                            onClick = { if (pin.length < 4) pin += num.toString() },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(modifier = Modifier.weight(1f))
                PinButton("0", onClick = { if (pin.length < 4) pin += "0" }, modifier = Modifier.weight(1f))
                PinButton("⌫", onClick = { if (pin.isNotEmpty()) pin = pin.dropLast(1) }, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PinButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.2f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}