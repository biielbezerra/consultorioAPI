package com.nutriAPI.services

import com.nutriAPI.models.Agenda
import com.nutriAPI.models.Profissional
import java.time.LocalDateTime

class AgendaService {

    fun adicionarHorario(agenda: Agenda, horario: LocalDateTime) {
        agenda.horariosDisponiveis.add(horario)
    }

    fun removerHorario(agenda: Agenda, horario: LocalDateTime) {
        agenda.horariosDisponiveis.remove(horario)
    }

    fun bloquearHorario(agenda: Agenda, horario: LocalDateTime) {
        if (agenda.horariosDisponiveis.contains(horario)) {
            agenda.horariosBloqueados.add(horario)
        }
    }

    fun liberarHorario(agenda: Agenda, horario: LocalDateTime) {
        agenda.horariosBloqueados.remove(horario)
    }

    fun listarHorariosDisponiveis(agenda: Agenda): List<LocalDateTime> {
        return agenda.horariosDisponiveis.filter { !agenda.horariosBloqueados.contains(it) }
    }

}