package com.consultorioAPI.config

import io.github.cdimascio.dotenv.dotenv
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.cio.CIO
import kotlinx.datetime.TimeZone

val fusoHorarioPadrao = TimeZone.UTC

object SupabaseConfig {

    private val dotenv = dotenv()

    private val SUPABASE_URL = dotenv["SUPABASE_URL"]
        ?: throw IllegalStateException("Variável 'SUPABASE_URL' não definida no .env.")

    private val SUPABASE_SERVICE_KEY = dotenv["SUPABASE_SERVICE_KEY"]
        ?: throw IllegalStateException("Variável 'SUPABASE_SERVICE_KEY' não definida no .env.")

    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_SERVICE_KEY
    ) {
        httpEngine = CIO.create()

        install(Postgrest) {

        }

        // install(Realtime) { ... }
    }
}