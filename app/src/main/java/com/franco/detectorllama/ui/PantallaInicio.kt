package com.franco.detectorllama.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.franco.detectorllama.Rutas
import com.franco.detectorllama.ble.BleIoT
import com.franco.detectorllama.ble.EstadoBle

// pantalla de inicio de la unidad 1, muestra lo que manda el arduino por bluetooth
@Composable
fun PantallaInicio(ble: BleIoT, navController: NavController) {
    var confirmarDesactivar by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Detector de llama", style = MaterialTheme.typography.headlineMedium)
        Text("Conexión: ${ble.mensaje}")

        TarjetaEstado(ble.estadoDetector)

        if (ble.estado == EstadoBle.LISTO) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { ble.enviar(BleIoT.CMD_ACTIVAR) }) { Text("Activar") }
                // pido confirmacion antes de desactivar
                OutlinedButton(onClick = { confirmarDesactivar = true }) { Text("Desactivar") }
            }
        } else {
            Text("Conéctate al detector en Configuración para ver los datos.")
        }

        Button(onClick = { navController.navigate(Rutas.CONFIGURACION) }) {
            Text("Ir a Configuración")
        }
    }

    if (confirmarDesactivar) {
        AlertDialog(
            onDismissRequest = { confirmarDesactivar = false },
            title = { Text("¿Desactivar el detector?") },
            text = { Text("Mientras esté desactivado no recibirás alertas de fuego.") },
            confirmButton = {
                TextButton(onClick = {
                    ble.enviar(BleIoT.CMD_DESACTIVAR)
                    confirmarDesactivar = false
                }) { Text("Desactivar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarDesactivar = false }) { Text("Cancelar") }
            }
        )
    }
}

// tarjeta de color segun lo que manda el arduino
@Composable
private fun TarjetaEstado(estado: Int?) {
    val (texto, color) = when (estado) {
        BleIoT.ESTADO_FUEGO -> "🔥 ¡FUEGO DETECTADO!" to Color(0xFFD32F2F)
        BleIoT.ESTADO_NORMAL -> "✅ Sin fuego" to Color(0xFF388E3C)
        BleIoT.ESTADO_DESACTIVADO -> "⏸ Detector desactivado" to Color(0xFF616161)
        else -> "Sin datos del detector" to Color(0xFF9E9E9E)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Text(
            text = texto,
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(24.dp)
        )
    }
}
