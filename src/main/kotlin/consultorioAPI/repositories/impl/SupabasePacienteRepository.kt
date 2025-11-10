package com.consultorioAPI.repositories.impl

import com.consultorioAPI.config.SupabaseConfig
import com.consultorioAPI.models.Paciente
import com.consultorioAPI.repositories.PacienteRepository
import io.github.jan.supabase.postgrest.postgrest
import org.slf4j.LoggerFactory

class SupabasePacienteRepository : PacienteRepository {

    private val log = LoggerFactory.getLogger(javaClass)
    private val client = SupabaseConfig.client
    private val table = client.postgrest["Paciente"]

    override suspend fun salvar(paciente: Paciente): Paciente {
        log.debug("Salvando Paciente: ${paciente.idPaciente} para User: ${paciente.userId}")
        return table.upsert(paciente) {
            select()
        }.decodeSingle()
    }

    override suspend fun atualizar(paciente: Paciente): Paciente {
        log.debug("Atualizando Paciente: ${paciente.idPaciente}")
        return table.update(
            value = paciente
        ) {
            select()
            filter {
                eq("idPaciente", paciente.idPaciente)
            }
        }.decodeSingle()
    }

    override suspend fun buscarPorId(id: String): Paciente? {
        log.debug("Buscando Paciente por idPaciente: $id")
        return try {
            val response = table.select {
                filter {
                    eq("idPaciente", id)
                    eq("isDeletado", false)
                }
                limit(1)
            }
            response.decodeList<Paciente>().firstOrNull()
        } catch (e: Exception) {
            log.error("FALHA NA DECODIFICAÇÃO (buscarPorId): ${e.message}", e)
            null
        }
    }

    override suspend fun buscarPorNome(nome: String): List<Paciente> {
        log.debug("Buscando Paciente por nome: $nome")
        return table.select {
            filter {
                ilike("nomePaciente", "%${nome}%")
                eq("isDeletado", false)
            }
        }.decodeList()
    }

    override suspend fun buscarPorUserId(userId: String): Paciente? {
        log.debug("Buscando Paciente por userId: $userId")
        return try {
            val response = table.select {
                filter {
                    eq("userId", userId)
                    eq("isDeletado", false)
                }
                limit(1)
            }
            response.decodeList<Paciente>().firstOrNull()
        } catch (e: Exception) {
            log.error("FALHA NA DECODIFICAÇÃO (buscarPorUserId): ${e.message}", e) // ⬅️ MUDOU
            e.printStackTrace()
            null
        }
    }

    override suspend fun listarTodos(): List<Paciente> {
        log.debug("Listando todos os pacientes")
        return table.select {
            filter {
                eq("isDeletado", false)
            }
        }.decodeList()
    }

    override suspend fun deletarPorId(id: String) {
        log.debug("Deletando Paciente: $id")
        table.delete {
            filter {
                eq("idPaciente", id)
            }
        }
    }

}