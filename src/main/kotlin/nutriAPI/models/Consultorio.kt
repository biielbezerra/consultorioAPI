package com.nutriAPI.models

import java.time.LocalDateTime
import java.util.UUID

class Consultorio(
    val idConsultorio: String = UUID.randomUUID().toString(),
    val nomeConsultorio: String
) {

    val listaPacientes = mutableListOf<Paciente>()
    val listaProfissionais = mutableListOf<Profissional>()
    val listaConsultas = mutableListOf<Consulta>()


    fun buscarProfissionaisPorArea(area: String): List<Profissional> {
        return listaProfissionais.filter { it.areaAtuacao == area }
    }

    fun adicionarProfissional(nome: String, email: String, senha: String, areaAtuacao: String): Profissional {
        val novaAgenda = Agenda(
            mutableListOf(),
            mutableListOf()
        )
        val novoProfissional = Profissional(
            nomeProfissional = nome,
            email = email,
            senha = senha,
            areaAtuacao = areaAtuacao,
            agenda = novaAgenda
        )
        listaProfissionais.add(novoProfissional)
        return novoProfissional
    }

    fun adicionarNutri(nome: String, email: String, senha: String): Nutricionista {
        val profissional = adicionarProfissional(nome, email, senha, "Nutrição")

        val nutricionista = Nutricionista(
            nomeNutri = profissional.nomeProfissional,
            email = profissional.email,
            senha = profissional.senha,
            agenda = profissional.agenda
        )
        return nutricionista
    }


    fun adicionarPaciente(nome: String, email: String, senha: String): Paciente {
        val novaAgenda = Agenda(
            mutableListOf(),
            mutableListOf()
        )
        val novoPaciente = Paciente(
            nomePaciente = nome,
            email = email,
            senha = senha,
            agenda = novaAgenda
        )
        novoPaciente.dataCadastro = LocalDateTime.now()
        listaPacientes.add(novoPaciente)
        return novoPaciente
    }


}