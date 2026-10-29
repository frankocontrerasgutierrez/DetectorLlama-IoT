package com.franco.detectorllama

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.franco.detectorllama.ble.BleIoT
import com.franco.detectorllama.firebase.FirebaseConfig
import com.franco.detectorllama.firebase.RepositorioDetector
import com.franco.detectorllama.ui.PantallaConfiguracion
import com.franco.detectorllama.ui.PantallaInicio
import com.franco.detectorllama.ui.PantallaLogin
import com.franco.detectorllama.ui.PantallaMonitor
import com.franco.detectorllama.ui.PantallaRol
import com.franco.detectorllama.ui.PantallaSensor
import com.google.firebase.auth.FirebaseAuth
import com.franco.detectorllama.ui.theme.DetectorLlamaBLETheme

// nombres de las pantallas, asi no me equivoco escribiendolos
object Rutas {
    const val INICIO = "inicio"
    const val CONFIGURACION = "configuracion"
    // pantallas de la eva 2 (firebase)
    const val LOGIN = "login"
    const val ROL = "rol"
    const val SENSOR = "sensor"
    const val MONITOR = "monitor"
}

// unica activity, todas las pantallas van adentro con compose
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // primero inicio firebase, si no el login no funciona
        FirebaseConfig.inicializar(this)
        setContent {
            DetectorLlamaBLETheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppDetector(Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun AppDetector(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // objeto bluetooth para las pantallas de la unidad 1
    val ble = remember { BleIoT(context.applicationContext) }
    // al cerrar la app corto el bluetooth
    DisposableEffect(Unit) { onDispose { ble.cerrar() } }

    // el repositorio lo comparten sensor y monitor
    val repo = remember { RepositorioDetector() }

    // si ya habia iniciado sesion me salto el login
    val inicio = if (FirebaseAuth.getInstance().currentUser != null) Rutas.ROL else Rutas.LOGIN

    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = inicio,
        modifier = modifier
    ) {
        composable(Rutas.LOGIN) {
            PantallaLogin(alEntrar = {
                navController.navigate(Rutas.ROL) { popUpTo(Rutas.LOGIN) { inclusive = true } }
            })
        }
        composable(Rutas.ROL) {
            PantallaRol(
                alElegirSensor = { navController.navigate(Rutas.SENSOR) },
                alElegirMonitor = { navController.navigate(Rutas.MONITOR) },
                alCerrarSesion = {
                    navController.navigate(Rutas.LOGIN) { popUpTo(Rutas.ROL) { inclusive = true } }
                }
            )
        }
        composable(Rutas.SENSOR) { PantallaSensor(repo, alVolver = { navController.popBackStack() }) }
        composable(Rutas.MONITOR) { PantallaMonitor(repo, alVolver = { navController.popBackStack() }) }
        // pantallas de bluetooth de la unidad 1, las dejo para la eva 3
        composable(Rutas.INICIO) { PantallaInicio(ble, navController) }
        composable(Rutas.CONFIGURACION) { PantallaConfiguracion(ble, navController) }
    }
}
