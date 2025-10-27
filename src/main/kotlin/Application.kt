package com

import com.consultorioAPI.config.FirebaseConfig
import com.consultorioAPI.config.configureSecurity
import io.ktor.server.application.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.serialization.json.Json

fun main(args: Array<String>) {
    FirebaseConfig.init()
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
    configureSecurity()
    configureRouting()

    // TODO: Configure CORS
    // TODO: Configure StatusPages
    // TODO: Configure Koin
}
