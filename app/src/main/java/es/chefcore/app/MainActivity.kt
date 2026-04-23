package es.chefcore.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import es.chefcore.app.data.database.ChefCoreDatabase
import es.chefcore.app.logic.CocinaManager
import es.chefcore.app.ui.navigation.ChefCoreNavigation
import es.chefcore.app.ui.theme.ChefCoreTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = ChefCoreDatabase.getDatabase(applicationContext)
        val iDao = db.ingredienteDao()
        val rDao = db.recetaDao()
        val aDao = db.albaranDao()
        val uDao = db.usuarioDao()
        val cocinaManager = CocinaManager(iDao, rDao)

        setContent {
            ChefCoreTheme(darkTheme = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChefCoreNavigation(
                        iDao = iDao,
                        rDao = rDao,
                        aDao = aDao,
                        uDao = uDao,
                        cocinaManager = cocinaManager
                    )
                }
            }
        }
    }
}
