package es.chefcore.app.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import es.chefcore.app.data.database.*
import es.chefcore.app.logic.CocinaManager
import es.chefcore.app.ui.screens.*

// 1. DEFINIMOS LAS RUTAS CON LOS ICONOS DEL FIGMA
sealed class Ruta(val ruta: String, val titulo: String, val icono: @Composable () -> Unit) {
    object Personal : Ruta("personal", "Ajustes", { Icon(Icons.Filled.People, contentDescription = "Personal") })
    object Inventario : Ruta("inventario", "Inventario", { Icon(Icons.Filled.Inventory, contentDescription = "Inventario") })
    object Recetas : Ruta("recetas", "Recetas", { Icon(Icons.Filled.Book, contentDescription = "Recetas") })
    object Escaner : Ruta("escaner", "Escáner", { Icon(Icons.Filled.QrCodeScanner, contentDescription = "Escáner") })

    // Rutas ocultas de la barra
    object DetalleReceta : Ruta("receta_detalle/{nombre}", "Detalle", { }) {
        fun crearRuta(nombre: String) = "receta_detalle/$nombre"
    }
}

@Composable
fun ChefCoreAppNavigation(
    iDao: IngredienteDao,
    rDao: RecetaDao,
    aDao: AlbaranDao,
    uDao: UsuarioDao,
    cocinaManager: CocinaManager,
    textoVoz: String
) {
    val navController = rememberNavController()
    // Orden de los botones según tu Figma
    val items = listOf(Ruta.Personal, Ruta.Inventario, Ruta.Recetas, Ruta.Escaner)

    // Escuchador del VoiceCommander para saltar de pantalla por voz
    LaunchedEffect(textoVoz) {
        if (textoVoz.startsWith("NAV_DETALLE|")) {
            val nombre = textoVoz.substringAfter("|")
            navController.navigate(Ruta.DetalleReceta.crearRuta(nombre))
        }
    }

    // ESTRUCTURA BASE: Fila con Barra Lateral + Contenido
    Surface(color = MaterialTheme.colorScheme.background) {
        Row(modifier = Modifier.fillMaxSize()) {

            // --- BARRA LATERAL (NAVIGATION RAIL) ---
            val rutaActual = navController.currentBackStackEntryAsState().value?.destination?.route

            NavigationRail(
                modifier = Modifier.width(100.dp), // Ancho de la barra según Figma
                containerColor = Color.White,
                header = {
                    // Aquí podrías poner el logo de ChefCore si quieres
                    Spacer(modifier = Modifier.height(32.dp))
                }
            ) {
                items.forEach { pantalla ->
                    val seleccionado = rutaActual == pantalla.ruta
                    NavigationRailItem(
                        icon = pantalla.icono,
                        label = { Text(pantalla.titulo) },
                        selected = seleccionado,
                        onClick = {
                            navController.navigate(pantalla.ruta) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = Color(0xFF2E7D32), // Verde Corporativo [cite: 534]
                            selectedTextColor = Color(0xFF2E7D32),
                            unselectedIconColor = Color(0xFF6B7280), // Gris
                            unselectedTextColor = Color(0xFF6B7280),
                            indicatorColor = Color(0xFFE8F5E9) // Fondo verde clarito al seleccionar
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Separador visual entre la barra y el contenido
            VerticalDivider(color = Color.LightGray, thickness = 1.dp)

            // --- CONTENIDO DE LAS PANTALLAS ---
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                NavHost(
                    navController = navController,
                    startDestination = Ruta.Inventario.ruta
                ) {
                    composable(Ruta.Inventario.ruta) {
                        InventarioScreen(iDao = iDao, cocinaManager = cocinaManager)
                    }
                    composable(Ruta.Recetas.ruta) {
                        RecetasScreen(rDao = rDao, navController = navController)
                    }
                    // TODO: Las siguientes pantallas las haremos paso a paso
                    composable(Ruta.Personal.ruta) {
                        // PersonalScreen(uDao = uDao)
                    }
                    composable(Ruta.Escaner.ruta) {
                        AlbaranesScreen(aDao = aDao, cocinaManager = cocinaManager) // De momento usamos Albaranes aquí
                    }
                    composable(Ruta.DetalleReceta.ruta) { backStackEntry ->
                        val nombreReceta = backStackEntry.arguments?.getString("nombre") ?: ""
                        RecetaDetailScreen(
                            nombreReceta = nombreReceta, rDao = rDao, iDao = iDao,
                            cocinaManager = cocinaManager, onVolver = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}