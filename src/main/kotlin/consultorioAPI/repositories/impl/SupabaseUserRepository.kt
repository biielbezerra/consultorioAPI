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
        return table.update(user) {
            select()
            filter {
                eq("idUsuario", user.idUsuario)
            }
        }.decodeSingle()
    }

    override suspend fun buscarPorId(id: String, incluirDeletados: Boolean): User? {
        println("DEBUG [Repository] - Buscando usuário com id: $id, incluirDeletados: $incluirDeletados")

        return try {
            val response = table.select {
                filter {
                    // Verifique se a coluna é "id_usuario" no Supabase e ajuste aqui se necessário
                    eq("idUsuario", id)
                    if (!incluirDeletados) {
                        eq("isDeletado", false)
                    }
                }
                limit(1)
            }

            val users = response.decodeList<User>()
            val result = users.firstOrNull()
            println("DEBUG [Repository] - Resultado da busca específica: $result")
            result
        } catch (e: Exception) {
            println("DEBUG [Repository] - FALHA NA DECODIFICAÇÃO: ${e.message}")
            e.printStackTrace()
            null
        }
    }


    override suspend fun buscarPorEmail(email: String): User? {
        println("DEBUG [Repository] - Buscando usuário com email: $email")
        return try {
            val response = table.select {
                filter {
                    eq("email", email)
                    eq("isDeletado", false)
                }
                limit(1)
            }
            response.decodeList<User>().firstOrNull()
        } catch (e: Exception) {
            println("DEBUG [Repository] - FALHA NA DECODIFICAÇÃO (buscarPorEmail): ${e.message}")
            e.printStackTrace()
            null
        }
    }

    override suspend fun buscarPorToken(token: String): User? {
        println("DEBUG [Repository] - Buscando usuário com token: $token")
        return try {
            val response = table.select {
                filter {
                    eq("convite_token", token)
                    eq("isDeletado", false)
                }
                limit(1)
            }
            response.decodeList<User>().firstOrNull()
        } catch (e: Exception) {
            println("DEBUG [Repository] - FALHA NA DECODIFICAÇÃO (buscarPorToken): ${e.message}")
            e.printStackTrace()
            null
        }
    }

    override suspend fun deletarPorId(id: String) {
        table.delete {
            filter {
                eq("idUsuario", id)
            }
        }
    }
}