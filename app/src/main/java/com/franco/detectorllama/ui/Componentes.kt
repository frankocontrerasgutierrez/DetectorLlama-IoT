package com.franco.detectorllama.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.franco.detectorllama.firebase.RepositorioDetector
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// tarjeta que cambia de color segun el estado, la uso en sensor y monitor
@Composable
fun TarjetaEstadoDetector(estado: Int?) {
    val (texto, color) = when (estado) {
        RepositorioDetector.ESTADO_FUEGO -> "🔥 ¡FUEGO DETECTADO!" to Color(0xFFD32F2F)
        RepositorioDetector.ESTADO_NORMAL -> "✅ Sin fuego" to Color(0xFF388E3C)
        RepositorioDetector.ESTADO_DESACTIVADO -> "⏸ Detector desactivado" to Color(0xFF616161)
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

// paso los milisegundos a una fecha que se pueda leer
fun formatearFecha(ms: Long): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(ms))
