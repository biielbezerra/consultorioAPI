package com.nutriAPI.repositories

import com.nutriAPI.models.Profissional

interface ProfissionalRepository {

    fun salvar(profissional: Profissional): Profissional

    fun buscarPorId(id: String): Profissional?

    fun buscarPorNome(nome: String): List<Profissional>

    fun buscarPorEmail(email: String): Profissional?

    fun buscarPorArea(area: String): List<Profissional>

    fun listarTodos(): List<Profissional>

}