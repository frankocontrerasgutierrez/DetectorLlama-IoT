package com.franco.detectorllama.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.franco.detectorllama.ble.BleIoT
import com.franco.detectorllama.ble.EstadoBle
import com.franco.detectorllama.permisos.PermisosBluetoothConEstados

// pantalla para buscar y conectar el arduino por bluetooth (unidad 1)
@Composable
fun PantallaConfiguracion(ble: BleIoT, navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Conexión Bluetooth", style = MaterialTheme.typography.headlineSmall)

        // esto solo aparece si ya aceptaron los permisos
        PermisosBluetoothConEstados {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Estado: ${ble.mensaje}", style = MaterialTheme.typography.titleMedium)

                Button(
                    onClick = { ble.escanear() },
                    enabled = ble.estado in listOf(
                        EstadoBle.INACTIVO, EstadoBle.DESCONECTADO, EstadoBle.ERROR
                    )
                ) { Text("Buscar detector") }

                if (ble.estado == EstadoBle.ESCANEANDO) {
                    TextButton(onClick = { ble.detenerEscaneo() }) { Text("Detener búsqueda") }
                }

                // lista de dispositivos que encontro, solo los que tienen mi uuid
                ble.encontrados.forEach { r ->
                    val nombre = r.scanRecord?.deviceName ?: "(sin nombre)"
                    OutlinedButton(
                        onClick = { ble.conectar(r.device, nombre) },
                        enabled = ble.estado != EstadoBle.CONECTANDO && ble.estado != EstadoBle.LISTO,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // muestro la mac porque el nombre cualquiera lo puede copiar
                        Text("$nombre · ${r.device.address} · ${r.rssi} dBm")
                    }
                }

                if (ble.estado == EstadoBle.LISTO) {
                    Text("Conectado a: ${ble.nombreConectado}")
                    TextButton(onClick = { ble.desconectar() }) { Text("Desconectar") }
                }
            }
        }

        OutlinedButton(onClick = { navController.popBackStack() }) { Text("Volver") }
    }
}
