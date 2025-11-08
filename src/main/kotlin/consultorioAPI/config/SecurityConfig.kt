package com.consultorioAPI.config

import com.auth0.jwk.JwkProviderBuilder
import com.consultorioAPI.models.StatusUsuario
import com.consultorioAPI.repositories.UserRepository
import io.github.cdimascio.dotenv.dotenv
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.http.*
import io.ktor.server.response.*
import org.koin.ktor.ext.inject
import java.net.URL
import java.util.concurrent.TimeUnit

data class FirebasePrincipal(val uid: String, val email: String?, val name: String?)

fun Application.configureSecurity() {

    val userRepository by inject<UserRepository>()
    val dotenv = dotenv()

    val firebaseProjectId = dotenv["FIREBASE_PROJECT_ID"] ?: ""
    System.setProperty("FIREBASE_PROJECT_ID", firebaseProjectId)

    val firebaseIssuer = "https://securetoken.google.com/$firebaseProjectId"

    println("DEBUG - Firebase Project ID: $firebaseProjectId")
    println("DEBUG - Firebase Issuer: $firebaseIssuer")

    // Tente buscar as chaves manualmente para debug
    try {
        val jwksUrl = "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com"
        println("DEBUG - Tentando buscar JWKs de: $jwksUrl")
        val connection = URL(jwksUrl).openConnection()
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        val response = connection.getInputStream().bufferedReader().readText()
        println("DEBUG - JWKs Response: ${response.take(200)}...")
    } catch (e: Exception) {
        println("DEBUG - ERRO ao buscar JWKs: ${e.message}")
        e.printStackTrace()
    }

    val jwkProvider = JwkProviderBuilder(URL("https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com"))
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()

    install(Authentication) {

        jwt("auth-firebase-token") {
            verifier(jwkProvider, firebaseIssuer) {
                acceptLeeway(10) // Aumentei para 10 segundos
                withAudience(firebaseProjectId)
                // Não valida issuer aqui, vamos validar manualmente
                acceptIssuedAt(10)
                acceptExpiresAt(10)
            }

            realm = "Consultorio API"

            validate { credential ->
                println("DEBUG [auth-firebase-token] - Entrou no validate")
                println("DEBUG [auth-firebase-token] - Subject: ${credential.subject}")
                println("DEBUG [auth-firebase-token] - Issuer: ${credential.issuer}")
                println("DEBUG [auth-firebase-token] - Audience: ${credential.audience}")

                // Valida issuer manualmente
                if (credential.issuer != firebaseIssuer) {
                    println("DEBUG [auth-firebase-token] - Issuer inválido: ${credential.issuer} != $firebaseIssuer")
                    return@validate null
                }

                val uid = credential.subject ?: return@validate null
                val email = credential.getClaim("email", String::class)
                val name = credential.getClaim("name", String::class)

                println("DEBUG [auth-firebase-token] - Token validado: uid=$uid, email=$email")

                FirebasePrincipal(uid, email, name)
            }

            challenge { defaultScheme, realm ->
                println("DEBUG [auth-firebase-token] - CHALLENGE chamado")
                call.respondText("Não autorizado - Token", status = HttpStatusCode.Unauthorized)
            }
        }

        jwt("auth-firebase-user") {
            authHeader { call ->
                try {
                    val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")
                    println("DEBUG - Token recebido: ${token?.take(50)}...")

                    if (token != null) {
                        val parts = token.split(".")
                        if (parts.size == 3) {
                            val payload = String(java.util.Base64.getUrlDecoder().decode(parts[1]))
                            println("DEBUG - Payload do token: $payload")
                        }
                    }

                    call.request.parseAuthorizationHeader()
                } catch (e: Exception) {
                    println("DEBUG - Erro ao processar header: ${e.message}")
                    e.printStackTrace()
                    null
                }
            }

            verifier(jwkProvider, firebaseIssuer) {
                acceptLeeway(10)
                withAudience(firebaseProjectId)
                acceptIssuedAt(10)
                acceptExpiresAt(10)
            }

            realm = "Consultorio API"

            validate { credential ->
                println("DEBUG [auth-firebase-user] - Entrou no validate")

                // Valida issuer manualmente
                if (credential.issuer != firebaseIssuer) {
                    println("DEBUG [auth-firebase-user] - Issuer inválido")
                    return@validate null
                }

                val uid = credential.subject ?: return@validate null

                println("DEBUG [auth-firebase-user] - Buscando usuário: $uid")

                val user = userRepository.buscarPorId(uid, incluirDeletados = false)

                println("DEBUG [auth-firebase-user] - Usuário encontrado: ${user != null}")
                if (user != null) {
                    println("DEBUG [auth-firebase-user] - Status: ${user.status}")
                }

                if (user != null && user.status == StatusUsuario.ATIVO) {
                    user
                } else {
                    println("DEBUG [auth-firebase-user] - Validação falhou")
                    null
                }
            }

            challenge { defaultScheme, realm ->
                println("DEBUG [auth-firebase-user] - CHALLENGE chamado")
                call.respondText("Não autorizado - User", status = HttpStatusCode.Unauthorized)
            }
        }
    }
}