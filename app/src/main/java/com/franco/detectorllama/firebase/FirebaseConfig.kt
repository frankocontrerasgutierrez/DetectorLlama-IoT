package com.franco.detectorllama.firebase

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

// aca inicio firebase a mano, los datos los saque del google-services.json
// lo hice asi porque el plugin de google services me estaba dando problemas con gradle
object FirebaseConfig {
    // api key del proyecto
    const val API_KEY = "AIzaSyDAsOgU21QeCBh7f4U8k2vkFj10X9xMrL0"
    // id de la app android que registre en firebase
    const val APP_ID = "1:189176797250:android:2cfb4016dca1a828f535db"
    // id del proyecto
    const val PROJECT_ID = "detectorllama-e221d"
    // url de la realtime database
    const val DATABASE_URL = "https://detectorllama-e221d-default-rtdb.firebaseio.com"

    val estaConfigurado: Boolean
        get() = listOf(API_KEY, APP_ID, PROJECT_ID, DATABASE_URL).none { it == "PENDIENTE" }

    fun inicializar(context: Context) {
        if (!estaConfigurado) return
        if (FirebaseApp.getApps(context).isNotEmpty()) return   // si ya estaba iniciado no lo hago de nuevo
        val opciones = FirebaseOptions.Builder()
            .setApiKey(API_KEY)
            .setApplicationId(APP_ID)
            .setProjectId(PROJECT_ID)
            .setDatabaseUrl(DATABASE_URL)
            .build()
        FirebaseApp.initializeApp(context, opciones)
    }
}
