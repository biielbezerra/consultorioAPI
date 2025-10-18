package com.nutriAPI.models

import com.nutriAPI.services.AgendaService
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

class Consultorio(
    val idConsultorio: String = UUID.randomUUID().toString(),
    val nomeConsultorio: String,
    val endereco: String
) {

    val listaConsultorio = mutableListOf<Consultorio>()
    val listaPacientes = mutableListOf<Paciente>()
    val listaProfissionais = mutableListOf<Profissional>()

    private val agendaService = AgendaService()

    fun cadastroConsultorio(nome: String, endereco: String): Consultorio {
        val novoConsultorio = Consultorio(nomeConsultorio = nome, endereco = endereco)
        return novoConsultorio
    }

    fun buscarProfissionaisPorArea(area: String): List<Profissional> {
        return listaProfissionais.filter { it.areaAtuacao == area }
    }

    fun adicionarProfissional(
        nome: String,
        email: String,
        senha: String,
        areaAtuacao: String,
        diasDeTrabalho: List<HorarioTrabalho>
    ): Profissional {
        val novaAgenda = Agenda(
            mutableListOf(),
            mutableListOf()
        )
        val novoProfissional = Profissional(
            nomeProfissional = nome,
            email = email,
            senha = senha,
            areaAtuacao = areaAtuacao,
            agenda = novaAgenda,
            diasDeTrabalho = diasDeTrabalho
        )
        agendaService.inicializarAgenda(novoProfissional)
        listaProfissionais.add(novoProfissional)
        return novoProfissional
    }

    fun adicionarNutri(
        nome: String,
        email: String,
        senha: String,
        diasDeTrabalho: List<HorarioTrabalho>
    ): Nutricionista {
        val novaAgenda = Agenda(mutableListOf(), mutableListOf())
        val novoNutricionista = Nutricionista(
            nomeNutri = nome,
            email = email,
            senha = senha,
            agenda = novaAgenda,
            diasDeTrabalho = diasDeTrabalho
        )
        agendaService.inicializarAgenda(novoNutricionista)
        listaProfissionais.add(novoNutricionista)
        return novoNutricionista
    }

    fun adicionarPaciente(nome: String, email: String, senha: String): Paciente {
        val novoPaciente = Paciente(
            nomePaciente = nome,
            email = email,
            senha = senha
        )
        novoPaciente.dataCadastro = LocalDateTime.now()
        listaPacientes.add(novoPaciente)
        return novoPaciente
    }


}