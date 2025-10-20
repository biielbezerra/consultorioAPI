package com.consultorioAPI.repositories

import com.consultorioAPI.models.Consultorio

interface ConsultorioRepository {

    suspend fun salvar(consultorio: Consultorio): Consultorio

    suspend fun buscarPorId(id: String): Consultorio?

    suspend fun buscarPorNome(nome: String): List<Consultorio>

    suspend fun listarTodos(): List<Consultorio>

}