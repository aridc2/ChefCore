package es.chefcore.app.data.database

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "receta_ingrediente",
    primaryKeys = ["recetaId", "ingredienteId"],
    foreignKeys = [
        ForeignKey(
            entity = Receta::class,
            parentColumns = ["id"],
            childColumns = ["recetaId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Ingrediente::class,
            parentColumns = ["id"],
            childColumns = ["ingredienteId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class RecetaIngrediente(
    val recetaId: Int,
    val ingredienteId: Int,
    val cantidadNecesaria: Double
)

data class IngredienteEnReceta(
    val ingredienteId: Int,
    val nombre: String,
    val cantidadNecesaria: Double,
    val unidad: String,
    val precioUnitario: Double
) {
    val costeTotal: Double get() = cantidadNecesaria * precioUnitario
}