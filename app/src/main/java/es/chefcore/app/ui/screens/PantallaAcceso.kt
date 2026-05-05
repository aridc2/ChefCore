package es.chefcore.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.chefcore.app.R
import es.chefcore.app.data.database.Usuario
import es.chefcore.app.ui.components.ChefCoreTextField
import es.chefcore.app.ui.components.NumericButton
import es.chefcore.app.ui.components.PinIndicator
import es.chefcore.app.ui.components.PrimaryButton
import es.chefcore.app.ui.theme.ChefCoreColors
import es.chefcore.app.viewmodel.AuthViewModel

/**
 * Pantalla unificada de acceso con dos tabs:
 * - Tab 0: Iniciar sesión (PIN numérico + fallback email)
 * - Tab 1: Registrar restaurante (primera vez)
 *
 * @param modoInicial 0 = Login, 1 = Registro
 */
@Composable
fun PantallaAcceso(
    viewModel: AuthViewModel,
    usuarios: List<Usuario>,
    nombreRestaurante: String,
    modoInicial: Int = 0
) {
    var tabSeleccionada by remember { mutableIntStateOf(modoInicial) }
    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ChefCoreColors.BackgroundLight)
            .imePadding()
            .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Tabs ─────────────────────────────────────────────────────────
            TabRow(
                selectedTabIndex = tabSeleccionada,
                containerColor = Color.White,
                contentColor = ChefCoreColors.PrimaryGreen
            ) {
                Tab(
                    selected = tabSeleccionada == 0,
                    onClick = { tabSeleccionada = 0; focusManager.clearFocus() },
                    modifier = Modifier.height(64.dp)
                ) {
                    Text(
                        "Iniciar Sesión",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (tabSeleccionada == 0) FontWeight.Bold else FontWeight.Normal
                    )
                }
                Tab(
                    selected = tabSeleccionada == 1,
                    onClick = { tabSeleccionada = 1; focusManager.clearFocus() },
                    modifier = Modifier.height(64.dp)
                ) {
                    Text(
                        "Registrar Restaurante",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (tabSeleccionada == 1) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            // ── Contenido según tab ───────────────────────────────────────────
            when (tabSeleccionada) {
                0 -> ContenidoLogin(
                    viewModel = viewModel,
                    usuarios = usuarios,
                    nombreRestaurante = nombreRestaurante
                )
                1 -> ContenidoRegistro(viewModel = viewModel)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB 0 — LOGIN CON PIN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ContenidoLogin(
    viewModel: AuthViewModel,
    usuarios: List<Usuario>,
    nombreRestaurante: String
) {
    var pin by remember { mutableStateOf("") }
    var mostrarEmailLogin by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    LaunchedEffect(pin) {
        if (pin.length == 4) viewModel.validarPin(pin, usuarios)
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
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = nombreRestaurante.ifBlank { "ChefCore" },
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = ChefCoreColors.AccentYellow,
            textAlign = TextAlign.Center
        )

        Text(
            "Introduce tu PIN",
            style = MaterialTheme.typography.titleLarge,
            color = ChefCoreColors.TextDark,
            textAlign = TextAlign.Center
        )

        PinIndicator(pinLength = pin.length, maxLength = 4)

        AnimatedVisibility(visible = errorMessage != null) {
            Text(
                text = errorMessage ?: "",
                color = ChefCoreColors.ErrorRed,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }

        // Teclado numérico
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
                        NumericButton(
                            number = num.toString(),
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
                NumericButton("0", onClick = { if (pin.length < 4) pin += "0" }, modifier = Modifier.weight(1f))
                NumericButton("⌫", onClick = { if (pin.isNotEmpty()) pin = pin.dropLast(1) }, modifier = Modifier.weight(1f))
            }
        }

        // Fallback email
        TextButton(onClick = { mostrarEmailLogin = !mostrarEmailLogin }) {
            Text(
                text = if (mostrarEmailLogin) "Volver al PIN" else "¿Olvidaste el PIN? Entra con correo",
                color = ChefCoreColors.TextMedium,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        AnimatedVisibility(visible = mostrarEmailLogin) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HorizontalDivider(color = ChefCoreColors.SurfaceGray)
                ChefCoreTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Correo electrónico",
                    keyboardType = KeyboardType.Email
                )
                ChefCoreTextField(
                    value = password,
                    onValueChange = { password = it },
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
                        text = "Iniciar sesión",
                        onClick = { focusManager.clearFocus(); viewModel.loginConEmail(email, password) },
                        enabled = email.isNotBlank() && password.isNotBlank()
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB 1 — REGISTRO DE RESTAURANTE
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ContenidoRegistro(viewModel: AuthViewModel) {
    var restaurantName by remember { mutableStateOf("") }
    var managerEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(100.dp)
                .background(ChefCoreColors.SurfaceGray, RoundedCornerShape(50.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_chef_logo),
                contentDescription = "ChefCore Logo",
                modifier = Modifier.size(64.dp),
                tint = ChefCoreColors.TextDark
            )
        }

        Text("ChefCore", style = MaterialTheme.typography.displayLarge, color = ChefCoreColors.AccentYellow)

        Spacer(modifier = Modifier.height(8.dp))

        ChefCoreTextField(
            value = restaurantName,
            onValueChange = { restaurantName = it; viewModel.clearError() },
            label = "Nombre del restaurante"
        )
        ChefCoreTextField(
            value = managerEmail,
            onValueChange = { managerEmail = it; viewModel.clearError() },
            label = "Correo electrónico del gerente",
            keyboardType = KeyboardType.Email
        )
        ChefCoreTextField(
            value = password,
            onValueChange = { password = it; viewModel.clearError() },
            label = "Contraseña (mínimo 6 caracteres)",
            isPassword = true
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = ChefCoreColors.ErrorRed,
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (isLoading) {
            CircularProgressIndicator(color = ChefCoreColors.PrimaryGreen)
        } else {
            PrimaryButton(
                text = "Registrar y Continuar",
                onClick = {
                    focusManager.clearFocus()
                    viewModel.registrarRestaurante(restaurantName, managerEmail, password)
                },
                enabled = restaurantName.isNotBlank() && managerEmail.isNotBlank() && password.isNotBlank()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}