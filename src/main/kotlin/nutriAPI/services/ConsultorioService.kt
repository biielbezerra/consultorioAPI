package com.nutriAPI.services

import com.nutriAPI.models.Agenda
import com.nutriAPI.models.Consulta
import com.nutriAPI.models.Paciente
import com.nutriAPI.models.StatusConsulta
import java.time.LocalDateTime

class ConsultorioService {

    fun agendarConsulta(nomePaciente:String,
                        emailPaciente:String,
                        dataHora: LocalDateTime,
                        valor:Double,
                        isAvulso:Boolean = true): List<Consulta> {
        TODO()
    }

    fun verificarInatividade(paciente: Paciente): Boolean {
        val noventaDiasAtras = LocalDateTime.now().minusDays(90)
        return paciente.consultasPaciente.none() {consulta -> consulta.statusConsulta == StatusConsulta.REALIZADA &&
                consulta.dataHoraConsulta.isAfter(noventaDiasAtras)}
    }

    fun isClienteFiel(paciente: Paciente): Boolean {
        val noventaDiasAtras = LocalDateTime.now().minusDays(90)
        return paciente.consultasPaciente.any() {consulta -> consulta.statusConsulta == StatusConsulta.REALIZADA &&
                consulta.dataHoraConsulta.isAfter(noventaDiasAtras)}
    }

    fun temDesconto(paciente: Paciente, quantidade: Int? = 1): Boolean {
        if(paciente.consultasPaciente.isEmpty() && quantidade == 2) {
            return true
        } else if(isClienteFiel(paciente) && !verificarInatividade(paciente)) {
            return true
        }
        return false
    }

    fun calcularValorConsulta(paciente: Paciente, desconto: Double, consulta: Consulta): Double {
        return if(temDesconto(paciente)){
            consulta.valorConsulta*(1 - (desconto/100))
        } else{
            consulta.valorConsulta
        }
    }


    fun validarHorarioParaAgendamento(agenda: Agenda, horario: LocalDateTime): Boolean {
        return agenda.estaDisponivel(horario)
    }

    fun reagendarConsulta(consulta: Consulta, novaData: LocalDateTime){
        TODO()
    }

    fun aplicarDescontoManual(consulta: Consulta, desconto: Double) {
        consulta.descontoPercentual = desconto
        consulta.valorConsulta *= (1 - desconto / 100)
    }

    fun cancelarConsulta(){
        TODO()
    }



}