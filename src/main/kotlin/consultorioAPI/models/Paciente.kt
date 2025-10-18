package com.consultorioAPI.models

import java.time.LocalDateTime
import java.util.UUID

data class Paciente(
    val idPaciente: String = UUID.randomUUID().toString(),
    val nomePaciente: String,
    var email: String,
    var senha: String
) {
    var dataCadastro: LocalDateTime? = null
}