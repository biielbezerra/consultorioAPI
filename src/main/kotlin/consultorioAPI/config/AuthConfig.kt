package com.consultorioAPI.config

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

val jwtSecret = System.getenv("SUPABASE_JWT_SECRET") ?: throw IllegalStateException("SUPABASE_JWT_SECRET não definida.")
val jwtIssuer = System.getenv("SUPABASE_URL") ?: "https://<SEU-ID-DE-PROJETO>.supabase.co"
val jwtAudience = "authenticated"
val jwtRealm = "Consultorio API"

fun Application.configureSecurity() {
    install(Authentication) {
        jwt("auth-jwt") {
            realm = jwtRealm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(jwtSecret))
                    .withAudience(jwtAudience)
                    .withIssuer(jwtIssuer)
                    .build()
            )
            validate { credential ->
                val userId = credential.payload.subject
                if (userId != null) {
                    UserIdPrincipal(userId)
                } else {
                    null
                }
            }
        }
    }
}

data class UserIdPrincipal(val userId: String) : Principal