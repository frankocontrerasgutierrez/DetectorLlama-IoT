package com.franco.detectorllama.permisos

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

// pido solo los permisos que necesita cada version de android
fun permisosBluetoothNecesarios(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {          // android 12 para arriba
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {                                                       // android 11 o menos
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

// reviso si ya estan aceptados
fun tienePermisosBluetooth(context: Context): Boolean =
    permisosBluetoothNecesarios().all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

private enum class EstadoPermiso { SIN_PEDIR, CONCEDIDO, DENEGADO, DENEGADO_PERMANENTE }

// busco la activity real porque en compose el context puede venir de otra forma
private fun Context.buscarActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.buscarActivity()
    else -> null
}

// si los permisos estan ok muestra la pantalla, si no explica para que son
// y ve los casos de aceptado, rechazado o rechazado para siempre
@Composable
fun PermisosBluetoothConEstados(contenido: @Composable () -> Unit) {
    val context = LocalContext.current
    var estado by remember {
        mutableStateOf(
            if (tienePermisosBluetooth(context)) EstadoPermiso.CONCEDIDO else EstadoPermiso.SIN_PEDIR
        )
    }

    // abre la ventanita de permisos y recibe lo que elige el usuario
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        estado = if (tienePermisosBluetooth(context)) {
            EstadoPermiso.CONCEDIDO
        } else {
            val activity = context.buscarActivity()
            val puedeExplicar = activity != null && permisosBluetoothNecesarios().any {
                ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
            }
            if (puedeExplicar) EstadoPermiso.DENEGADO else EstadoPermiso.DENEGADO_PERMANENTE
        }
    }

    when (estado) {
        EstadoPermiso.CONCEDIDO -> contenido()

        EstadoPermiso.SIN_PEDIR -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Para buscar y conectarse al detector de llama, la app necesita usar el Bluetooth.")
            Button(onClick = { launcher.launch(permisosBluetoothNecesarios()) }) {
                Text("Permitir Bluetooth")
            }
        }

        EstadoPermiso.DENEGADO -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Sin el permiso Bluetooth no se pueden recibir las alertas de fuego del detector.")
            Button(onClick = { launcher.launch(permisosBluetoothNecesarios()) }) {
                Text("Reintentar")
            }
        }

        EstadoPermiso.DENEGADO_PERMANENTE -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("El permiso fue bloqueado. Actívalo en Ajustes > Permisos > Dispositivos cercanos.")
            Button(onClick = {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)
                )
                context.startActivity(intent)
            }) { Text("Abrir Ajustes") }
            OutlinedButton(onClick = {
                if (tienePermisosBluetooth(context)) estado = EstadoPermiso.CONCEDIDO
            }) { Text("Ya lo activé, verificar") }
        }
    }
}
