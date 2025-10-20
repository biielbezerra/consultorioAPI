package com.consultorioAPI.models

import kotlinx.datetime.LocalDateTime
import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
data class Paciente(
    val idPacienteArg: String? = null,
    val nomePaciente: String,
    val userId: String,
    var status: StatusUsuario = StatusUsuario.ATIVO
) {
    val idPaciente: String = idPacienteArg ?: UUID.randomUUID().toString()
    var dataCadastro: LocalDateTime? = null
}