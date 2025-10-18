package com.nutriAPI.models

import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

open class Profissional(
    val idProfissional: String = UUID.randomUUID().toString(),
    val nomeProfissional: String,
    var email: String,
    var senha: String,
    val areaAtuacao: String,
    var valorBaseConsulta: Double = 170.0,
    var agenda: Agenda,
    var diasDeTrabalho: List<HorarioTrabalho>
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