package com.consultorioAPI.repositories.impl

import com.consultorioAPI.config.SupabaseConfig
import com.consultorioAPI.models.Consultorio
import com.consultorioAPI.repositories.ConsultorioRepository
import io.github.jan.supabase.postgrest.postgrest

class SupabaseConsultorioRepository : ConsultorioRepository {

    private val client = SupabaseConfig.client
    private val table = client.postgrest["Consultorio"]

    override suspend fun salvar(consultorio: Consultorio): Consultorio {
        return table.upsert(consultorio) {
            select()
        }.decodeSingle()
    }

    override suspend fun buscarPorId(id: String): Consultorio? {
        return table.select {
            filter {
                eq("idConsultorio", id)
            }
        }.decodeAsOrNull<Consultorio>()
    }

    override suspend fun buscarPorNome(nome: String): List<Consultorio> {
        return table.select {
            filter {
                ilike("nomeConsultorio", "%${nome}%")
            }
        }.decodeList()
    }

    override suspend fun listarTodos(): List<Consultorio> {
        return table.select().decodeList()
    }
}