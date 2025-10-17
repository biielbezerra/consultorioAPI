package com.nutriAPI.models

import java.time.Duration
import java.time.LocalDateTime

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
            if (this.horariosBloqueados.contains(atual)) {
                return false
            }
            atual = atual.plus(tamanhoSlot)
        }
        return true
    }

    fun bloquearHorario(
        horario: LocalDateTime,
        consulta: Consulta,
        tamanhoSlot: Duration = Duration.ofMinutes(30)
    ) {
        val horarioFimConsulta = horario.plus(consulta.duracao)
        var atual = horario

        while (atual.isBefore(horarioFimConsulta)) { // não inclui o horário final, para não bloquear além da duração real da consulta
            if (!horariosBloqueados.contains(atual)) {
                horariosBloqueados.add(atual)
            }
            atual = atual.plus(tamanhoSlot)
        }
    }

}