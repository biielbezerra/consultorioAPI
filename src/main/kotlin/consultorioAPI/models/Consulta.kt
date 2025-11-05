package com.consultorioAPI.models

import com.consultorioAPI.config.fusoHorarioPadrao
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.minutes
import java.util.UUID
import kotlin.time.ExperimentalTime
import kotlinx.serialization.Serializable

@Serializable
data class Consulta(
    var idConsulta: String = "",
    val pacienteID: String?,
    val nomePaciente: String?,
    val profissionalID: String?,
    val nomeProfissional: String?,
    val area: String,
    var dataHoraConsulta: LocalDateTime? = null,
    var statusConsulta: StatusConsulta = StatusConsulta.PENDENTE,
    var valorBase: Double,
    var valorConsulta: Double,
    var descontoPercentual: Double = 0.0,
    var isAvulso: Boolean = false,
    var duracaoEmMinutos: Int = 60,
    var promocoesAplicadasIds: List<String>? = null
) {

    fun aplicarDesconto(desconto: Double) {
        this.descontoPercentual = desconto
        this.valorConsulta = this.valorBase * (1 - desconto / 100)
    }

    @OptIn(ExperimentalTime::class)
    fun horarioFim(): LocalDateTime? {
        val data = dataHoraConsulta ?: return null
        val duracao = this.duracaoEmMinutos.minutes
        return data.toInstant(fusoHorarioPadrao).plus(duracao).toLocalDateTime(fusoHorarioPadrao)
    }
}

@Serializable
enum class StatusConsulta {
    AGENDADA, CANCELADA, REALIZADA, NAO_COMPARECEU, PENDENTE
}