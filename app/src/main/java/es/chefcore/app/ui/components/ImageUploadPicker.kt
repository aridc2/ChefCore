package es.chefcore.app.ui.components

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import es.chefcore.app.ui.theme.ChefCoreColors

@Composable
fun ImageUploadPicker(
    imageUri: Uri?,
    onImageSelected: (Uri?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) onImageSelected(uri)
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempImageUri != null) {
            onImageSelected(tempImageUri)
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (imageUri != null) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp))) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Imagen seleccionada",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                IconButton(
                    onClick = { onImageSelected(null) },
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(ChefCoreColors.SurfaceGray.copy(alpha = 0.7f), RoundedCornerShape(50))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Eliminar", tint = ChefCoreColors.TextDark)
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(ChefCoreColors.SurfaceGray, RoundedCornerShape(12.dp))
                    .border(2.dp, ChefCoreColors.PrimaryGreen, RoundedCornerShape(12.dp))
                    .clickable { galleryLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Image, contentDescription = "Subir", modifier = Modifier.size(48.dp), tint = ChefCoreColors.PrimaryGreen)
                    Spacer(Modifier.height(8.dp))
                    Text("Toca para añadir foto de la receta", color = ChefCoreColors.TextMedium)
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = ChefCoreColors.SurfaceGray, contentColor = ChefCoreColors.TextDark)
            ) {
                Icon(Icons.Default.Image, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Galería")
            }
            Button(
                onClick = {
                    val uri = createImageUri(context)
                    tempImageUri = uri
                    cameraLauncher.launch(uri)
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = ChefCoreColors.PrimaryGreen)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Cámara")
            }
        }
    }
}

@Composable
fun IngredientImageUploadPicker(
    imageUri: Uri?,
    onImageSelected: (Uri?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) onImageSelected(uri)
    }

    Box(modifier = modifier) {
        if (imageUri != null) {
            Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp))) {
                AsyncImage(model = imageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                IconButton(
                    onClick = { onImageSelected(null) },
                    modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(ChefCoreColors.SurfaceGray.copy(alpha = 0.8f), RoundedCornerShape(50))
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp), tint = ChefCoreColors.TextDark)
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(ChefCoreColors.SurfaceGray, RoundedCornerShape(8.dp))
                    .border(1.dp, ChefCoreColors.PrimaryGreen, RoundedCornerShape(8.dp))
                    .clickable { galleryLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Image, contentDescription = null, tint = ChefCoreColors.PrimaryGreen)
            }
        }
    }
}

fun createImageUri(context: Context): Uri {
    val timestamp = System.currentTimeMillis()
    val storageDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)

    val imageFile = java.io.File.createTempFile(
        "JPEG_${timestamp}_", /* prefijo */
        ".jpg",               /* sufijo */
        storageDir            /* directorio */
    )

    return androidx.core.content.FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile
    )
}