package com.consultorioAPI.repositories.impl

import com.consultorioAPI.config.SupabaseConfig
import com.consultorioAPI.models.AreaAtuacao
import com.consultorioAPI.repositories.AreaAtuacaoRepository
import io.github.jan.supabase.postgrest.postgrest

class SupabaseAreaAtuacaoRepository : AreaAtuacaoRepository {
    private val client = SupabaseConfig.client
    private val table = client.postgrest["AreaAtuacao"]

    override suspend fun salvar(area: AreaAtuacao): AreaAtuacao {
        return table.upsert(area) {
            select()
        }.decodeSingle()
    }

    override suspend fun buscarPorId(id: String): AreaAtuacao? {
        return try {
            val response = table.select {
                filter {
                    eq("idArea", id)
                }
                limit(1)
            }
            response.decodeList<AreaAtuacao>().firstOrNull()
        } catch (e: Exception) {
            println("DEBUG [AreaAtuacaoRepo] - FALHA NA DECODIFICAÇÃO: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    override suspend fun listarTodas(): List<AreaAtuacao> {
        return table.select().decodeList()
    }

}