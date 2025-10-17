package com.nutriAPI.models

import java.time.LocalDateTime

data class Agenda(
    val horariosDisponiveis: MutableList<LocalDateTime>,
    val horariosBloqueados: MutableList<LocalDateTime>
) {

    fun estaDisponivel(horario: LocalDateTime): Boolean {
        return horariosDisponiveis.contains(horario) && !horariosBloqueados.contains(horario)
    }


}