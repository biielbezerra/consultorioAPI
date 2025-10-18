package com.consultorioAPI.config

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.cio.CIO


object SupabaseConfig {

    private const val SUPABASE_URL = "https://moprznbedfwcecjefkyr.supabase.co"
    private const val SUPABASE_SERVICE_KEY =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1vcHJ6bmJlZGZ3Y2VjamVma3lyIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc2MDc0NjI1OSwiZXhwIjoyMDc2MzIyMjU5fQ.kRSwKJMGhYWLc4xLPa-M0g57_WDlFGKcDFfThXE_v9Y"

    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_SERVICE_KEY
    ) {
        httpEngine = CIO.create()

        // Instala o módulo de Autenticação (para o Passo 4)
        install(Auth) {
            // Configurações de autenticação, se necessário (ex: autoRefreshToken = true)
        }

        install(Postgrest) {
            // Configurações do Postgrest, se necessário (ex: defaultSchema = "public")
        }

        // install(Realtime) { ... }
    }
}