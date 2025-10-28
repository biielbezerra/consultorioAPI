package com.consultorioAPI.config

import com.auth0.jwk.JwkProviderBuilder
import com.consultorioAPI.models.StatusUsuario
import com.consultorioAPI.models.User
import com.consultorioAPI.repositories.UserRepository
import com.google.firebase.auth.FirebaseAuth
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import org.koin.ktor.ext.inject
import java.net.URL
import java.util.concurrent.TimeUnit

data class FirebasePrincipal(val uid: String, val email: String?)

fun Application.configureSecurity() {

    val userRepository by inject<UserRepository>()

    val firebaseProjectId = environment.config.property("ktor.firebase.projectId").getString()
    val firebaseIssuer = "https://securetoken.google.com/$firebaseProjectId"

    val jwkProvider = JwkProviderBuilder(URL("https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com"))
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()

    install(Authentication) {

        jwt("auth-firebase-token") {
            verifier(jwkProvider, firebaseIssuer)
            validate { credential ->
                val uid = credential.subject ?: return@validate null
                val email = credential.getClaim("email", String::class)
                FirebasePrincipal(uid, email)
            }
        }

        jwt("auth-firebase-user") {
            verifier(jwkProvider, firebaseIssuer)
            validate { credential ->
                val uid = credential.subject ?: return@validate null
                val user = userRepository.buscarPorId(uid, incluirDeletados = false)
                if (user != null && user.status == StatusUsuario.ATIVO) {
                    user
                } else {
                    null
                }
            }
        }
    }
}