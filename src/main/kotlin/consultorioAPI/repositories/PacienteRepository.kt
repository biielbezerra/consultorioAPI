package com.consultorioAPI.repositories

import com.consultorioAPI.models.Paciente

interface PacienteRepository {

    suspend fun salvar(paciente: Paciente): Paciente

    suspend fun atualizar(paciente: Paciente): Paciente

    suspend fun buscarPorId(id: String): Paciente?

    suspend fun buscarPorNome(nome: String): List<Paciente>

    suspend fun buscarPorUserId(userID: String): Paciente?

    suspend fun listarTodos(): List<Paciente>



}