package es.chefcore.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import es.chefcore.app.ui.components.NumericButton
import es.chefcore.app.ui.components.PinIndicator
import es.chefcore.app.ui.components.PrimaryButton
import es.chefcore.app.ui.theme.ChefCoreColors

@Composable
fun CreatePinScreen(
    onPinCreated: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var pinOriginal by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isConfirmingPin by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = ChefCoreColors.BackgroundLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
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
                else "Este PIN de 4 dígitos te permitirá entrar rápidamente a la app todos los días.",
                style = MaterialTheme.typography.bodyMedium,
                color = ChefCoreColors.TextMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            PinIndicator(
                pinLength = if (isConfirmingPin) confirmPin.length else pin.length,
                maxLength = 4
            )

            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = ChefCoreColors.ErrorRed,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Teclado numérico
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Filas 1-2-3, 4-5-6, 7-8-9
                for (rowStart in listOf(1, 4, 7)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        repeat(3) { index ->
                            val num = rowStart + index
                            NumericButton(
                                number = num.toString(),
                                onClick = {
                                    if (!isConfirmingPin && pin.length < 4) {
                                        pin += num.toString()
                                    } else if (isConfirmingPin && confirmPin.length < 4) {
                                        confirmPin += num.toString()
                                    }
                                    errorMessage = ""
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Fila 0 + borrar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NumericButton(
                        number = "0",
                        onClick = {
                            if (!isConfirmingPin && pin.length < 4) pin += "0"
                            else if (isConfirmingPin && confirmPin.length < 4) confirmPin += "0"
                            errorMessage = ""
                        },
                        modifier = Modifier.weight(1f)
                    )
                    NumericButton(
                        number = "⌫",
                        onClick = {
                            if (!isConfirmingPin && pin.isNotEmpty()) pin = pin.dropLast(1)
                            else if (isConfirmingPin && confirmPin.isNotEmpty()) confirmPin = confirmPin.dropLast(1)
                            errorMessage = ""
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (!isConfirmingPin) {
                PrimaryButton(
                    text = "Siguiente",
                    onClick = {
                        if (pin.length == 4) {
                            pinOriginal = pin
                            pin = ""
                            isConfirmingPin = true
                        } else {
                            errorMessage = "El PIN debe tener 4 dígitos"
                        }
                    },
                    enabled = pin.length == 4
                )
            } else {
                PrimaryButton(
                    text = "Confirmar PIN",
                    onClick = {
                        if (confirmPin.length == 4) {
                            if (pinOriginal == confirmPin) {
                                onPinCreated(pinOriginal)
                            } else {
                                errorMessage = "Los PINs no coinciden"
                                confirmPin = ""
                            }
                        } else {
                            errorMessage = "El PIN debe tener 4 dígitos"
                        }
                    },
                    enabled = confirmPin.length == 4
                )
            }
        }
    }
}
