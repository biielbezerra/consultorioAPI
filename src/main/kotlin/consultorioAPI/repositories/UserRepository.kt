package com.consultorioAPI.repositories

import com.consultorioAPI.models.User

interface UserRepository {

    suspend fun salvar(user: User): User

    suspend fun atualizar(user: User): User

    suspend fun buscarPorId(id: String, incluirDeletados: Boolean = false): User?

    suspend fun deletarPorId(id: String)

    suspend fun buscarPorEmail(email: String): User?

    suspend fun buscarPorToken(token: String): User?

}