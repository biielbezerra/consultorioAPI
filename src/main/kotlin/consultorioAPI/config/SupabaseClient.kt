package com.consultorioAPI.config

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.cio.CIO
import kotlinx.datetime.TimeZone

val fusoHorarioPadrao = TimeZone.UTC

object SupabaseConfig {

    private val SUPABASE_URL = System.getenv("SUPABASE_URL")
        ?: throw IllegalStateException("Variável de ambiente SUPABASE_URL não definida.")

    private val SUPABASE_SERVICE_KEY = System.getenv("SUPABASE_SERVICE_KEY")
        ?: throw IllegalStateException("Variável de ambiente SUPABASE_SERVICE_KEY não definida.")

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