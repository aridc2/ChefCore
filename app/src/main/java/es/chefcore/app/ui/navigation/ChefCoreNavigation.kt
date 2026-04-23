package es.chefcore.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import es.chefcore.app.data.database.AlbaranDao
import es.chefcore.app.data.database.IngredienteDao
import es.chefcore.app.data.database.RecetaDao
import es.chefcore.app.data.database.UsuarioDao
import es.chefcore.app.logic.CocinaManager
import es.chefcore.app.ui.screens.CreatePinScreen
import es.chefcore.app.ui.screens.InventoryScreen
import es.chefcore.app.ui.screens.PersonalManagementScreen
import es.chefcore.app.ui.screens.RecipesScreen
import es.chefcore.app.ui.screens.RegisterRestaurantScreen

sealed class ChefCoreRoute {
    object Register : ChefCoreRoute()
    object Login : ChefCoreRoute()
    object CreatePin : ChefCoreRoute()
    object Inventory : ChefCoreRoute()
    object PersonalManagement : ChefCoreRoute()
    object Recipes : ChefCoreRoute()
    object Settings : ChefCoreRoute()
    object Scanner : ChefCoreRoute()
}

@Composable
fun ChefCoreNavigation(
    iDao: IngredienteDao,
    rDao: RecetaDao,
    aDao: AlbaranDao,
    uDao: UsuarioDao,
    cocinaManager: CocinaManager
) {
    var currentRoute by remember { mutableStateOf<ChefCoreRoute>(ChefCoreRoute.Register) }

    when (currentRoute) {

        is ChefCoreRoute.Register -> {
            RegisterRestaurantScreen(
                onRegisterSuccess = { currentRoute = ChefCoreRoute.CreatePin },
                onNavigateToLogin = { currentRoute = ChefCoreRoute.Login }
            )
        }

        is ChefCoreRoute.Login -> {
            RegisterRestaurantScreen(
                onRegisterSuccess = { currentRoute = ChefCoreRoute.CreatePin },
                onNavigateToLogin = { }
            )
        }

        is ChefCoreRoute.CreatePin -> {
            CreatePinScreen(
                onPinCreated = { currentRoute = ChefCoreRoute.Inventory }
            )
        }

        is ChefCoreRoute.Inventory -> {
            InventoryScreen(
                iDao = iDao,
                cocinaManager = cocinaManager,
                onSettingsClick = { currentRoute = ChefCoreRoute.Settings },
                onRecipesClick = { currentRoute = ChefCoreRoute.Recipes },
                onPersonalClick = { currentRoute = ChefCoreRoute.PersonalManagement },
                onScannerClick = { currentRoute = ChefCoreRoute.Scanner }
            )
        }

        is ChefCoreRoute.PersonalManagement -> {
            PersonalManagementScreen(
                uDao = uDao,
                onSettingsClick = { currentRoute = ChefCoreRoute.Settings },
                onInventoryClick = { currentRoute = ChefCoreRoute.Inventory },
                onRecipesClick = { currentRoute = ChefCoreRoute.Recipes },
                onScannerClick = { currentRoute = ChefCoreRoute.Scanner }
            )
        }

        is ChefCoreRoute.Recipes -> {
            RecipesScreen(
                rDao = rDao,
                iDao = iDao,
                cocinaManager = cocinaManager,
                onSettingsClick = { currentRoute = ChefCoreRoute.Settings },
                onInventoryClick = { currentRoute = ChefCoreRoute.Inventory },
                onPersonalClick = { currentRoute = ChefCoreRoute.PersonalManagement },
                onScannerClick = { currentRoute = ChefCoreRoute.Scanner }
            )
        }

        is ChefCoreRoute.Settings -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Ajustes — Próximamente", style = MaterialTheme.typography.headlineMedium)
            }
        }

        is ChefCoreRoute.Scanner -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Escáner — Próximamente", style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
}
