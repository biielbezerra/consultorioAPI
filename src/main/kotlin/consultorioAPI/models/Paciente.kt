package com.consultorioAPI.models

import kotlinx.datetime.LocalDateTime
import java.util.UUID

data class Paciente(
    val idPaciente: String = UUID.randomUUID().toString(),
    val nomePaciente: String,
    val userId: String,
    var status: StatusUsuario = StatusUsuario.ATIVO
) {
    var dataCadastro: LocalDateTime? = null
}