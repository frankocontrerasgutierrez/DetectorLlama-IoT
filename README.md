# Detector de Llama IoT

Proyecto de la asignatura **Aplicaciones Móviles para IoT (TI3042)** - INACAP
Evaluación Unidad 2 - Franko Contreras

## De qué se trata

Es una app Android para detectar fuego y avisar en tiempo real a otro dispositivo.
La misma app tiene dos roles:

- **Sensor:** simula el sensor de llama KY-026 con un switch y sube el estado a la nube.
- **Monitor:** recibe el estado al instante, puede activar o desactivar el sensor y ve el historial de alertas.

Los dos dispositivos se comunican por Wi-Fi usando Firebase Realtime Database.

## Estados

| Valor | Significado |
|---|---|
| 0 | Normal |
| 1 | Fuego |
| 2 | Desactivado |

## Herramientas

- Android Studio + Kotlin
- Jetpack Compose con Material Design 3
- Navigation Compose
- Firebase Authentication (login con correo y contraseña)
- Firebase Realtime Database

## Estructura de datos en Firebase

```
sensores/
  detector-01/
    estado:  { valor, timestamp, usuario }    <- lo escribe el sensor
    comando: { activo, timestamp, usuario }   <- lo escribe el monitor
    alertas: { id: { timestamp, usuario } }   <- historial de fuego
```

## Seguridad

- Login obligatorio con Firebase Authentication.
- Conexión cifrada (HTTPS/TLS).
- Validación de datos: solo se aceptan los valores 0, 1 y 2.
- La hora la pone el servidor y cada registro guarda el usuario (trazabilidad).
- Confirmación antes de desactivar el detector.
- Solo se piden los permisos necesarios.

## Cómo probarlo

1. Instalar la app en dos dispositivos o emuladores.
2. Iniciar sesión (o crear cuenta).
3. En uno elegir **Sensor** y en el otro **Monitor**.
4. Activar el switch de llama en el sensor y ver la alerta en el monitor.

La carpeta `arduino/` y las pantallas de Bluetooth son de la Unidad 1 y se van a usar en la Evaluación 3 con el Arduino UNO R4 WiFi.
