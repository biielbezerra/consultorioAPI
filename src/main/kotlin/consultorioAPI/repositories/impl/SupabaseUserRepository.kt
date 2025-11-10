package com.consultorioAPI.repositories.impl

import com.consultorioAPI.config.SupabaseConfig
import com.consultorioAPI.models.User
import com.consultorioAPI.repositories.UserRepository
import io.github.jan.supabase.postgrest.postgrest
import org.slf4j.LoggerFactory

class SupabaseUserRepository : UserRepository {

    private val log = LoggerFactory.getLogger(javaClass)
    private val client = SupabaseConfig.client
    private val table = client.postgrest["User"]

    override suspend fun salvar(user: User): User {
        log.debug("Salvando User: ${user.idUsuario}")
        return table.upsert(user) {
            select()
        }.decodeSingle()
    }

    override suspend fun atualizar(user: User): User {
        log.debug("Atualizando User: ${user.idUsuario}")
        return table.update(user) {
            select()
            filter {
                eq("idUsuario", user.idUsuario)
            }
        }.decodeSingle()
    }

    override suspend fun buscarPorId(id: String, incluirDeletados: Boolean): User? {
        log.debug("Buscando usuário com id: $id, incluirDeletados: $incluirDeletados")

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
            log.debug("Resultado da busca específica: $result")
            result
        } catch (e: Exception) {
            log.error("FALHA NA DECODIFICAÇÃO (buscarPorId): ${e.message}", e)
            null
        }
    }


    override suspend fun buscarPorEmail(email: String): User? {
        log.debug("Buscando usuário com email: $email")
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
            log.error("FALHA NA DECODIFICAÇÃO (buscarPorEmail): ${e.message}", e)
            null
        }
    }

    override suspend fun buscarPorToken(token: String): User? {
        log.debug("Buscando usuário com token: $token")
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
            log.error("FALHA NA DECODIFICAÇÃO (buscarPorToken): ${e.message}", e)
            null
        }
    }

    override suspend fun deletarPorId(id: String) {
        log.debug("Deletando User: $id")
        table.delete {
            filter {
                eq("idUsuario", id)
            }
        }
    }
}