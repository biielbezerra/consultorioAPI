package com.nutriAPI.models

import java.time.LocalDateTime
import java.util.UUID

data class Paciente(val idPaciente: String = UUID.randomUUID().toString(),
               val nomePaciente: String,
               var email: String,
               var senha: String,
               val consultasPaciente: MutableList<Consulta>,
               val dataCadastro: LocalDateTime) {

    fun listarConsultas(): List<Consulta> {
        return consultasPaciente
    }




}