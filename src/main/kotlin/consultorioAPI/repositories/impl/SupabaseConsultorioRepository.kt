package com.consultorioAPI.repositories.impl

import com.consultorioAPI.config.SupabaseConfig
import com.consultorioAPI.models.Consultorio
import com.consultorioAPI.repositories.ConsultorioRepository
import io.github.jan.supabase.postgrest.postgrest
import org.slf4j.LoggerFactory

class SupabaseConsultorioRepository : ConsultorioRepository {

    private val log = LoggerFactory.getLogger(javaClass)
    private val client = SupabaseConfig.client
    private val table = client.postgrest["Consultorio"]

    override suspend fun salvar(consultorio: Consultorio): Consultorio {
        log.debug("Salvando Consultorio: ${consultorio.nomeConsultorio}")
        return table.upsert(consultorio) {
            select()
        }.decodeSingle()
    }

    override suspend fun buscarPorId(id: String): Consultorio? {
        log.debug("Buscando Consultorio por idConsultorio: $id")
        return try {
            val response = table.select {
                filter {
                    eq("idConsultorio", id)
                }
                limit(1)
            }
            response.decodeList<Consultorio>().firstOrNull()
        } catch (e: Exception) {
            log.error("FALHA NA DECODIFICAÇÃO (buscarPorId): ${e.message}", e)
            null
        }
    }

    override suspend fun buscarPorNome(nome: String): List<Consultorio> {
        log.debug("Buscando Consultorio por nome: $nome")
        return table.select {
            filter {
                ilike("nomeConsultorio", "%${nome}%")
            }
        }.decodeList()
    }

    override suspend fun listarTodos(): List<Consultorio> {
        log.debug("Listando todos os consultórios")
        return table.select().decodeList()
    }
}