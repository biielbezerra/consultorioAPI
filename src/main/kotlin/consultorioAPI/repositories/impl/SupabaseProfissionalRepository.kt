package com.consultorioAPI.repositories.impl

import com.consultorioAPI.config.SupabaseConfig
import com.consultorioAPI.models.Paciente
import com.consultorioAPI.models.Profissional
import com.consultorioAPI.models.StatusUsuario
import com.consultorioAPI.repositories.ProfissionalRepository
import io.github.jan.supabase.postgrest.postgrest
import org.slf4j.LoggerFactory

class SupabaseProfissionalRepository : ProfissionalRepository {

    private val log = LoggerFactory.getLogger(javaClass)
    private val client = SupabaseConfig.client
    private val table = client.postgrest["Profissional"]

    override suspend fun salvar(profissional: Profissional): Profissional {
        log.debug("Salvando Profissional: ${profissional.idProfissional} para User: ${profissional.userId}")
        return table.upsert(profissional) {
            select()
        }.decodeSingle()
    }

    override suspend fun atualizar(profissional: Profissional): Profissional {
        log.debug("Atualizando Profissional: ${profissional.idProfissional}")
        return table.update(
            value = profissional
        ) {
            select()
            filter {
                eq("idProfissional", profissional.idProfissional)
            }
        }.decodeSingle()
    }

    override suspend fun buscarPorId(id: String): Profissional? {
        log.debug("Buscando Profissional por idProfissional: $id")
        return try {
            val response = table.select {
                filter {
                    eq("idProfissional", id)
                    eq("isDeletado", false)
                }
                limit(1)
            }
            response.decodeList<Profissional>().firstOrNull()
        } catch (e: Exception) {
            log.error("FALHA NA DECODIFICAÇÃO (buscarPorId): ${e.message}", e)
            null
        }
    }

    override suspend fun buscarPorNome(nome: String): List<Profissional> {
        log.debug("Buscando Profissional por nome: $nome")
        return table.select {
            filter {
                ilike("nomeProfissional", "%${nome}%")
                eq("isDeletado", false)
            }
        }.decodeList()
    }

    override suspend fun buscarPorUserId(userId: String): Profissional? {
        log.debug("Buscando Profissional por userId: $userId")
        return try {
            val response = table.select {
                filter {
                    eq("userId", userId)
                    eq("isDeletado", false)
                }
                limit(1)
            }
            response.decodeList<Profissional>().firstOrNull()
        } catch (e: Exception) {
            log.error("FALHA NA DECODIFICAÇÃO (buscarPorUserId): ${e.message}", e)
            null
        }
    }

    override suspend fun buscarPorArea(areaId: String): List<Profissional> {
        log.debug("Buscando Profissionais por areaId: $areaId")
        return table.select {
            filter {
                eq("areaAtuacaoId", areaId)
                eq("isDeletado", false)
            }
        }.decodeList()
    }

    override suspend fun listarTodos(): List<Profissional> {
        log.debug("Listando todos os profissionais")
        return table.select {
            filter {
                eq("isDeletado", false)
            }
        }.decodeList()
    }

    override suspend fun listarTodosAtivos(): List<Profissional> {
        log.debug("Listando todos os profissionais ATIVOS")
        return table.select {
            filter {
                eq("status", StatusUsuario.ATIVO.name)
            }
        }.decodeList()
    }

    override suspend fun deletarPorId(id: String) {
        log.debug("Deletando Profissional: $id")
        table.delete {
            filter {
                eq("idProfissional", id)
            }
        }
    }

}