package com.franco.detectorllama.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth

// la misma app sirve para los dos, un celu es sensor y el otro monitor
@Composable
fun PantallaRol(
    alElegirSensor: () -> Unit,
    alElegirMonitor: () -> Unit,
    alCerrarSesion: () -> Unit
) {
    val correo = FirebaseAuth.getInstance().currentUser?.email ?: ""
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("¿Qué rol cumple este dispositivo?", style = MaterialTheme.typography.headlineSmall)
        Text("Sesión: $correo")

        Button(onClick = alElegirSensor, modifier = Modifier.fillMaxWidth()) {
            Text("Sensor (simula el detector KY-026 y envía datos)")
        }
        Button(onClick = alElegirMonitor, modifier = Modifier.fillMaxWidth()) {
            Text("Monitor (recibe alertas y controla el sensor)")
        }

        TextButton(onClick = {
            FirebaseAuth.getInstance().signOut()
            alCerrarSesion()
        }) { Text("Cerrar sesión") }
    }
}
