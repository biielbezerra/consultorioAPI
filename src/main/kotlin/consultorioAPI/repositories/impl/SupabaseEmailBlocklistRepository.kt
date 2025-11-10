package com.consultorioAPI.repositories.impl

import com.consultorioAPI.config.SupabaseConfig
import com.consultorioAPI.repositories.EmailBlocklistRepository
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

@Serializable
private data class BlockedEmail(val email: String)

class SupabaseEmailBlocklistRepository : EmailBlocklistRepository {

    private val log = LoggerFactory.getLogger(javaClass)
    private val client = SupabaseConfig.client
    private val table = client.postgrest["EmailBlocklist"]

    override suspend fun salvar(email: String) {
        log.debug("Salvando email na blocklist: $email")
        table.upsert(BlockedEmail(email = email))
    }

    override suspend fun buscarPorEmail(email: String): String? {
        log.debug("Buscando email na blocklist: $email")
        val result = try {
            val response = table.select {
                filter {
                    eq("email", email)
                }
                limit(1)
            }
            response.decodeList<BlockedEmail>().firstOrNull()
        } catch (e: Exception) {
            log.error("FALHA NA DECODIFICAÇÃO (buscarPorEmail): ${e.message}", e)
            null
        }
        return result?.email
    }

    override suspend fun deletarPorEmail(email: String) {
        log.debug("Deletando email da blocklist: $email")
        table.delete {
            filter {
                eq("email", email)
            }
        }
    }

    override suspend fun listarTodos(): List<String> {
        log.debug("Listando todos os emails da blocklist")
        val results = table.select().decodeList<BlockedEmail>()
        return results.map { it.email }
    }

}