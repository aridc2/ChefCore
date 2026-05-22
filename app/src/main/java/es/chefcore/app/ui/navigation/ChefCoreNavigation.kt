package es.chefcore.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import es.chefcore.app.ui.screens.*
import es.chefcore.app.ui.theme.ChefCoreColors
import es.chefcore.app.viewmodel.AuthUiState
import es.chefcore.app.viewmodel.AuthViewModel
import es.chefcore.app.viewmodel.EstadoOcr
import es.chefcore.app.viewmodel.OcrViewModel
import es.chefcore.app.viewmodel.PersonalManagementViewModel
import es.chefcore.app.viewmodel.RecipesViewModel
import es.chefcore.app.viewmodel.VoiceViewModel

sealed class ChefCoreRoute {
    object Inventory          : ChefCoreRoute()
    object PersonalManagement : ChefCoreRoute()
    object Recipes            : ChefCoreRoute()
    object Settings           : ChefCoreRoute()
    object Scanner            : ChefCoreRoute()
    object Albaranes          : ChefCoreRoute()
    object RecipeCreation     : ChefCoreRoute()
    data class RecipeCreationVoz(val nombreInicial: String) : ChefCoreRoute()
    object OcrValidacion      : ChefCoreRoute()
    data class RecipeEdit(val recetaId: Int) : ChefCoreRoute()
}

/** Serializa ChefCoreRoute a String para sobrevivir rotación y muerte de proceso. */
private val RouteSaver = Saver<ChefCoreRoute, String>(
    save = { route ->
        when (route) {
            is ChefCoreRoute.Inventory          -> "Inventory"
            is ChefCoreRoute.Recipes            -> "Recipes"
            is ChefCoreRoute.Settings           -> "Settings"
            is ChefCoreRoute.Scanner            -> "Scanner"
            is ChefCoreRoute.Albaranes          -> "Albaranes"
            is ChefCoreRoute.PersonalManagement -> "PersonalManagement"
            is ChefCoreRoute.RecipeCreation     -> "RecipeCreation"
            is ChefCoreRoute.RecipeCreationVoz -> "RecipeCreationVoz:${route.nombreInicial}"
            is ChefCoreRoute.OcrValidacion      -> "Scanner"  // OCR state en ViewModel
            is ChefCoreRoute.RecipeEdit         -> "RecipeEdit:${route.recetaId}"
        }
    },
    restore = { s ->
        when {
            s == "Recipes"            -> ChefCoreRoute.Recipes
            s == "Settings"           -> ChefCoreRoute.Settings
            s == "Scanner"            -> ChefCoreRoute.Scanner
            s == "Albaranes"          -> ChefCoreRoute.Albaranes
            s == "PersonalManagement" -> ChefCoreRoute.PersonalManagement
            s == "RecipeCreation"     -> ChefCoreRoute.RecipeCreation
            s.startsWith("RecipeCreationVoz:") -> ChefCoreRoute.RecipeCreationVoz(s.substringAfter(":"))
            s.startsWith("RecipeEdit:") -> ChefCoreRoute.RecipeEdit(
                s.substringAfter(":").toIntOrNull() ?: 0
            )
            else -> ChefCoreRoute.Inventory
        }
    }
)

@Composable
fun ChefCoreNavigation() {
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()

    // rememberSaveable: sobrevive tanto rotación como muerte de proceso
    var currentRoute by rememberSaveable(stateSaver = RouteSaver) {
        mutableStateOf<ChefCoreRoute>(ChefCoreRoute.Inventory)
    }

    // Resetear a Inventario en cada nuevo login
    var prevAuthState by remember { mutableStateOf<AuthUiState>(AuthUiState.Loading) }
    LaunchedEffect(authState) {
        if (authState is AuthUiState.LoggedIn && prevAuthState !is AuthUiState.LoggedIn) {
            currentRoute = ChefCoreRoute.Inventory
        }
        prevAuthState = authState
    }

    when (authState) {

        // Comprobando estado inicial — spinner
        is AuthUiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ChefCoreColors.PrimaryGreen)
            }
        }

        // Sin cuenta → mostrar tab de Registro
        is AuthUiState.NeedRegister -> {
            val personalVM: PersonalManagementViewModel = viewModel()
            val usuarios by personalVM.usuarios.collectAsStateWithLifecycle()
            PantallaAcceso(
                viewModel = authViewModel,
                usuarios = usuarios,
                nombreRestaurante = authViewModel.getNombreRestaurante(),
                modoInicial = 1 // Abre en tab Registro
            )
        }

        // Registrado en Firebase pero sin PIN → Crear PIN
        is AuthUiState.NeedCreatePin -> {
            CreatePinScreen(viewModel = authViewModel)
        }

        // Tiene cuenta y PIN → Selección de perfil
        is AuthUiState.NeedPinLogin -> {
            val personalVM: PersonalManagementViewModel = viewModel()
            val usuarios by personalVM.usuarios.collectAsStateWithLifecycle()
            SeleccionUsuarioScreen(
                usuarios = usuarios,
                nombreRestaurante = authViewModel.getNombreRestaurante(),
                viewModel = authViewModel
            )
        }

        // Autenticado → Mostrar la pantalla actual de la app
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
                    onNavigateToCreate    = { currentRoute = ChefCoreRoute.RecipeCreation },
                    onNavigateToCreateVoz = { nombre -> currentRoute = ChefCoreRoute.RecipeCreationVoz(nombre) },
                    onNavigateToEdit   = { id -> currentRoute = ChefCoreRoute.RecipeEdit(id) }
                )

                is ChefCoreRoute.Settings -> {
                    val pinChangeResult by authViewModel.pinChangeResult.collectAsStateWithLifecycle()
                    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
                    LaunchedEffect(pinChangeResult) {
                        pinChangeResult?.let {
                            val msg = it.substringAfter(":")
                            snackbarHostState.showSnackbar(msg)
                            authViewModel.clearPinChangeResult()
                        }
                    }
                    SettingsScreen(
                        esGerente         = esGerente,
                        onInventoryClick  = { currentRoute = ChefCoreRoute.Inventory },
                        onRecipesClick    = { currentRoute = ChefCoreRoute.Recipes },
                        onPersonalClick   = { if (esGerente) currentRoute = ChefCoreRoute.PersonalManagement },
                        onScannerClick    = { currentRoute = ChefCoreRoute.Scanner },
                        onLogout          = { authViewModel.cerrarSesion() },
                        onCambiarPin      = { pinActual, pinNuevo ->
                            authViewModel.cambiarPin(pinActual, pinNuevo)
                        },
                        onCheckCurrentPin = { authViewModel.checkCurrentPin(it) }
                    )
                }

                is ChefCoreRoute.Scanner -> {
                    val ocrVM: OcrViewModel = viewModel()
                    EscanerScreen(
                        esGerente         = esGerente,
                        onSettingsClick   = { currentRoute = ChefCoreRoute.Settings },
                        onInventoryClick  = { currentRoute = ChefCoreRoute.Inventory },
                        onRecipesClick    = { currentRoute = ChefCoreRoute.Recipes },
                        onPersonalClick   = { if (esGerente) currentRoute = ChefCoreRoute.PersonalManagement },
                        onAlbaranesClick  = { currentRoute = ChefCoreRoute.Albaranes },
                        onResultadoListo  = { currentRoute = ChefCoreRoute.OcrValidacion },
                        ocrViewModel      = ocrVM
                    )
                }

                is ChefCoreRoute.OcrValidacion -> {
                    val ocrVM: OcrViewModel = viewModel()
                    val estado by ocrVM.estado.collectAsStateWithLifecycle()

                    LaunchedEffect(estado) {
                        if (estado is EstadoOcr.Guardado) {
                            ocrVM.resetear()
                            currentRoute = ChefCoreRoute.Albaranes
                        }
                    }

                    val resultado = (estado as? EstadoOcr.PaginaLista)?.resultado
                    if (resultado != null) {
                        OcrValidacionScreen(
                            resultado  = resultado,
                            onGuardar  = { proveedor, fecha, total, items ->
                                ocrVM.guardarAlbaranConItems(
                                    proveedor, fecha, total,
                                    authViewModel.getCurrentUserId(),
                                    items
                                )
                            },
                            onCancelar = {
                                currentRoute = ChefCoreRoute.Scanner
                            }
                        )
                    } else {
                        LaunchedEffect(Unit) { currentRoute = ChefCoreRoute.Scanner }
                    }
                }

                is ChefCoreRoute.Albaranes -> AlbaranListScreen(
                    esGerente         = esGerente,
                    onSettingsClick   = { currentRoute = ChefCoreRoute.Settings },
                    onInventoryClick  = { currentRoute = ChefCoreRoute.Inventory },
                    onRecipesClick    = { currentRoute = ChefCoreRoute.Recipes },
                    onScannerClick    = { currentRoute = ChefCoreRoute.Scanner },
                    onPersonalClick   = { if (esGerente) currentRoute = ChefCoreRoute.PersonalManagement }
                )

                is ChefCoreRoute.RecipeCreationVoz -> {
                    val vm: RecipesViewModel = viewModel()
                    val nombreInicial = (currentRoute as ChefCoreRoute.RecipeCreationVoz).nombreInicial
                    RecipeCreationScreen(
                        viewModel   = vm,
                        nombreInicial = nombreInicial,
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

                is ChefCoreRoute.RecipeCreationVoz -> {
                    val vm: RecipesViewModel = viewModel()
                    val nombreInicial = (currentRoute as ChefCoreRoute.RecipeCreationVoz).nombreInicial
                    RecipeCreationScreen(
                        viewModel     = vm,
                        nombreInicial = nombreInicial,
                        onSaveRecipe  = { name, cost, tiempo, uri, inst, ings ->
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