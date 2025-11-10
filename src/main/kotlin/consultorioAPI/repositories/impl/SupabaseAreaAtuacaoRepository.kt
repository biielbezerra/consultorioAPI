package com.consultorioAPI.repositories.impl

import com.consultorioAPI.config.SupabaseConfig
import com.consultorioAPI.models.AreaAtuacao
import com.consultorioAPI.repositories.AreaAtuacaoRepository
import io.github.jan.supabase.postgrest.postgrest
import org.slf4j.LoggerFactory

class SupabaseAreaAtuacaoRepository : AreaAtuacaoRepository {

    private val log = LoggerFactory.getLogger(javaClass)
    private val client = SupabaseConfig.client
    private val table = client.postgrest["AreaAtuacao"]

    override suspend fun salvar(area: AreaAtuacao): AreaAtuacao {
        log.debug("Salvando AreaAtuacao: ${area.nome}")
        return table.upsert(area) {
            select()
        }.decodeSingle()
    }

    override suspend fun buscarPorId(id: String): AreaAtuacao? {
        log.debug("Buscando AreaAtuacao por idArea: $id")
        return try {
            val response = table.select {
                filter {
                    eq("idArea", id)
                }
                limit(1)
            }
            response.decodeList<AreaAtuacao>().firstOrNull()
        } catch (e: Exception) {
            log.error("FALHA NA DECODIFICAÇÃO (buscarPorId): ${e.message}", e)
            null
        }
    }

    override suspend fun listarTodas(): List<AreaAtuacao> {
        log.debug("Listando todas as AreaAtuacao")
        return table.select().decodeList()
    }

}