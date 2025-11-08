package com

import com.consultorioAPI.config.FirebaseConfig
import com.consultorioAPI.config.configureSecurity
import com.consultorioAPI.config.configureStatusPages
import consultorioAPI.config.appModule
import io.github.cdimascio.dotenv.dotenv
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.openapi.*
import io.ktor.server.resources.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import kotlin.time.ExperimentalTime

fun main(args: Array<String>) {
    val dotenv = dotenv()

    System.setProperty("FIREBASE_SERVICE_ACCOUNT_PATH", dotenv["FIREBASE_SERVICE_ACCOUNT_PATH"] ?: "")
    System.setProperty("SUPABASE_URL", dotenv["SUPABASE_URL"] ?: "")
    System.setProperty("SUPABASE_SERVICE_KEY", dotenv["SUPABASE_SERVICE_KEY"] ?: "")
    System.setProperty("FIREBASE_PROJECT_ID", dotenv["FIREBASE_PROJECT_ID"] ?: "")

    FirebaseConfig.init()
    io.ktor.server.netty.EngineMain.main(args)
}

@OptIn(ExperimentalTime::class)
fun Application.module() {

    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        })
    }

    install(Koin) {
        slf4jLogger()
        modules(appModule)
    }

    install(CORS) {
        anyHost()

        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Get)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader("X-Cron-Secret")

        allowCredentials = true
        allowNonSimpleContentTypes = true

        // --- PARA PRODUÇÃO, use esta configuração em vez de 'anyHost()' ---
        // allowHost("meu-frontend.com", schemes = ["https"])
        // allowHost("www.meu-frontend.com", schemes = ["https"])
        // allowHost("localhost:3000") // Para desenvolvimento local do frontend
    }

    install(Resources)
    install(AutoHeadResponse)

    configureStatusPages()
    configureSecurity()
    configureRouting()

}
