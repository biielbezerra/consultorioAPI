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
import org.slf4j.LoggerFactory

data class FirebasePrincipal(val uid: String, val email: String?, val name: String?)

fun Application.configureSecurity() {

    val log = LoggerFactory.getLogger("com.consultorioAPI.config.SecurityConfig")
    val userRepository by inject<UserRepository>()
    val dotenv = dotenv()

    val firebaseProjectId = dotenv["FIREBASE_PROJECT_ID"] ?: ""
    System.setProperty("FIREBASE_PROJECT_ID", firebaseProjectId)

    val firebaseIssuer = "https://securetoken.google.com/$firebaseProjectId"

    log.debug("Configurando segurança. Firebase Project ID: $firebaseProjectId")
    log.debug("Firebase Issuer: $firebaseIssuer")

    // Tente buscar as chaves manualmente para debug
    try {
        val jwksUrl = "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com"
        log.debug("Tentando buscar JWKs de: $jwksUrl")
        val connection = URL(jwksUrl).openConnection()
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        val response = connection.getInputStream().bufferedReader().readText()
        log.debug("JWKs Response: ${response.take(200)}...")
    } catch (e: Exception) {
        log.error("ERRO ao buscar JWKs: ${e.message}", e)
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
                log.debug("[auth-firebase-token] - Entrou no validate")
                log.trace("[auth-firebase-token] - Subject: ${credential.subject}")
                log.trace("[auth-firebase-token] - Issuer: ${credential.issuer}")

                // Valida issuer manualmente
                if (credential.issuer != firebaseIssuer) {
                    log.warn("[auth-firebase-token] - Issuer inválido: ${credential.issuer} != $firebaseIssuer")
                    return@validate null
                }

                val uid = credential.subject ?: return@validate null
                val email = credential.getClaim("email", String::class)
                val name = credential.getClaim("name", String::class)

                log.debug("[auth-firebase-token] - Token validado: uid=$uid, email=$email")

                FirebasePrincipal(uid, email, name)
            }

            challenge { defaultScheme, realm ->
                log.warn("[auth-firebase-token] - CHALLENGE chamado. Token inválido/ausente.")
                call.respondText("Não autorizado - Token", status = HttpStatusCode.Unauthorized)
            }
        }

        jwt("auth-firebase-user") {
            authHeader { call ->
                try {
                    val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")
                    log.debug("Token recebido: ${token?.take(50)}...")

                    if (token != null) {
                        val parts = token.split(".")
                        if (parts.size == 3) {
                            val payload = String(java.util.Base64.getUrlDecoder().decode(parts[1]))
                            log.trace("Payload do token: $payload")
                        }
                    }

                    call.request.parseAuthorizationHeader()
                } catch (e: Exception) {
                    log.error("Erro ao processar header: ${e.message}", e)
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
                log.debug("[auth-firebase-user] - Entrou no validate")

                // Valida issuer manualmente
                if (credential.issuer != firebaseIssuer) {
                    log.warn("[auth-firebase-user] - Issuer inválido")
                    return@validate null
                }

                val uid = credential.subject ?: return@validate null

                log.debug("[auth-firebase-user] - Buscando usuário: $uid")

                val user = userRepository.buscarPorId(uid, incluirDeletados = false)

                log.debug("[auth-firebase-user] - Usuário encontrado: ${user != null}")
                if (user != null) {
                    log.debug("[auth-firebase-user] - Status: ${user.status}")
                }

                if (user != null && user.status == StatusUsuario.ATIVO) {
                    user
                } else {
                    log.warn("[auth-firebase-user] - Validação falhou. User nulo ou status não ATIVO.")
                    null
                }
            }

            challenge { defaultScheme, realm ->
                log.warn("[auth-firebase-user] - CHALLENGE chamado. Token válido, mas usuário não autorizado.")
                call.respondText("Não autorizado - User", status = HttpStatusCode.Unauthorized)
            }
        }
    }
}