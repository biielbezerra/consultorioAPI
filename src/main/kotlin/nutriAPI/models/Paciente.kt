package com.nutriAPI.models

import java.time.LocalDateTime
import java.util.UUID

data class Paciente(
    val idPaciente: String = UUID.randomUUID().toString(),
    val nomePaciente: String,
    var email: String,
    var senha: String,
    val agenda: Agenda
) {
    var dataCadastro: LocalDateTime? = null
    val consultasPaciente = mutableListOf<Consulta>()

    fun listarConsultas(): List<Consulta> {
        return consultasPaciente
    }

}