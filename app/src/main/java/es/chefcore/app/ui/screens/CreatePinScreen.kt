package es.chefcore.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import es.chefcore.app.ui.components.NumericButton
import es.chefcore.app.ui.components.PinIndicator
import es.chefcore.app.ui.components.PrimaryButton
import es.chefcore.app.ui.theme.ChefCoreColors
import es.chefcore.app.viewmodel.AuthViewModel

@Composable
fun CreatePinScreen(viewModel: AuthViewModel) {
    var pin by remember { mutableStateOf("") }
    var pinOriginal by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isConfirmingPin by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Box(
        modifier = Modifier.fillMaxSize().background(ChefCoreColors.BackgroundLight)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Crea tu PIN de acceso",
                style = MaterialTheme.typography.headlineLarge,
                color = ChefCoreColors.TextDark,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (isConfirmingPin) "Confirma tu PIN de 4 dígitos"
                else "Este PIN te permitirá entrar rápidamente a la app todos los días.",
                style = MaterialTheme.typography.bodyMedium,
                color = ChefCoreColors.TextMedium,
                textAlign = TextAlign.Center
            )

            PinIndicator(
                pinLength = if (isConfirmingPin) confirmPin.length else pin.length,
                maxLength = 4
            )

            if (errorMessage.isNotEmpty()) {
                Text(errorMessage, style = MaterialTheme.typography.bodySmall, color = ChefCoreColors.ErrorRed)
            }

            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                for (rowStart in listOf(1, 4, 7)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        repeat(3) { index ->
                            val num = rowStart + index
                            NumericButton(
                                number = num.toString(),
                                onClick = {
                                    errorMessage = ""
                                    if (!isConfirmingPin && pin.length < 4) pin += num.toString()
                                    else if (isConfirmingPin && confirmPin.length < 4) confirmPin += num.toString()
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumericButton("0", onClick = {
                        if (!isConfirmingPin && pin.length < 4) pin += "0"
                        else if (isConfirmingPin && confirmPin.length < 4) confirmPin += "0"
                    }, modifier = Modifier.weight(1f))
                    NumericButton("⌫", onClick = {
                        if (!isConfirmingPin && pin.isNotEmpty()) pin = pin.dropLast(1)
                        else if (isConfirmingPin && confirmPin.isNotEmpty()) confirmPin = confirmPin.dropLast(1)
                    }, modifier = Modifier.weight(1f))
                }
            }

            if (!isConfirmingPin) {
                PrimaryButton("Siguiente", onClick = {
                    if (pin.length == 4) { pinOriginal = pin; pin = ""; isConfirmingPin = true }
                    else errorMessage = "El PIN debe tener 4 dígitos"
                }, enabled = pin.length == 4)
            } else {
                PrimaryButton("Confirmar PIN", onClick = {
                    if (confirmPin.length == 4) {
                        if (pinOriginal == confirmPin) viewModel.guardarPin(pinOriginal)
                        else { errorMessage = "Los PINs no coinciden"; confirmPin = "" }
                    } else errorMessage = "El PIN debe tener 4 dígitos"
                }, enabled = confirmPin.length == 4)
            }
        }
    }
}