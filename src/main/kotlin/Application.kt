package com

import com.consultorioAPI.config.FirebaseConfig
import com.consultorioAPI.config.configureSecurity
import com.consultorioAPI.config.fusoHorarioPadrao
import com.consultorioAPI.services.ManutencaoService
import io.ktor.server.application.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject
import consultorioAPI.config.appModule
import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime

fun main(args: Array<String>) {
    val dotenv = dotenv()

    System.setProperty("FIREBASE_SERVICE_ACCOUNT_PATH", dotenv["FIREBASE_SERVICE_ACCOUNT_PATH"] ?: "")
    System.setProperty("SUPABASE_URL", dotenv["SUPABASE_URL"] ?: "")
    System.setProperty("SUPABASE_SERVICE_KEY", dotenv["SUPABASE_SERVICE_KEY"] ?: "")

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
        })
    }

    install(Koin) {
        slf4jLogger()
        modules(appModule)
    }

    configureSecurity()
    configureRouting()

    // TODO: Configure CORS
    // TODO: Configure StatusPages
}
