package com.consultorioAPI.models

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.minutes
import java.util.UUID
import kotlin.time.ExperimentalTime

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
    var duracaoEmMinutos: Int = 60
) {

    fun aplicarDesconto(desconto: Double) {
        this.descontoPercentual = desconto
        this.valorConsulta = this.valorBase * (1 - desconto / 100)
    }

    @OptIn(ExperimentalTime::class)
    fun horarioFim(): LocalDateTime {
        val fusoHorario = TimeZone.currentSystemDefault() // Ou TimeZone.UTC
        val duracao = this.duracaoEmMinutos.minutes
        return dataHoraConsulta.toInstant(fusoHorario).plus(duracao).toLocalDateTime(fusoHorario)
    }
}

enum class StatusConsulta {
    AGENDADA, CANCELADA, REALIZADA, NAO_COMPARECEU
}