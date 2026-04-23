package es.chefcore.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import es.chefcore.app.R
import es.chefcore.app.ui.components.ChefCoreTextField
import es.chefcore.app.ui.components.PrimaryButton
import es.chefcore.app.ui.theme.ChefCoreColors

@Composable
fun RegisterRestaurantScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(1) }
    var restaurantName by remember { mutableStateOf("") }
    var managerEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = ChefCoreColors.BackgroundLight)
            .imePadding() // <--- AÑADIDO: Ajusta el contenido cuando aparece el teclado
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = ChefCoreColors.PrimaryGreen,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        onNavigateToLogin()
                    },
                    text = { Text("Iniciar Sesión", style = MaterialTheme.typography.titleMedium) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Registrar Restaurante", style = MaterialTheme.typography.titleMedium) }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(color = ChefCoreColors.SurfaceGray, shape = RoundedCornerShape(60.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_chef_logo),
                        contentDescription = "ChefCore Logo",
                        modifier = Modifier.size(80.dp),
                        tint = ChefCoreColors.TextDark
                    )
                }

                Text(
                    text = "ChefCore",
                    style = MaterialTheme.typography.displayLarge,
                    color = ChefCoreColors.AccentYellow
                )

                Spacer(modifier = Modifier.height(24.dp))

                ChefCoreTextField(
                    value = restaurantName,
                    onValueChange = { restaurantName = it },
                    label = "Nombre del restaurante"
                )

                ChefCoreTextField(
                    value = managerEmail,
                    onValueChange = { managerEmail = it },
                    label = "Correo electrónico del gerente",
                    keyboardType = KeyboardType.Email
                )

                ChefCoreTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Contraseña",
                    isPassword = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                PrimaryButton(
                    text = "Registrar y Continuar",
                    onClick = {
                        isLoading = true
                        if (restaurantName.isNotEmpty() && managerEmail.isNotEmpty() && password.isNotEmpty()) {
                            onRegisterSuccess()
                        }
                        isLoading = false
                    },
                    enabled = !isLoading && restaurantName.isNotEmpty() && managerEmail.isNotEmpty() && password.isNotEmpty()
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
