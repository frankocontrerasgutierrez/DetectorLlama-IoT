package com.franco.detectorllama.firebase

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener

data class Alerta(val id: String, val timestamp: Long, val usuario: String)

class RepositorioDetector {

    companion object {
        const val SENSOR_ID = "detector-0"
        const val ESTADO_NORMAL = 0
        const val ESTADO_FUEGO = 1
        const val ESTADO_DESACTIVADO = 2
    }

    // variables que leen las pantallas, cuando cambian se actualiza la ui
    var estado by mutableStateOf<Int?>(null); private set          // null si todavia no llega nada
    var ultimaActualizacion by mutableStateOf<Long?>(null); private set
    var activo by mutableStateOf(true); private set                 // lo controla el monitor
    var conectadoNube by mutableStateOf(false); private set         // para mostrar si hay conexion
    var error by mutableStateOf<String?>(null); private set
    val alertas = mutableStateListOf<Alerta>()

    private val db: FirebaseDatabase by lazy { FirebaseDatabase.getInstance() }
    private val ref: DatabaseReference by lazy { db.getReference("sensores").child(SENSOR_ID) }
    // traigo solo las ultimas 10 alertas
    private val consultaAlertas by lazy { ref.child("alertas").orderByChild("timestamp").limitToLast(10) }
    // cuento cuantas pantallas estan escuchando, asi al cambiar de rol no se corta la conexion
    private var pantallasEscuchando = 0

    private fun usuarioActual(): String = FirebaseAuth.getInstance().currentUser?.email ?: "desconocido"

    // listeners: firebase me avisa cuando cambia algo
    private val estadoListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val v = snapshot.child("valor").getValue(Long::class.java)?.toInt()
            // solo acepto 0, 1 o 2, si llega otra cosa lo ignoro
            estado = if (v != null && v in ESTADO_NORMAL..ESTADO_DESACTIVADO) v else null
            ultimaActualizacion = snapshot.child("timestamp").getValue(Long::class.java)
        }
        override fun onCancelled(e: DatabaseError) { error = "Sin acceso al estado: ${e.message}" }
    }

    private val comandoListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            // si no hay comando todavia parte activado
            activo = snapshot.child("activo").getValue(Boolean::class.java) ?: true
        }
        override fun onCancelled(e: DatabaseError) { error = "Sin acceso al comando: ${e.message}" }
    }

    private val alertasListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val lista = snapshot.children.mapNotNull { hijo ->
                val ts = hijo.child("timestamp").getValue(Long::class.java) ?: return@mapNotNull null
                Alerta(hijo.key ?: "", ts, hijo.child("usuario").getValue(String::class.java) ?: "")
            }
            alertas.clear()
            alertas.addAll(lista.sortedByDescending { it.timestamp })   // ordeno para que salga primero la mas nueva
        }
        override fun onCancelled(e: DatabaseError) { error = "Sin acceso al historial: ${e.message}" }
    }

    private val conexionListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            conectadoNube = snapshot.getValue(Boolean::class.java) ?: false
        }
        override fun onCancelled(e: DatabaseError) { conectadoNube = false }
    }

    fun escuchar() {
        pantallasEscuchando++
        if (pantallasEscuchando > 1) return   // ya estaba escuchando
        error = null
        ref.child("estado").addValueEventListener(estadoListener)
        ref.child("comando").addValueEventListener(comandoListener)
        consultaAlertas.addValueEventListener(alertasListener)
        db.getReference(".info/connected").addValueEventListener(conexionListener)
    }

    fun dejarDeEscuchar() {
        if (pantallasEscuchando == 0) return
        pantallasEscuchando--
        if (pantallasEscuchando > 0) return   // otra pantalla todavia lo necesita
        ref.child("estado").removeEventListener(estadoListener)
        ref.child("comando").removeEventListener(comandoListener)
        consultaAlertas.removeEventListener(alertasListener)
        db.getReference(".info/connected").removeEventListener(conexionListener)
    }


    // el sensor sube su estado a firebase
    fun publicarEstado(valor: Int) {
        if (valor !in ESTADO_NORMAL..ESTADO_DESACTIVADO) return
        val datos = mapOf(
            "valor" to valor,
            "timestamp" to ServerValue.TIMESTAMP,   // uso la hora del servidor y no la del celu
            "usuario" to usuarioActual()
        )
        ref.child("estado").setValue(datos)
            .addOnFailureListener { error = "No se pudo publicar: ${it.message}" }
    }

    // cuando hay fuego guardo la alerta en el historial
    fun registrarAlerta() {
        val datos = mapOf("timestamp" to ServerValue.TIMESTAMP, "usuario" to usuarioActual())
        ref.child("alertas").push().setValue(datos)   // push genera un id distinto para cada alerta
            .addOnFailureListener { error = "No se pudo guardar la alerta: ${it.message}" }
    }

    // el monitor activa o desactiva el sensor
    fun enviarComando(activar: Boolean) {
        val datos = mapOf(
            "activo" to activar,
            "timestamp" to ServerValue.TIMESTAMP,
            "usuario" to usuarioActual()
        )
        ref.child("comando").setValue(datos)
            .addOnFailureListener { error = "No se pudo enviar el comando: ${it.message}" }
    }
}
