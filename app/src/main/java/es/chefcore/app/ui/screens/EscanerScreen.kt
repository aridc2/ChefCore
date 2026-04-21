import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EscanerScreen() {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF3F4F6))) {
        Column(modifier = Modifier.padding(32.dp)) {
            Text("Escanear Albarán", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Text("Enfoca el ticket o factura para registrar stock automáticamente", color = Color(0xFF6B7280))

            Spacer(modifier = Modifier.height(32.dp))

            // Simulación del visor de cámara
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("Visor de Cámara", color = Color.White)
                // Aquí irá el CameraPreview más adelante
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { /* Acción */ },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Text("TOMAR FOTO", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}