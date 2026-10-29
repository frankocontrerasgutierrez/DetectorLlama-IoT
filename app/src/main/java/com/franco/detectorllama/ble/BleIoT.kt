package com.franco.detectorllama.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.UUID

// estados de la conexion, la pantalla cambia segun esto
enum class EstadoBle { INACTIVO, ESCANEANDO, CONECTANDO, LISTO, DESCONECTADO, ERROR }

// los permisos se piden antes en PermisosBluetooth.kt
@SuppressLint("MissingPermission")
class BleIoT(private val context: Context) {

    companion object {
        // tienen que ser los mismos uuid del arduino
        val SERVICIO: UUID = UUID.fromString("19b10000-e8f2-537e-4f6c-d104768a1214")
        val COMANDO: UUID = UUID.fromString("19b10001-e8f2-537e-4f6c-d104768a1214")
        val ESTADO: UUID = UUID.fromString("19b10002-e8f2-537e-4f6c-d104768a1214")

        // descriptor para activar las notificaciones
        val CCCD: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        // comandos que mando al arduino
        const val CMD_DESACTIVAR = 0
        const val CMD_ACTIVAR = 1

        // estados que manda el arduino
        const val ESTADO_NORMAL = 0
        const val ESTADO_FUEGO = 1
        const val ESTADO_DESACTIVADO = 2
    }

    // variables que usa la pantalla
    var estado by mutableStateOf(EstadoBle.INACTIVO); private set
    var mensaje by mutableStateOf("Sin conexión"); private set
    var estadoDetector by mutableStateOf<Int?>(null); private set   // null si no ha llegado nada
    var nombreConectado by mutableStateOf<String?>(null); private set
    val encontrados = mutableStateListOf<ScanResult>()

    private val adapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    private val handler = Handler(Looper.getMainLooper())
    private var gatt: BluetoothGatt? = null
    private var caracteristicaComando: BluetoothGattCharacteristic? = null

    // lo del bluetooth llega en otro hilo, asi que lo paso al hilo principal
    private fun enMain(bloque: () -> Unit) {
        handler.post { bloque() }
    }

    private fun ponerError(texto: String) {
        estado = EstadoBle.ERROR
        mensaje = texto
    }

    // buscar dispositivos
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            // para que no salga repetido el mismo arduino
            if (encontrados.none { it.device.address == result.device.address }) {
                encontrados.add(result)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            ponerError("Falló el escaneo (código $errorCode)")
        }
    }

    private val cortarEscaneo = Runnable { detenerEscaneo() }

    fun escanear() {
        val tieneBle = context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
        if (!tieneBle || adapter == null) {
            ponerError("Este equipo no tiene Bluetooth (¿emulador?)")
            return
        }
        if (!adapter.isEnabled) {
            ponerError("Active el Bluetooth del celular")
            return
        }
        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            ponerError("No se pudo acceder al escáner Bluetooth")
            return
        }

        encontrados.clear()
        // solo busco los que tienen mi servicio
        val filtro = ScanFilter.Builder().setServiceUuid(ParcelUuid(SERVICIO)).build()
        val ajustes = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()

        try {
            scanner.startScan(listOf(filtro), ajustes, scanCallback)
        } catch (e: SecurityException) {
            ponerError("Faltan los permisos Bluetooth")
            return
        }
        estado = EstadoBle.ESCANEANDO
        mensaje = "Buscando dispositivos…"

        // a los 10 segundos paro de buscar para no gastar bateria
        handler.removeCallbacks(cortarEscaneo)
        handler.postDelayed(cortarEscaneo, 10_000)
    }

    fun detenerEscaneo() {
        handler.removeCallbacks(cortarEscaneo)
        if (estado == EstadoBle.ESCANEANDO) {
            runCatching { adapter?.bluetoothLeScanner?.stopScan(scanCallback) }
            estado = EstadoBle.INACTIVO
            mensaje = if (encontrados.isEmpty()) "No se encontró ningún dispositivo"
                      else "Elija su dispositivo"
        }
    }

    // conectarse
    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    enMain { mensaje = "Conectado. Descubriendo servicios…" }
                    g.discoverServices()   // hay que hacer esto antes de leer o escribir
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    g.close()              // libero la conexion
                    enMain {
                        if (gatt == g) gatt = null
                        caracteristicaComando = null
                        estadoDetector = null
                        nombreConectado = null
                        estado = EstadoBle.DESCONECTADO
                        mensaje = if (status == BluetoothGatt.GATT_SUCCESS) "Desconectado"
                                  else "Se perdió la conexión (código $status)"
                    }
                }
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            val servicio = g.getService(SERVICIO)
            val comando = servicio?.getCharacteristic(COMANDO)
            val estadoC = servicio?.getCharacteristic(ESTADO)
            if (status != BluetoothGatt.GATT_SUCCESS || comando == null || estadoC == null) {
                enMain { ponerError("El dispositivo no tiene el servicio del detector") }
                g.disconnect()
                return
            }
            enMain {
                caracteristicaComando = comando
                mensaje = "Activando notificaciones…"
            }
            activarNotificaciones(g, estadoC)
        }

        // si se activaron las notificaciones ya queda listo
        override fun onDescriptorWrite(g: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            enMain {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    estado = EstadoBle.LISTO
                    mensaje = "Listo: recibiendo datos del detector"
                } else {
                    ponerError("No se pudieron activar las notificaciones (código $status)")
                }
            }
        }

        // android 13 para arriba
        override fun onCharacteristicChanged(
            g: BluetoothGatt, c: BluetoothGattCharacteristic, value: ByteArray
        ) {
            procesarDato(c.uuid, value)
        }

        // android 12 o menos (forma antigua)
        @Deprecated("Deprecated in Java")
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(g: BluetoothGatt, c: BluetoothGattCharacteristic) {
            val valor = c.value ?: return
            procesarDato(c.uuid, valor)
        }
    }

    private fun activarNotificaciones(g: BluetoothGatt, c: BluetoothGattCharacteristic) {
        g.setCharacteristicNotification(c, true)   // le aviso a android
        val descriptor = c.getDescriptor(CCCD)     // y le aviso al arduino
        if (descriptor == null) {
            enMain { ponerError("La característica no permite notificaciones") }
            return
        }
        val valor = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            g.writeDescriptor(descriptor, valor)
        } else {
            @Suppress("DEPRECATION")
            run {
                descriptor.value = valor
                g.writeDescriptor(descriptor)
            }
        }
    }

    // solo acepto 0, 1 y 2
    private fun procesarDato(uuid: UUID, valor: ByteArray) {
        if (uuid != ESTADO) return
        val v = valor.firstOrNull()?.toInt() ?: return
        if (v in ESTADO_NORMAL..ESTADO_DESACTIVADO) {
            enMain { estadoDetector = v }
        }
    }

    fun conectar(dispositivo: BluetoothDevice, nombre: String) {
        detenerEscaneo()
        estado = EstadoBle.CONECTANDO
        mensaje = "Conectando…"
        nombreConectado = nombre
        gatt = dispositivo.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    fun desconectar() {
        gatt?.disconnect()
    }

    fun cerrar() {   // se llama al salir de la app
        handler.removeCallbacksAndMessages(null)
        runCatching { adapter?.bluetoothLeScanner?.stopScan(scanCallback) }
        runCatching { gatt?.close() }
        gatt = null
    }

    // enviar comandos
    fun enviar(valor: Int) {
        // solo mando comandos que existen
        if (valor != CMD_ACTIVAR && valor != CMD_DESACTIVAR) return
        val g = gatt ?: return
        val c = caracteristicaComando ?: return
        if (estado != EstadoBle.LISTO) return

        val datos = byteArrayOf(valor.toByte())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // android 13 para arriba
            g.writeCharacteristic(c, datos, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
        } else {
            // android 12 o menos
            @Suppress("DEPRECATION")
            run {
                c.value = datos
                c.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                g.writeCharacteristic(c)
            }
        }
    }
}
