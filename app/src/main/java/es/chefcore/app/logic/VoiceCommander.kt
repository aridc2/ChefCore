package es.chefcore.app.logic

import es.chefcore.app.data.database.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class VoiceCommander(
    private val iDao: IngredienteDao,
    private val rDao: RecetaDao,
    private val aDao: AlbaranDao,
    private val scope: CoroutineScope
) {
    // Memoria a corto plazo: sabe qué receta estamos editando
    private var recetaActiva: String = ""

    fun ejecutarComando(texto: String, onFeedback: (String) -> Unit) {
        val t = texto.trim().lowercase()

        scope.launch {
            when {
                // 1. CREAR O ABRIR RECETA
                t.startsWith("nueva receta") || t.startsWith("receta de") -> {
                    val nombre = t.replace("nueva receta de", "").replace("receta de", "").trim()
                        .replaceFirstChar { it.uppercase() }

                    val existente = rDao.buscarPorNombre(nombre)
                    if (existente == null) {
                        rDao.insertar(Receta(nombre = nombre))
                    }

                    recetaActiva = nombre // Guardamos en memoria que estamos en esta receta
                    onFeedback("NAV_DETALLE|$nombre") // Avisamos a la UI para que navegue
                }

                // 2. AÑADIR PASO A LA RECETA ACTIVA
                t.startsWith("añadir paso") || t.startsWith("paso") -> {
                    val contenidoPaso = t.replace("añadir paso", "").replace("paso", "").trim()
                        .replaceFirstChar { it.uppercase() }

                    if (recetaActiva.isNotEmpty()) {
                        val receta = rDao.buscarPorNombre(recetaActiva)
                        if (receta != null) {
                            // Pegamos el nuevo paso al final
                            val nuevasInstrucciones = if (receta.instrucciones.isEmpty()) {
                                contenidoPaso
                            } else {
                                receta.instrucciones + "\n" + contenidoPaso
                            }
                            rDao.actualizar(receta.copy(instrucciones = nuevasInstrucciones))
                            onFeedback("✅ Paso añadido a $recetaActiva")
                        }
                    } else {
                        onFeedback("⚠️ Abre una receta primero para añadir pasos")
                    }
                }

                // CASO B: ALBARANES
                t.startsWith("albarán de") || t.startsWith("nuevo albarán") -> {
                    val prov = t.replace("albarán de", "")
                        .replace("nuevo albarán", "")
                        .trim().replaceFirstChar { it.uppercase() }

                    aDao.insertar(Albaran(fecha = "Hoy", proveedor = prov, totalEuros = 0.0))
                    onFeedback("📄 Albarán de: $prov")
                }

                // CASO C: INGREDIENTES
                else -> {
                    val nuevoIng = VoiceParser.parsearIngrediente(t)
                    val existente = iDao.buscarPorNombre(nuevoIng.nombre)

                    if (existente != null) {
                        val actualizado = existente.copy(
                            cantidad = existente.cantidad + nuevoIng.cantidad,
                            precio = if (nuevoIng.precio > 0.0) nuevoIng.precio else existente.precio
                        )
                        iDao.actualizar(actualizado)
                        onFeedback("➕ Stock actualizado: ${nuevoIng.nombre}")
                    } else {
                        iDao.insertar(nuevoIng)
                        onFeedback("🍅 Nuevo ingrediente: ${nuevoIng.nombre}")
                    }
                }
            }
        }
    }
}