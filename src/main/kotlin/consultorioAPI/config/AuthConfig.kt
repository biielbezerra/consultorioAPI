package com.consultorioAPI.config

import com.google.firebase.auth.FirebaseAuth
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.http.auth.*

data class FirebasePrincipal(
    val uid: String,
    val email: String?
)

fun Application.configureSecurity() {
    install(Authentication) {
        jwt("auth-firebase") {
            realm = "Consultorio API Access"

            validate { credential ->
                val token = credential.payload.toString()
                if (token.isBlank()) {
                    return@validate null
                }

                try {
                    val decodedToken = FirebaseAuth.getInstance().verifyIdToken(token)

                    FirebasePrincipal(decodedToken.uid, decodedToken.email)
                } catch (e: Exception) {
                    // Token inválido, expirado ou com assinatura errada
                    println("Erro na validação do token: ${e.message}")
                    null
                }
            }
        }
    }
}