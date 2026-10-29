package com.franco.detectorllama.ui

import android.util.Patterns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

// paso los errores de firebase a mensajes mas entendibles
// no digo si fallo el correo o la clave, asi no se puede adivinar que cuentas existen
private fun mensajeDeError(e: Exception?): String = when (e) {
    is FirebaseAuthUserCollisionException -> "Ese correo ya está registrado"
    is FirebaseAuthWeakPasswordException -> "La contraseña debe tener al menos 6 caracteres"
    is FirebaseNetworkException -> "Sin conexión a internet"
    else -> "Correo o contraseña incorrectos"
}

@Composable
fun PantallaLogin(alEntrar: () -> Unit) {
    val auth = remember { FirebaseAuth.getInstance() }
    var correo by remember { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // reviso los datos antes de mandarlos a firebase
    fun validar(): Boolean {
        error = when {
            !Patterns.EMAIL_ADDRESS.matcher(correo.trim()).matches() -> "Ingresa un correo válido"
            clave.length < 6 -> "La contraseña debe tener al menos 6 caracteres"
            else -> null
        }
        return error == null
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Detector de llama IoT", style = MaterialTheme.typography.headlineMedium)
        Text("Inicia sesión para monitorear o simular el sensor.")

        OutlinedTextField(
            value = correo,
            onValueChange = { correo = it },
            label = { Text("Correo") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = clave,
            onValueChange = { clave = it },
            label = { Text("Contraseña") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),   // para que la clave salga con puntitos
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        error?.let { Text(it, color = Color(0xFFD32F2F)) }

        if (cargando) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    if (validar()) {
                        cargando = true
                        auth.signInWithEmailAndPassword(correo.trim(), clave)
                            .addOnCompleteListener { tarea ->
                                cargando = false
                                if (tarea.isSuccessful) alEntrar()
                                else error = mensajeDeError(tarea.exception)
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Ingresar") }

            OutlinedButton(
                onClick = {
                    if (validar()) {
                        cargando = true
                        auth.createUserWithEmailAndPassword(correo.trim(), clave)
                            .addOnCompleteListener { tarea ->
                                cargando = false
                                if (tarea.isSuccessful) alEntrar()
                                else error = mensajeDeError(tarea.exception)
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Crear cuenta") }
        }
    }
}
