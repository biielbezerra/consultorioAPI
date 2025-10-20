package com.consultorioAPI.repositories.impl

import com.consultorioAPI.config.SupabaseConfig
import com.consultorioAPI.models.Recepcionista
import com.consultorioAPI.repositories.RecepcionistaRepository
import io.github.jan.supabase.postgrest.postgrest

class SupabaseRecepcionistaRepository : RecepcionistaRepository {

    private val client = SupabaseConfig.client
    private val table = client.postgrest["Recepcionista"]

    override suspend fun salvar(recepcionista: Recepcionista): Recepcionista {
        return table.upsert(recepcionista) {
            select()
        }.decodeSingle()
    }

    override suspend fun atualizar(recepcionista: Recepcionista): Recepcionista {
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
        return table.select {
            filter {
                eq("idRecepcionista", id)
            }
        }.decodeAsOrNull<Recepcionista>()
    }

    override suspend fun buscarPorUserId(userId: String): Recepcionista? {
        return table.select {
            filter {
                eq("userId", userId)
            }
        }.decodeAsOrNull<Recepcionista>()
    }

    override suspend fun buscarPorNome(nome: String): List<Recepcionista> {
        return table.select {
            filter {
                ilike("nomeRecepcionista", "%${nome}%")
            }
        }.decodeList()
    }

    override suspend fun listarTodos(): List<Recepcionista> {
        return table.select().decodeList()
    }
}