package es.chefcore.app.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "albaranes")
data class Albaran(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val fecha: String,
    val proveedor: String,
    val totalEuros: Double
)