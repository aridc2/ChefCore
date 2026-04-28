package es.chefcore.app.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recetas")
data class Receta(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nombre: String,
    val tiempoPreparacionMinutos: Int = 30,
    val instrucciones: String = "",
    val precioVenta: Double = 0.0,
    val imagenUri: String? = null
)