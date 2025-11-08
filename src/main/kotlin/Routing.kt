package com

import com.consultorioAPI.auth.authRoutes
import com.consultorioAPI.routes.*
import consultorioAPI.routes.adminRoutes
import consultorioAPI.routes.consultaRoutes
import consultorioAPI.routes.profissionalRoutes
import consultorioAPI.routes.publicCronRoutes
import consultorioAPI.routes.usuarioRoutes
import io.ktor.server.application.*
import io.ktor.server.plugins.openapi.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class OnboardingRequest(val nome: String)

fun Application.configureRouting() {

    routing {
        openAPI(path = "openapi", swaggerFile = "openapi/documentation.yaml")
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")

        get("/") {
            call.respondText("API do Consultório v1.0 - OK")
        }

        authRoutes()
        usuarioRoutes()
        consultaRoutes()
        profissionalRoutes()
        pacienteRoutes()
        adminRoutes()
        publicCronRoutes()
    }
}