package com.nutriAPI.models

import java.time.LocalDateTime
import java.util.UUID

class Consultorio(val idConsultorio: String = UUID.randomUUID().toString(),
                  val nomeConsultorio: String) {

    val listaPacientes = mutableListOf<Paciente>()
    val listaNutricionista = mutableListOf<Nutricionista>()
    val listaConsultas = mutableListOf<Consulta>()

    fun adicionarPaciente(nome: String, email: String, senha: String): Paciente {
        val novoPaciente = Paciente(nomePaciente = nome,
            email = email,
            senha = senha,
            consultasPaciente = mutableListOf(),
            dataCadastro = LocalDateTime.now()
        )
        listaPacientes.add(novoPaciente)
        return novoPaciente
    }

    fun adicionarNutri(nome: String, email: String, senha: String): Nutricionista {
        val novaAgenda = Agenda(mutableListOf(),
                                mutableListOf()
        )
        val novoNutricionista = Nutricionista(nomeNutri = nome,
            email = email,
            senha = senha,
            agenda = novaAgenda
        )
        listaNutricionista.add(novoNutricionista)
        return novoNutricionista
    }



}