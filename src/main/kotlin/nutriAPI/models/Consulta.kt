package com.nutriAPI.models

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class Consulta(
    val idConsulta: String = UUID.randomUUID().toString(),
    val pacienteID: String?,
    val nomePaciente: String?,
    val profissionalID: String?,
    val nomeProfissional: String?,
    val area: String,
    val dataHoraConsulta: LocalDateTime,
    val statusConsulta: StatusConsulta,
    var valorBase: Double,
    var valorConsulta: Double,
    var descontoPercentual: Double = 0.0,
    var isAvulso: Boolean = false,
    var duracao: Duration = Duration.ofMinutes(60)
) {
    fun horarioFim(): LocalDateTime {
        return dataHoraConsulta.plus(duracao)
    }
}

enum class StatusConsulta {
    AGENDADA, CANCELADA, REALIZADA
}