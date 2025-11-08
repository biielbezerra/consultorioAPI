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
        return try {
            val response = table.select {
                filter {
                    eq("idConsultorio", id)
                }
                limit(1)
            }
            response.decodeList<Consultorio>().firstOrNull()
        } catch (e: Exception) {
            println("DEBUG [ConsultorioRepo] - FALHA NA DECODIFICAÇÃO: ${e.message}")
            e.printStackTrace()
            null
        }
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