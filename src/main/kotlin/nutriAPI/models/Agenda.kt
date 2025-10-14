package com.nutriAPI.models

import java.time.LocalDateTime

class Agenda(val horariosDisponiveis: MutableList<LocalDateTime>,
             val horariosBloqueados: MutableList<LocalDateTime>) {

    fun adicionarHorario(horario: LocalDateTime) {
        horariosDisponiveis.add(horario)
    }

    fun removerHorario(horario: LocalDateTime){
        horariosDisponiveis.remove(horario)
    }

    fun estaDisponivel(horario: LocalDateTime): Boolean{
        return horariosDisponiveis.contains(horario) && !horariosBloqueados.contains(horario)
    }

    fun bloquearHorario(horario: LocalDateTime){
        if(horariosDisponiveis.contains(horario)){
            horariosBloqueados.add(horario)
        }
    }

    fun liberarHorario(horario: LocalDateTime){
        horariosBloqueados.remove(horario)
    }

}