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
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
@Serializable
data class Consulta(
    var idConsulta: String = UUID.randomUUID().toString(),
    val pacienteID: String?,
    val nomePaciente: String?,
    val profissionalID: String?,
    val nomeProfissional: String?,
    var consultorioId: String? = null,
    val area: String,
    var dataHoraConsulta: Instant? = null,
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
    fun horarioFim(): Instant? {
        val data = dataHoraConsulta ?: return null
        val duracao = this.duracaoEmMinutos.minutes
        return data.plus(duracao)
    }
}

@Serializable
enum class StatusConsulta {
    AGENDADA, CANCELADA, REALIZADA, NAO_COMPARECEU, PENDENTE
}