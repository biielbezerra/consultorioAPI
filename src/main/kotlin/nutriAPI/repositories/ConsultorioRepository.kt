package com.nutriAPI.repositories

import com.nutriAPI.models.Consultorio

interface ConsultorioRepository {

    fun salvar(consultorio: Consultorio): Consultorio

    fun buscarPorId(id: String): Consultorio?

    fun buscarPorNome(nome: String): List<Consultorio>

    fun listarTodos(): List<Consultorio>

}