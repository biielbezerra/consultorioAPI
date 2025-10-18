package com.nutriAPI.services

import com.nutriAPI.models.Agenda
import com.nutriAPI.models.Consulta
import com.nutriAPI.models.Paciente
import com.nutriAPI.models.StatusConsulta
import com.nutriAPI.repositories.ConsultaRepository
import java.time.Duration
import java.time.LocalDateTime

class PacienteService(private val consultaRepository: ConsultaRepository) {

    fun isClienteFiel(paciente: Paciente): Boolean {
        val noventaDiasAtras = LocalDateTime.now().minusDays(90)
        val consultas = consultaRepository.buscarPorPacienteId(paciente.idPaciente)
        return consultas.any() { consulta ->
            consulta.statusConsulta == StatusConsulta.REALIZADA &&
                    consulta.dataHoraConsulta.isAfter(noventaDiasAtras)
        }
    }

    fun verificarInatividade(paciente: Paciente): Boolean {
        val noventaDiasAtras = LocalDateTime.now().minusDays(90)
        val consultas = consultaRepository.buscarPorPacienteId(paciente.idPaciente)
        return consultas.none() { consulta ->
            consulta.statusConsulta == StatusConsulta.REALIZADA &&
                    consulta.dataHoraConsulta.isAfter(noventaDiasAtras)
        }
    }

    fun isPacienteDisponivel(paciente: Paciente, novoHorario: LocalDateTime, duracao: Duration): Boolean {
        val consultas = consultaRepository.buscarPorPacienteId(paciente.idPaciente)
        return consultas.none { consulta ->
            if(consulta.statusConsulta == StatusConsulta.CANCELADA) return false

            val consultaExistenteInicio = consulta.dataHoraConsulta
            val consultaExistenteFim = consulta.horarioFim()
            val novoHorarioFim = novoHorario.plus(duracao)

            novoHorario.isBefore(consultaExistenteFim) && novoHorarioFim.isAfter(consultaExistenteInicio)
        }
    }

}