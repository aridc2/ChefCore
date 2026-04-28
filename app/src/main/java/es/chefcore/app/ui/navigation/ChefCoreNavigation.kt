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
import androidx.lifecycle.viewmodel.compose.viewModel
import es.chefcore.app.ui.screens.*
import es.chefcore.app.viewmodel.RecipesViewModel

sealed class ChefCoreRoute {
    object Register : ChefCoreRoute()
    object Login : ChefCoreRoute()
    object CreatePin : ChefCoreRoute()
    object Inventory : ChefCoreRoute()
    object PersonalManagement : ChefCoreRoute()
    object Recipes : ChefCoreRoute()
    object Settings : ChefCoreRoute()
    object Scanner : ChefCoreRoute()
    object RecipeCreation : ChefCoreRoute()
    // data class RecipeEdit(val recetaId: Int) : ChefCoreRoute() // ⏳ DESACTIVADO temporalmente
}

@Composable
fun ChefCoreNavigation() {
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
            CreatePinScreen(onPinCreated = { currentRoute = ChefCoreRoute.Inventory })
        }

        is ChefCoreRoute.Inventory -> {
            InventoryScreen(
                onSettingsClick = { currentRoute = ChefCoreRoute.Settings },
                onRecipesClick = { currentRoute = ChefCoreRoute.Recipes },
                onPersonalClick = { currentRoute = ChefCoreRoute.PersonalManagement },
                onScannerClick = { currentRoute = ChefCoreRoute.Scanner }
            )
        }

        is ChefCoreRoute.PersonalManagement -> {
            PersonalManagementScreen(
                onSettingsClick = { currentRoute = ChefCoreRoute.Settings },
                onInventoryClick = { currentRoute = ChefCoreRoute.Inventory },
                onRecipesClick = { currentRoute = ChefCoreRoute.Recipes },
                onScannerClick = { currentRoute = ChefCoreRoute.Scanner }
            )
        }

        is ChefCoreRoute.Recipes -> {
            RecipesScreen(
                onSettingsClick = { currentRoute = ChefCoreRoute.Settings },
                onInventoryClick = { currentRoute = ChefCoreRoute.Inventory },
                onPersonalClick = { currentRoute = ChefCoreRoute.PersonalManagement },
                onScannerClick = { currentRoute = ChefCoreRoute.Scanner },
                onNavigateToCreate = { currentRoute = ChefCoreRoute.RecipeCreation }
                // onEditarReceta REMOVIDO temporalmente - se añadirá en Fase 2
            )
        }

        is ChefCoreRoute.Settings -> {
            SettingsScreen(
                onInventoryClick = { currentRoute = ChefCoreRoute.Inventory },
                onRecipesClick = { currentRoute = ChefCoreRoute.Recipes },
                onPersonalClick = { currentRoute = ChefCoreRoute.PersonalManagement },
                onScannerClick = { currentRoute = ChefCoreRoute.Scanner }
            )
        }

        is ChefCoreRoute.Scanner -> {
            EscanerScreen(
                onSettingsClick = { currentRoute = ChefCoreRoute.Settings },
                onInventoryClick = { currentRoute = ChefCoreRoute.Inventory },
                onRecipesClick = { currentRoute = ChefCoreRoute.Recipes },
                onPersonalClick = { currentRoute = ChefCoreRoute.PersonalManagement }
            )
        }

        is ChefCoreRoute.RecipeCreation -> {
            val recipesViewModel: RecipesViewModel = viewModel()

            RecipeCreationScreen(
                viewModel = recipesViewModel,
                onSaveRecipe = { name, cost, imageUri, instructions, ingredientesLista ->
                    recipesViewModel.crearReceta(
                        nombre = name,
                        precioVenta = cost,
                        instrucciones = instructions,
                        imagenUri = imageUri?.toString(),
                        ingredientes = ingredientesLista
                    )
                    currentRoute = ChefCoreRoute.Recipes
                },
                onCancel = { currentRoute = ChefCoreRoute.Recipes },
                onSettingsClick = { currentRoute = ChefCoreRoute.Settings },
                onInventoryClick = { currentRoute = ChefCoreRoute.Inventory },
                onRecipesClick = { currentRoute = ChefCoreRoute.Recipes },
                onPersonalClick = { currentRoute = ChefCoreRoute.PersonalManagement },
                onScannerClick = { currentRoute = ChefCoreRoute.Scanner }
            )
        }

        //  CASO RecipeEdit DESACTIVADO temporalmente
        // Se activará en Fase 2 cuando RecipeCreationScreen soporte modo edición
        /*
        is ChefCoreRoute.RecipeEdit -> {
            val recipesViewModel: RecipesViewModel = viewModel()

            RecipeCreationScreen(
                viewModel = recipesViewModel,
                recetaId = (currentRoute as ChefCoreRoute.RecipeEdit).recetaId,
                onSaveRecipe = { ... },
                ...
            )
        }
        */
    }
}