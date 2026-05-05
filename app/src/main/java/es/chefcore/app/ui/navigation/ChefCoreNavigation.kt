package es.chefcore.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import es.chefcore.app.ui.screens.*
import es.chefcore.app.ui.theme.ChefCoreColors
import es.chefcore.app.viewmodel.AuthUiState
import es.chefcore.app.viewmodel.AuthViewModel
import es.chefcore.app.viewmodel.PersonalManagementViewModel
import es.chefcore.app.viewmodel.RecipesViewModel

sealed class ChefCoreRoute {
    object Inventory      : ChefCoreRoute()
    object PersonalManagement : ChefCoreRoute()
    object Recipes        : ChefCoreRoute()
    object Settings       : ChefCoreRoute()
    object Scanner        : ChefCoreRoute()
    object RecipeCreation : ChefCoreRoute()
    data class RecipeEdit(val recetaId: Int) : ChefCoreRoute()
}

@Composable
fun ChefCoreNavigation() {
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    var currentRoute by remember { mutableStateOf<ChefCoreRoute>(ChefCoreRoute.Inventory) }

    when (authState) {

        is AuthUiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ChefCoreColors.PrimaryGreen)
            }
        }

        is AuthUiState.NeedRegister -> {
            val personalVM: PersonalManagementViewModel = viewModel()
            val usuarios by personalVM.usuarios.collectAsStateWithLifecycle()
            PantallaAcceso(
                viewModel = authViewModel,
                usuarios = usuarios,
                nombreRestaurante = authViewModel.getNombreRestaurante(),
                modoInicial = 1
            )
        }

        is AuthUiState.NeedCreatePin -> {
            CreatePinScreen(viewModel = authViewModel)
        }

        is AuthUiState.NeedPinLogin -> {
            val personalVM: PersonalManagementViewModel = viewModel()
            val usuarios by personalVM.usuarios.collectAsStateWithLifecycle()
            SeleccionUsuarioScreen(
                usuarios = usuarios,
                nombreRestaurante = authViewModel.getNombreRestaurante(),
                viewModel = authViewModel
            )
        }

        is AuthUiState.LoggedIn -> {
            val esGerente = (authState as AuthUiState.LoggedIn).rol == "Gerente"

            when (currentRoute) {

                is ChefCoreRoute.Inventory -> InventoryScreen(
                    esGerente         = esGerente,
                    onSettingsClick   = { currentRoute = ChefCoreRoute.Settings },
                    onRecipesClick    = { currentRoute = ChefCoreRoute.Recipes },
                    onPersonalClick   = { if (esGerente) currentRoute = ChefCoreRoute.PersonalManagement },
                    onScannerClick    = { currentRoute = ChefCoreRoute.Scanner }
                )

                is ChefCoreRoute.PersonalManagement -> {
                    if (esGerente) {
                        PersonalManagementScreen(
                            onSettingsClick   = { currentRoute = ChefCoreRoute.Settings },
                            onInventoryClick  = { currentRoute = ChefCoreRoute.Inventory },
                            onRecipesClick    = { currentRoute = ChefCoreRoute.Recipes },
                            onScannerClick    = { currentRoute = ChefCoreRoute.Scanner }
                        )
                    } else {
                        currentRoute = ChefCoreRoute.Inventory
                    }
                }

                is ChefCoreRoute.Recipes -> RecipesScreen(
                    esGerente         = esGerente,
                    onSettingsClick   = { currentRoute = ChefCoreRoute.Settings },
                    onInventoryClick  = { currentRoute = ChefCoreRoute.Inventory },
                    onPersonalClick   = { if (esGerente) currentRoute = ChefCoreRoute.PersonalManagement },
                    onScannerClick    = { currentRoute = ChefCoreRoute.Scanner },
                    onNavigateToCreate = { currentRoute = ChefCoreRoute.RecipeCreation },
                    onNavigateToEdit   = { id -> currentRoute = ChefCoreRoute.RecipeEdit(id) }
                )

                is ChefCoreRoute.Settings -> SettingsScreen(
                    esGerente         = esGerente,
                    onInventoryClick  = { currentRoute = ChefCoreRoute.Inventory },
                    onRecipesClick    = { currentRoute = ChefCoreRoute.Recipes },
                    onPersonalClick   = { if (esGerente) currentRoute = ChefCoreRoute.PersonalManagement },
                    onScannerClick    = { currentRoute = ChefCoreRoute.Scanner },
                    onLogout          = { authViewModel.cerrarSesion() }
                )

                is ChefCoreRoute.Scanner -> EscanerScreen(
                    onSettingsClick   = { currentRoute = ChefCoreRoute.Settings },
                    onInventoryClick  = { currentRoute = ChefCoreRoute.Inventory },
                    onRecipesClick    = { currentRoute = ChefCoreRoute.Recipes },
                    onPersonalClick   = { if (esGerente) currentRoute = ChefCoreRoute.PersonalManagement }
                )

                is ChefCoreRoute.RecipeCreation -> {
                    val vm: RecipesViewModel = viewModel()
                    RecipeCreationScreen(
                        viewModel   = vm,
                        onSaveRecipe = { name, cost, tiempo, uri, inst, ings ->
                            vm.crearReceta(name, cost, tiempo, inst, uri?.toString(), ings)
                            currentRoute = ChefCoreRoute.Recipes
                        },
                        onCancel        = { currentRoute = ChefCoreRoute.Recipes },
                        onSettingsClick = { currentRoute = ChefCoreRoute.Settings },
                        onInventoryClick= { currentRoute = ChefCoreRoute.Inventory },
                        onRecipesClick  = { currentRoute = ChefCoreRoute.Recipes },
                        onPersonalClick = { if (esGerente) currentRoute = ChefCoreRoute.PersonalManagement },
                        onScannerClick  = { currentRoute = ChefCoreRoute.Scanner }
                    )
                }

                is ChefCoreRoute.RecipeEdit -> {
                    val vm: RecipesViewModel = viewModel()
                    val recetaId = (currentRoute as ChefCoreRoute.RecipeEdit).recetaId
                    RecipeCreationScreen(
                        viewModel   = vm,
                        recetaId    = recetaId,
                        onSaveRecipe = { name, cost, tiempo, uri, inst, ings ->
                            vm.actualizarReceta(recetaId, name, cost, tiempo, inst, uri?.toString(), ings)
                            currentRoute = ChefCoreRoute.Recipes
                        },
                        onCancel        = { currentRoute = ChefCoreRoute.Recipes },
                        onSettingsClick = { currentRoute = ChefCoreRoute.Settings },
                        onInventoryClick= { currentRoute = ChefCoreRoute.Inventory },
                        onRecipesClick  = { currentRoute = ChefCoreRoute.Recipes },
                        onPersonalClick = { if (esGerente) currentRoute = ChefCoreRoute.PersonalManagement },
                        onScannerClick  = { currentRoute = ChefCoreRoute.Scanner }
                    )
                }
            }
        }
    }
}