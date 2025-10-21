package com.consultorioAPI.repositories.impl

import com.consultorioAPI.config.SupabaseConfig
import com.consultorioAPI.models.Paciente
import com.consultorioAPI.models.Profissional
import com.consultorioAPI.models.StatusUsuario
import com.consultorioAPI.repositories.ProfissionalRepository
import io.github.jan.supabase.postgrest.postgrest

class SupabaseProfissionalRepository : ProfissionalRepository {

    private val client = SupabaseConfig.client
    private val table = client.postgrest["Profissional"]

    override suspend fun salvar(profissional: Profissional): Profissional {
        return table.upsert(profissional) {
            select()
        }.decodeSingle()
    }

    override suspend fun atualizar(profissional: Profissional): Profissional {
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
        return table.select {
            filter {
                eq("idProfissional", id)
                eq("isDeletado", false)
            }
        }.decodeAsOrNull<Profissional>()
    }

    override suspend fun buscarPorNome(nome: String): List<Profissional> {
        return table.select {
            filter {
                ilike("nomeProfissional", "%${nome}%")
                eq("isDeletado", false)
            }
        }.decodeList()
    }

    override suspend fun buscarPorUserId(userId: String): Profissional? {
        return table.select {
            filter {
                eq("userId", userId)
                eq("isDeletado", false)
            }
        }.decodeAsOrNull<Profissional>()
    }

    override suspend fun buscarPorArea(areaId: String): List<Profissional> {
        return table.select {
            filter {
                eq("areaAtuacaoId", areaId)
                eq("isDeletado", false)
            }
        }.decodeList()
    }

    override suspend fun listarTodos(): List<Profissional> {
        return table.select {
            filter {
                eq("isDeletado", false)
            }
        }.decodeList()
    }

    override suspend fun listarTodosAtivos(): List<Profissional> {
        return table.select {
            filter {
                eq("status", StatusUsuario.ATIVO.name)
            }
        }.decodeList()
    }

    override suspend fun buscarPorToken(token: String): Profissional? {
        return table.select{
            filter {
                eq("conviteToken", token)
            }
        }.decodeAsOrNull<Profissional>()
    }

    override suspend fun deletarPorId(id: String) {
        table.delete {
            filter {
                eq("idProfissional", id)
            }
        }
    }

}