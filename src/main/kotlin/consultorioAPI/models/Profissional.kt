package com.consultorioAPI.models

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
data class Profissional(
    val idProfissionalArg: String? = null,
    val nomeProfissional: String,
    val userId: String,
    val areaAtuacaoId: String,
    var valorBaseConsulta: Double = 170.0,
    var agenda: Agenda,
    var diasDeTrabalho: List<HorarioTrabalho> = emptyList(),
    var status: StatusUsuario = StatusUsuario.CONVIDADO,
    var atributosEspecificos: Map<String, String> = emptyMap()
) {
    val idProfissional: String = idProfissionalArg ?: UUID.randomUUID().toString()

    fun setValorConsulta(novoValor: Double) {
        this.valorBaseConsulta = novoValor
    }

}

@Serializable
data class HorarioTrabalho(
    val diaDaSemana: DayOfWeek,
    val horarioInicio: LocalTime,
    val horarioFim: LocalTime,
    val consultorioId: String
)