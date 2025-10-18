package com.consultorioAPI.repositories

import com.consultorioAPI.models.Paciente

interface PacienteRepository {

    fun salvar(paciente: Paciente): Paciente

    fun buscarPorId(id: String): Paciente?

    fun buscarPorNome(nome: String): List<Paciente>

    fun buscarPorEmail(email: String): Paciente?

    fun listarTodos(): List<Paciente>

}