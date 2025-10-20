package com.consultorioAPI.models

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

data class Agenda(
    val horariosDisponiveis: MutableList<LocalDateTime>,
    val horariosBloqueados: MutableList<LocalDateTime>
) {

    @OptIn(ExperimentalTime::class)
    fun estaDisponivel(
        horario: LocalDateTime,
        duracao: Duration,
        tamanhoSlot: Duration = 30.minutes
    ): Boolean {
        val fusoHorario = TimeZone.currentSystemDefault()
        val horarioFimConsulta = horario.toInstant(fusoHorario).plus(duracao).toLocalDateTime(fusoHorario)
        var atual = horario

        while(atual < horarioFimConsulta) {
            if (!this.horariosDisponiveis.contains(atual) || this.horariosBloqueados.contains(atual)) {
                return false
            }
            atual = atual.toInstant(fusoHorario).plus(tamanhoSlot).toLocalDateTime(fusoHorario)
        }
        return true
    }

    @OptIn(ExperimentalTime::class)
    fun liberarHorario(
        horario: LocalDateTime,
        duracao: Duration,
        tamanhoSlot: Duration = 30.minutes
    ) {
        val fusoHorario = TimeZone.currentSystemDefault() // Ou TimeZone.UTC
        val horarioFimConsulta = horario.toInstant(fusoHorario).plus(duracao).toLocalDateTime(fusoHorario)
        var atual = horario
        while (atual < horarioFimConsulta) {
            this.horariosBloqueados.remove(atual)
            atual = atual.toInstant(fusoHorario).plus(tamanhoSlot).toLocalDateTime(fusoHorario)
        }
    }

    @OptIn(ExperimentalTime::class)
    fun bloquearHorario(
        horario: LocalDateTime,
        consulta: Consulta,
        tamanhoSlot: Duration = 30.minutes
    ) {
        val fusoHorario = TimeZone.currentSystemDefault()
        val duracaoConsulta = consulta.duracaoEmMinutos.minutes
        val horarioFimConsulta = horario.toInstant(fusoHorario).plus(duracaoConsulta).toLocalDateTime(fusoHorario)
        var atual = horario
        while (atual < horarioFimConsulta) {
            if (!horariosBloqueados.contains(atual)) {
                horariosBloqueados.add(atual)
            }
            atual = atual.toInstant(fusoHorario).plus(tamanhoSlot).toLocalDateTime(fusoHorario)
        }
    }

}
