package com.consultorioAPI.repositories

import com.consultorioAPI.models.Profissional

interface ProfissionalRepository {

    fun salvar(profissional: Profissional): Profissional

    fun atualizar(profissional: Profissional): Profissional

    fun buscarPorId(id: String): Profissional?

    fun buscarPorNome(nome: String): List<Profissional>

    fun buscarPorUserId(userID: String): Profissional?

    fun buscarPorArea(area: String): List<Profissional>

    fun listarTodos(): List<Profissional>

}