package com.consultorioAPI.models

import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

open class Profissional(
    val idProfissional: String = UUID.randomUUID().toString(),
    val nomeProfissional: String,
    val userId: String,
    val areaAtuacaoId: String,
    var valorBaseConsulta: Double = 170.0,
    var agenda: Agenda,
    var diasDeTrabalho: List<HorarioTrabalho> = emptyList(),
    var status: StatusUsuario = StatusUsuario.CONVIDADO,
    var atributosEspecificos: Map<String, String> = emptyMap()
) {

    fun setValorConsulta(novoValor: Double) {
        this.valorBaseConsulta = novoValor
    }

}

data class HorarioTrabalho(
    val diaDaSemana: DayOfWeek,
    val horarioInicio: LocalTime,
    val horarioFim: LocalTime,
    val consultorioId: String
)