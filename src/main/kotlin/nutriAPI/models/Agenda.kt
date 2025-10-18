package com.nutriAPI.models

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

data class Agenda(
    val horariosDisponiveis: MutableList<LocalDateTime>,
    val horariosBloqueados: MutableList<LocalDateTime>
) {

    fun estaDisponivel(
        horario: LocalDateTime,
        duracao: Duration,
        tamanhoSlot: Duration = Duration.ofMinutes(30)
    ): Boolean {
        val horarioFimConsulta = horario.plus(duracao)
        var atual = horario

        while(atual.isBefore(horarioFimConsulta)) {
            if (!this.horariosDisponiveis.contains(atual) || this.horariosBloqueados.contains(atual)) {
                return false
            }
            atual = atual.plus(tamanhoSlot)
        }
        return true
    }

    fun liberarHorario(
        horario: LocalDateTime,
        duracao: Duration,
        tamanhoSlot: Duration = Duration.ofMinutes(30)
    ) {
        val horarioFimConsulta = horario.plus(duracao)
        var atual = horario

        while (atual.isBefore(horarioFimConsulta)) {
            this.horariosBloqueados.remove(atual)
            atual = atual.plus(tamanhoSlot)
        }
    }

    fun bloquearHorario(
        horario: LocalDateTime,
        consulta: Consulta,
        tamanhoSlot: Duration = Duration.ofMinutes(30)
    ) {
        val horarioFimConsulta = horario.plus(consulta.duracao)
        var atual = horario

        while (atual.isBefore(horarioFimConsulta)) {
            if (!horariosBloqueados.contains(atual)) {
                horariosBloqueados.add(atual)
            }
            atual = atual.plus(tamanhoSlot)
        }
    }

}
