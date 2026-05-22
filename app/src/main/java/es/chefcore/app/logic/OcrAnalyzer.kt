package es.chefcore.app.logic

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Resultado completo de ML Kit con texto plano + bloques con posición */
data class ResultadoMLKit(
    val texto: String,
    val bloques: List<Text.TextBlock>
)

/**
 * Wrapper de ML Kit Text Recognition.
 * recognizeWithBlocks devuelve bloques con bounding boxes —
 * necesarios para reconstruir filas de tabla por posición Y.
 */
object OcrAnalyzer {

    /**
     * Extrae texto Y estructura de bloques (con posición X,Y).
     * Usar cuando se necesite reconstruir filas de tabla (albaranes).
     */
    suspend fun recognizeWithBlocks(bitmap: Bitmap): ResultadoMLKit =
        suspendCancellableCoroutine { cont ->
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    cont.resume(ResultadoMLKit(result.text, result.textBlocks))
                }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        }

    /** Versión simplificada para compatibilidad */
    suspend fun recognizeText(bitmap: Bitmap): String =
        recognizeWithBlocks(bitmap).texto
}