package com.consultorioAPI.repositories

import com.consultorioAPI.models.AreaAtuacao
import com.consultorioAPI.models.Profissional

interface ProfissionalRepository {

    suspend fun salvar(profissional: Profissional): Profissional

    suspend fun atualizar(profissional: Profissional): Profissional

    suspend fun buscarPorId(id: String): Profissional?

    suspend fun buscarPorNome(nome: String): List<Profissional>

    suspend fun buscarPorUserId(userId: String): Profissional?

    suspend fun buscarPorArea(areaId: String): List<Profissional>

    suspend fun listarTodos(): List<Profissional>

    suspend fun listarTodosAtivos(): List<Profissional>

    suspend fun buscarPorToken(token: String): Profissional?

    suspend fun deletarPorId(id: String)

}