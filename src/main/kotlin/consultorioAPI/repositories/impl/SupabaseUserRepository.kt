package com.consultorioAPI.repositories.impl

import com.consultorioAPI.config.SupabaseConfig
import com.consultorioAPI.models.User
import com.consultorioAPI.repositories.UserRepository
import io.github.jan.supabase.postgrest.postgrest

class SupabaseUserRepository : UserRepository {

    private val client = SupabaseConfig.client
    private val table = client.postgrest["User"]

    override suspend fun salvar(user: User): User {
        return table.upsert(user) {
            select()
        }.decodeSingle()
    }

    override suspend fun atualizar(user: User): User {
        return table.update(
            value = user
        ) {
            select()
            filter {
                eq("idUsuario", user.idUsuario)
            }
        }.decodeSingle()
    }

    override suspend fun buscarPorId(id: String): User? {
        return table.select {
            filter {
                eq("idUsuario", id)
            }
        }.decodeAsOrNull<User>()
    }

    override suspend fun buscarPorEmail(email: String): User? {
        return table.select {
            filter {
                eq("email", email)
            }
        }.decodeAsOrNull<User>()
    }

    override suspend fun deletarPorId(id: String) {
        table.delete {
            filter {
                eq("idUsuario", id)
            }
        }
    }
}