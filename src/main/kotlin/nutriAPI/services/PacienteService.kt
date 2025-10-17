package com.nutriAPI.services

import com.nutriAPI.models.Agenda
import com.nutriAPI.models.Consulta
import com.nutriAPI.models.Paciente
import com.nutriAPI.models.StatusConsulta
import java.time.Duration
import java.time.LocalDateTime

class PacienteService {

    fun isClienteFiel(paciente: Paciente): Boolean {
        val noventaDiasAtras = LocalDateTime.now().minusDays(90)
        return paciente.consultasPaciente.any() { consulta ->
            consulta.statusConsulta == StatusConsulta.REALIZADA &&
                    consulta.dataHoraConsulta.isAfter(noventaDiasAtras)
        }
    }

    fun verificarInatividade(paciente: Paciente): Boolean {
        val noventaDiasAtras = LocalDateTime.now().minusDays(90)
        return paciente.consultasPaciente.none() { consulta ->
            consulta.statusConsulta == StatusConsulta.REALIZADA &&
                    consulta.dataHoraConsulta.isAfter(noventaDiasAtras)
        }
    }

    fun isPacienteDisponivel(paciente: Paciente, novoHorario: LocalDateTime, duracao: Duration): Boolean {
        return paciente.consultasPaciente.none { consulta ->
            val consultaExistenteInicio = consulta.dataHoraConsulta
            val consultaExistenteFim = consulta.horarioFim()

            val novoHorarioFim = novoHorario.plus(duracao)

            novoHorario.isBefore(consultaExistenteFim) && novoHorarioFim.isAfter(consultaExistenteInicio)
        }
    }

}