package com.consultorioAPI.models

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
data class Profissional(
    var idProfissional: String = "",
    var nomeProfissional: String,
    var telefone: String? = null,
    val userId: String,
    val areaAtuacaoId: String,
    var valorBaseConsulta: Double = 200.0,
    var agenda: Agenda,
    var diasDeTrabalho: List<HorarioTrabalho> = emptyList(),
    var status: StatusUsuario = StatusUsuario.CONVIDADO,
    var atributosEspecificos: Map<String, String> = emptyMap(),
    var promocoesAtivadasIds: List<String> = emptyList(),
    var isDeletado: Boolean = false
)

@Serializable
data class HorarioTrabalho(
    val diaDaSemana: DayOfWeek,
    val horarioInicio: LocalTime,
    val horarioFim: LocalTime,
    val consultorioId: String
)