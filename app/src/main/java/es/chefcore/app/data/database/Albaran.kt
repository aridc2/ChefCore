package es.chefcore.app.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "albaranes",
    foreignKeys = [
        ForeignKey(
            entity = Usuario::class,
            parentColumns = ["id"],
            childColumns = ["idUsuario"],
            onDelete = ForeignKey.SET_DEFAULT
        )
    ],
    indices = [Index("idUsuario")]
)
data class Albaran(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val fecha: String,
    val proveedor: String,
    val totalEuros: Double,
    val idUsuario: Int = 0
)