package com.nutriAPI.services

import com.nutriAPI.models.Agenda
import java.time.LocalDateTime

class AgendaService {

    fun listarHorariosDisponiveis(agenda: Agenda): List<LocalDateTime> {
        return agenda.horariosDisponiveis.filter { !agenda.horariosBloqueados.contains(it) }
    }

    fun validarHorarioParaAgendamento(agenda: Agenda, horario: LocalDateTime): Boolean {
        return agenda.estaDisponivel(horario)
    }


}