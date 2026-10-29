package com.franco.detectorllama.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.franco.detectorllama.firebase.RepositorioDetector

// pantalla del monitor: recibe lo que manda el sensor, lo puede activar o desactivar
// y muestra el historial de alertas
@Composable
fun PantallaMonitor(repo: RepositorioDetector, alVolver: () -> Unit) {
    DisposableEffect(Unit) {
        repo.escuchar()
        onDispose { repo.dejarDeEscuchar() }
    }

    var confirmarDesactivar by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Rol: MONITOR", style = MaterialTheme.typography.headlineSmall)
        Text(if (repo.conectadoNube) "Nube: conectado" else "Nube: sin conexión")

        TarjetaEstadoDetector(repo.estado)
        repo.ultimaActualizacion?.let { Text("Último dato: ${formatearFecha(it)}") }

        // botones para controlar el sensor
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { repo.enviarComando(true) },
                enabled = !repo.activo
            ) { Text("Activar") }
            // antes de desactivar pregunto, por si se apreta sin querer
            OutlinedButton(
                onClick = { confirmarDesactivar = true },
                enabled = repo.activo
            ) { Text("Desactivar") }
        }

        repo.error?.let { Text(it, color = Color(0xFFD32F2F)) }

        HorizontalDivider()
        Text("Historial de alertas (últimas 10)", style = MaterialTheme.typography.titleMedium)
        if (repo.alertas.isEmpty()) {
            Text("Sin alertas registradas")
        } else {
            repo.alertas.forEach { alerta ->
                Text("🔥 ${formatearFecha(alerta.timestamp)} · ${alerta.usuario}")
            }
        }

        OutlinedButton(onClick = alVolver) { Text("Cambiar de rol") }
    }

    if (confirmarDesactivar) {
        AlertDialog(
            onDismissRequest = { confirmarDesactivar = false },
            title = { Text("¿Desactivar el detector?") },
            text = { Text("Mientras esté desactivado no recibirás alertas de fuego.") },
            confirmButton = {
                TextButton(onClick = {
                    repo.enviarComando(false)
                    confirmarDesactivar = false
                }) { Text("Desactivar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarDesactivar = false }) { Text("Cancelar") }
            }
        )
    }
}
