/*
 * Detector de llama BLE - Arduino UNO R4 WiFi + sensor KY-026
 * TI3042 Aplicaciones Moviles para IoT - Unidad 2
 *
 * El Arduino es el PERIFERICO BLE: se anuncia y espera que el celular (CENTRAL) se conecte.
 *  - Caracteristica COMANDO (19b10001): la app ESCRIBE 1 = activar detector, 0 = desactivar.
 *  - Caracteristica ESTADO  (19b10002): el Arduino NOTIFICA 0 = normal, 1 = fuego, 2 = desactivado.
 */
#include <ArduinoBLE.h>
#include "Arduino_LED_Matrix.h"

// configuracion
const int PIN_LLAMA = 2;              // Salida DO de la KY-026 conectada al pin digital 2
const int NIVEL_FUEGO = HIGH;         // Si al acercar la llama NO detecta, cambia HIGH por LOW
const char* NOMBRE_BLE = "IoT-Franco"; // Nombre unico para no confundirse con otras placas del aula

// valores que entiende la app (los mismos de BleIoT.kt)
const byte ESTADO_NORMAL = 0;
const byte ESTADO_FUEGO = 1;
const byte ESTADO_DESACTIVADO = 2;
const byte CMD_DESACTIVAR = 0;
const byte CMD_ACTIVAR = 1;

// servicio y caracteristicas ble
BLEService servicioDetector("19b10000-e8f2-537e-4f6c-d104768a1214");
BLEByteCharacteristic caracteristicaComando("19b10001-e8f2-537e-4f6c-d104768a1214", BLERead | BLEWrite);
BLEByteCharacteristic caracteristicaEstado("19b10002-e8f2-537e-4f6c-d104768a1214", BLERead | BLENotify);

ArduinoLEDMatrix matriz;

// Dibujos para la matriz 8x12 (1 = LED encendido)
uint8_t dibujoFuego[8][12] = {
  {0,0,0,0,0,1,0,0,0,0,0,0},
  {0,0,0,0,1,1,0,0,0,0,0,0},
  {0,0,0,0,1,1,1,0,0,0,0,0},
  {0,0,0,1,1,1,1,0,1,0,0,0},
  {0,0,1,1,1,0,1,1,1,0,0,0},
  {0,0,1,1,0,0,0,1,1,0,0,0},
  {0,0,1,1,0,0,0,1,1,0,0,0},
  {0,0,0,1,1,1,1,1,0,0,0,0}
};
uint8_t dibujoOk[8][12] = {
  {0,0,0,0,0,0,0,0,0,0,0,0},
  {0,0,0,0,0,0,0,0,0,1,0,0},
  {0,0,0,0,0,0,0,0,1,0,0,0},
  {0,0,0,0,0,0,0,1,0,0,0,0},
  {0,0,1,0,0,0,1,0,0,0,0,0},
  {0,0,0,1,0,1,0,0,0,0,0,0},
  {0,0,0,0,1,0,0,0,0,0,0,0},
  {0,0,0,0,0,0,0,0,0,0,0,0}
};
uint8_t dibujoDesactivado[8][12] = {
  {0,0,0,0,0,0,0,0,0,0,0,0},
  {0,0,0,0,0,0,0,0,0,0,0,0},
  {0,0,0,0,0,0,0,0,0,0,0,0},
  {0,0,0,1,1,1,1,1,1,0,0,0},
  {0,0,0,1,1,1,1,1,1,0,0,0},
  {0,0,0,0,0,0,0,0,0,0,0,0},
  {0,0,0,0,0,0,0,0,0,0,0,0},
  {0,0,0,0,0,0,0,0,0,0,0,0}
};

bool detectorActivo = true;      // Arranca vigilando
byte estadoActual = 255;         // 255 = "aun no se envio nada", fuerza el primer envio
unsigned long ultimoEnvio = 0;   // Para reenviar el estado cada 1 segundo

void mostrarEstado(byte estado) {
  if (estado == ESTADO_FUEGO) {
    matriz.renderBitmap(dibujoFuego, 8, 12);
    digitalWrite(LED_BUILTIN, HIGH);
    Serial.println("FUEGO detectado");
  } else if (estado == ESTADO_NORMAL) {
    matriz.renderBitmap(dibujoOk, 8, 12);
    digitalWrite(LED_BUILTIN, LOW);
    Serial.println("Normal, sin fuego");
  } else {
    matriz.renderBitmap(dibujoDesactivado, 8, 12);
    digitalWrite(LED_BUILTIN, LOW);
    Serial.println("Detector desactivado");
  }
}

void setup() {
  Serial.begin(9600);
  pinMode(PIN_LLAMA, INPUT);
  pinMode(LED_BUILTIN, OUTPUT);
  matriz.begin();

  if (!BLE.begin()) {                       // Enciende la radio Bluetooth del R4
    Serial.println("Error: no se pudo iniciar BLE");
    while (true);                           // Sin BLE no hay nada que hacer
  }

  BLE.setLocalName(NOMBRE_BLE);
  BLE.setAdvertisedService(servicioDetector);           // La app filtra por este UUID
  servicioDetector.addCharacteristic(caracteristicaComando);
  servicioDetector.addCharacteristic(caracteristicaEstado);
  BLE.addService(servicioDetector);

  caracteristicaComando.writeValue(CMD_ACTIVAR);        // Valores iniciales
  caracteristicaEstado.writeValue(ESTADO_NORMAL);

  BLE.advertise();                                      // "Estoy aqui"
  Serial.print("Anunciandose como ");
  Serial.println(NOMBRE_BLE);
}

void loop() {
  BLE.poll();   // Atiende los eventos BLE (conexiones, escrituras) sin bloquear el loop

  // 1) Recibir comandos de la app (validando: solo se aceptan 0 y 1)
  if (caracteristicaComando.written()) {
    byte comando = caracteristicaComando.value();
    if (comando == CMD_ACTIVAR) {
      detectorActivo = true;
    } else if (comando == CMD_DESACTIVAR) {
      detectorActivo = false;
    } else {
      Serial.print("Comando invalido ignorado: ");
      Serial.println(comando);
    }
  }

  // 2) Calcular el estado actual del detector
  byte nuevoEstado;
  if (!detectorActivo) {
    nuevoEstado = ESTADO_DESACTIVADO;
  } else if (digitalRead(PIN_LLAMA) == NIVEL_FUEGO) {
    nuevoEstado = ESTADO_FUEGO;
  } else {
    nuevoEstado = ESTADO_NORMAL;
  }

  // 3) Notificar a la app: de inmediato si cambio, y ademas cada 1 s para que la app siempre este al dia
  if (nuevoEstado != estadoActual) {
    estadoActual = nuevoEstado;
    mostrarEstado(estadoActual);
    caracteristicaEstado.writeValue(estadoActual);
    ultimoEnvio = millis();
  } else if (millis() - ultimoEnvio >= 1000) {
    caracteristicaEstado.writeValue(estadoActual);
    ultimoEnvio = millis();
  }
}
