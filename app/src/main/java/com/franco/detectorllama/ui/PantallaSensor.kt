package com.franco.detectorllama.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.franco.detectorllama.firebase.RepositorioDetector

// pantalla del sensor, por ahora simulo el KY-026 con un switch (en la eva 3 va el arduino)
// desactivado = 2, hay llama = 1, normal = 0
@Composable
fun PantallaSensor(repo: RepositorioDetector, alVolver: () -> Unit) {
    // escucho firebase solo mientras estoy en esta pantalla
    DisposableEffect(Unit) {
        repo.escuchar()
        onDispose { repo.dejarDeEscuchar() }
    }

    var hayLlama by remember { mutableStateOf(false) }   // esto reemplaza la lectura del sensor real

    val estadoCalculado = when {
        !repo.activo -> RepositorioDetector.ESTADO_DESACTIVADO
        hayLlama -> RepositorioDetector.ESTADO_FUEGO
        else -> RepositorioDetector.ESTADO_NORMAL
    }

    // cuando cambia el estado lo subo, y si es fuego guardo la alerta
    LaunchedEffect(estadoCalculado) {
        repo.publicarEstado(estadoCalculado)
        if (estadoCalculado == RepositorioDetector.ESTADO_FUEGO) repo.registrarAlerta()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Rol: SENSOR", style = MaterialTheme.typography.headlineSmall)
        Text(if (repo.conectadoNube) "Nube: conectado" else "Nube: sin conexión")

        TarjetaEstadoDetector(estadoCalculado)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Simular llama frente al sensor")
            Switch(checked = hayLlama, onCheckedChange = { hayLlama = it })
        }

        Text(
            if (repo.activo) "El monitor tiene el detector ACTIVADO"
            else "El monitor DESACTIVÓ el detector: no se envían alertas"
        )

        repo.error?.let { Text(it, color = Color(0xFFD32F2F)) }

        OutlinedButton(onClick = alVolver) { Text("Cambiar de rol") }
    }
}
