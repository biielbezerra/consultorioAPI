package com.nutriAPI.repositories

import com.nutriAPI.models.Paciente

interface PacienteRepository {

    fun salvar(paciente: Paciente): Paciente

    fun buscarPorId(id: String): Paciente?

    fun buscarPorNome(nome: String): List<Paciente>

    fun buscarPorEmail(email: String): Paciente?

    fun listarTodos(): List<Paciente>

}