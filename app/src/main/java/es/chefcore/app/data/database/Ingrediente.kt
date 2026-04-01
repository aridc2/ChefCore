package es.chefcore.app.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ingredientes")
data class Ingrediente(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nombre: String,
    val cantidad: Double,
    val unidad: String,
    val precio: Double = 0.0
)