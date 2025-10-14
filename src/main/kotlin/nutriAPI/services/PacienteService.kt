package com.nutriAPI.services

import com.nutriAPI.models.Consulta
import com.nutriAPI.models.Paciente
import com.nutriAPI.models.StatusConsulta
import java.time.LocalDateTime

class PacienteService {

    fun criarPaciente(nome: String, email: String, senha: String): Paciente {
        return TODO()
    }

    fun agendarConsulta(paciente: Paciente, dataHora: LocalDateTime): List<Consulta> {
        TODO()
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

    fun verificarInatividade(paciente: Paciente): Boolean {
        val noventaDiasAtras = LocalDateTime.now().minusDays(90)
        return paciente.consultasPaciente.none() {consulta -> consulta.statusConsulta == StatusConsulta.REALIZADA &&
        consulta.dataHoraConsulta.isAfter(noventaDiasAtras)}
    }


    fun calcularValorConsulta(paciente: Paciente, desconto: Double): Double {
        var total: Double = 170.0

        TODO()
        //if(temDesconto(paciente)){
        //    total -= desconto
        //}

        return total
    }


}