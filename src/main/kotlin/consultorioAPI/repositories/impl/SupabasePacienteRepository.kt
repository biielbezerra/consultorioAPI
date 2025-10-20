package com.consultorioAPI.repositories.impl

import com.consultorioAPI.config.SupabaseConfig
import com.consultorioAPI.models.Paciente
import com.consultorioAPI.repositories.PacienteRepository
import io.github.jan.supabase.postgrest.postgrest

class SupabasePacienteRepository : PacienteRepository {

    private val client = SupabaseConfig.client
    private val table = client.postgrest["Paciente"]

    override suspend fun salvar(paciente: Paciente): Paciente {
        return table.upsert(paciente) {
            select()
        }.decodeSingle()
    }

    override suspend fun atualizar(paciente: Paciente): Paciente {
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
        return table.select {
            filter {
                eq("idPaciente", id)
            }
        }.decodeAsOrNull<Paciente>()
    }

    override suspend fun buscarPorNome(nome: String): List<Paciente> {
        return table.select {
            filter {
                ilike("nomePaciente", "%${nome}%")
            }
        }.decodeList()
    }

    override suspend fun buscarPorUserId(userId: String): Paciente? {
        return table.select {
            filter {
                eq("userId", userId)
            }
        }.decodeAsOrNull<Paciente>()
    }

    override suspend fun listarTodos(): List<Paciente> {
        return table.select().decodeList()
    }
}