package com.nutriAPI.models

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class Consulta(val idConsulta: String = UUID.randomUUID().toString(),
                         val pacienteID: String?,
                         val nomePaciente:String?,
                         val dataHoraConsulta: LocalDateTime,
                         val statusConsulta: StatusConsulta,
                         var valorConsulta: Double,
                         var descontoPercentual: Double = 0.0,
                         var isAvulso: Boolean = false) {


    fun aplicarDescontoManual() {
        valorConsulta *= (1 - descontoPercentual / 100)
    }

    fun cancelarConsulta(){
        TODO()
    }

}

enum class StatusConsulta {
    AGENDADA, CANCELADA, REALIZADA
}