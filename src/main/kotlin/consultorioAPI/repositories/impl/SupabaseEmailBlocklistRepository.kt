package com.consultorioAPI.repositories.impl

import com.consultorioAPI.config.SupabaseConfig
import com.consultorioAPI.repositories.EmailBlocklistRepository
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable

@Serializable
private data class BlockedEmail(val email: String)

class SupabaseEmailBlocklistRepository : EmailBlocklistRepository {

    private val client = SupabaseConfig.client
    private val table = client.postgrest["EmailBlocklist"]

    override suspend fun salvar(email: String) {
        table.upsert(BlockedEmail(email = email))
    }

    override suspend fun buscarPorEmail(email: String): String? {
        val result = table.select {
            filter {
                eq("email", email)
            }
        }.decodeSingleOrNull<BlockedEmail>()
        return result?.email
    }

    override suspend fun deletarPorEmail(email: String) {
        table.delete {
            filter {
                eq("email", email)
            }
        }
    }

    override suspend fun listarTodos(): List<String> {
        val results = table.select().decodeList<BlockedEmail>()
        return results.map { it.email }
    }

}