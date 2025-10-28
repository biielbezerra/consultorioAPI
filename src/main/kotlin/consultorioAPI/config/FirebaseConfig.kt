package com.consultorioAPI.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import io.github.cdimascio.dotenv.dotenv
import java.io.FileInputStream
import java.io.ByteArrayInputStream

object FirebaseConfig {

    fun init() {
        if (FirebaseApp.getApps().isNotEmpty()) {
            println("Firebase Admin SDK já inicializado.")
            return
        }
        val serviceAccountJson = dotenv()["FIREBASE_SERVICE_ACCOUNT_JSON"]
        val serviceAccountPath = dotenv()["FIREBASE_SERVICE_ACCOUNT_PATH"]

        val credentials = when {
            serviceAccountJson != null && serviceAccountJson.isNotBlank() -> {
                println("Inicializando Firebase via JSON String (Produção)...")
                val stream = ByteArrayInputStream(serviceAccountJson.toByteArray(Charsets.UTF_8))
                GoogleCredentials.fromStream(stream)
            }
            serviceAccountPath != null && serviceAccountPath.isNotBlank() -> {
                println("Inicializando Firebase via Path (Dev Local)...")
                val stream = FileInputStream(serviceAccountPath)
                GoogleCredentials.fromStream(stream)
            }
            else -> {
                throw IllegalStateException("Variável de ambiente 'FIREBASE_SERVICE_ACCOUNT_JSON' ou 'FIREBASE_SERVICE_ACCOUNT_PATH' não definida.")
            }
        }

        val options = FirebaseOptions.builder()
            .setCredentials(credentials)
            .build()

        FirebaseApp.initializeApp(options)
        println("Firebase Admin SDK inicializado com sucesso.")
    }
}