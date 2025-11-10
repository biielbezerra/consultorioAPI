package com.consultorioAPI.repositories.impl

import com.consultorioAPI.config.SupabaseConfig
import com.consultorioAPI.models.Profissional
import com.consultorioAPI.models.Recepcionista
import com.consultorioAPI.repositories.RecepcionistaRepository
import io.github.jan.supabase.postgrest.postgrest
import org.slf4j.LoggerFactory


class SupabaseRecepcionistaRepository : RecepcionistaRepository {

    private val log = LoggerFactory.getLogger(javaClass)
    private val client = SupabaseConfig.client
    private val table = client.postgrest["Recepcionista"]

    override suspend fun salvar(recepcionista: Recepcionista): Recepcionista {
        log.debug("Salvando Recepcionista: ${recepcionista.idRecepcionista} para User: ${recepcionista.userId}")
        return table.upsert(recepcionista) {
            select()
        }.decodeSingle()
    }

    override suspend fun atualizar(recepcionista: Recepcionista): Recepcionista {
        log.debug("Atualizando Recepcionista: ${recepcionista.idRecepcionista}")
        return table.update(
            value = recepcionista
        ) {
            select()
            filter {
                eq("idRecepcionista", recepcionista.idRecepcionista)
            }
        }.decodeSingle()
    }

    override suspend fun buscarPorId(id: String): Recepcionista? {
        log.debug("Buscando Recepcionista por idRecepcionista: $id")
        return try {
            val response = table.select {
                filter {
                    eq("idRecepcionista", id)
                    eq("isDeletado", false)
                }
                limit(1)
            }
            response.decodeList<Recepcionista>().firstOrNull()
        } catch (e: Exception) {
            log.error("FALHA NA DECODIFICAÇÃO (buscarPorId): ${e.message}", e)
            null
        }
    }

    override suspend fun buscarPorUserId(userId: String): Recepcionista? {
        log.debug("Buscando Recepcionista por userId: $userId")
        return try {
            val response = table.select {
                filter {
                    eq("userId", userId)
                    eq("isDeletado", false)
                }
                limit(1)
            }
            response.decodeList<Recepcionista>().firstOrNull()
        } catch (e: Exception) {
            log.error("FALHA NA DECODIFICAÇÃO (buscarPorUserId): ${e.message}", e)
            null
        }
    }

    override suspend fun buscarPorNome(nome: String): List<Recepcionista> {
        log.debug("Buscando Recepcionista por nome: $nome")
        return table.select {
            filter {
                ilike("nomeRecepcionista", "%${nome}%")
                eq("isDeletado", false)
            }
        }.decodeList()
    }

    override suspend fun listarTodos(): List<Recepcionista> {
        log.debug("Listando todas as recepcionistas")
        return table.select {
            filter {
                eq("isDeletado", false)
            }
        }.decodeList()
    }

    override suspend fun deletarPorId(id: String) {
        log.debug("Deletando Recepcionista: $id")
        table.delete {
            filter {
                eq("idRecepcionista", id)
            }
        }
    }

}