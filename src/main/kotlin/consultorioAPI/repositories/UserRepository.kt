package com.consultorioAPI.repositories

import com.consultorioAPI.models.User

interface UserRepository {

    fun salvar(user: User): User

    fun buscarPorID(id: String): User?

    fun deletarPorId(id: String)

    fun buscarPorEmail(email: String): User?

}