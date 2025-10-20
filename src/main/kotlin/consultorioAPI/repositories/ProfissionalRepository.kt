package com.consultorioAPI.repositories

import com.consultorioAPI.models.Profissional

interface ProfissionalRepository {

    suspend fun salvar(profissional: Profissional): Profissional

    suspend fun atualizar(profissional: Profissional): Profissional

    suspend fun buscarPorId(id: String): Profissional?

    suspend fun buscarPorNome(nome: String): List<Profissional>

    suspend fun buscarPorEmail(email: String): Profissional?

    suspend fun buscarPorUserId(userID: String): Profissional?

    suspend fun buscarPorArea(area: String): List<Profissional>

    suspend fun listarTodos(): List<Profissional>

    suspend fun listarTodosAtivos(): List<Profissional>

}