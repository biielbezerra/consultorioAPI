package com.consultorioAPI.services

import com.consultorioAPI.models.Paciente
import com.consultorioAPI.models.StatusConsulta
import com.consultorioAPI.repositories.ConsultaRepository
import kotlinx.datetime.*
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class PacienteService(private val consultaRepository: ConsultaRepository) {

    @OptIn(ExperimentalTime::class)
    suspend fun isClienteFiel(paciente: Paciente): Boolean {
        val agora: Instant = Clock.System.now()
        val noventaDiasAtras: Instant = agora.minus(90.days)

        val consultas = consultaRepository.buscarPorPacienteId(paciente.idPaciente)
        return consultas.any { consulta ->
            val dataHoraConsultaInstant = consulta.dataHoraConsulta.toInstant(fusoHorarioPadrao)

            consulta.statusConsulta == StatusConsulta.REALIZADA &&
                    dataHoraConsultaInstant > noventaDiasAtras
        }
    }

    @OptIn(ExperimentalTime::class)
    suspend fun verificarInatividade(paciente: Paciente): Boolean {
        val agora: Instant = Clock.System.now()
        val noventaDiasAtras: Instant = agora.minus(90.days)

        val consultas = consultaRepository.buscarPorPacienteId(paciente.idPaciente)
        return consultas.none { consulta ->
            val dataHoraConsultaInstant = consulta.dataHoraConsulta.toInstant(fusoHorarioPadrao)
            consulta.statusConsulta == StatusConsulta.REALIZADA &&
                    dataHoraConsultaInstant > noventaDiasAtras
        }
    }
    @OptIn(ExperimentalTime::class)
    suspend fun isPacienteDisponivel(paciente: Paciente, novoHorario: LocalDateTime, duracao: Duration): Boolean {
        val consultas = consultaRepository.buscarPorPacienteId(paciente.idPaciente)
        val novoHorarioFim = novoHorario.toInstant(fusoHorarioPadrao).plus(duracao).toLocalDateTime(fusoHorarioPadrao)

        return consultas.none { consulta ->
            if(consulta.statusConsulta == StatusConsulta.CANCELADA) return@none false

            val consultaExistenteInicio = consulta.dataHoraConsulta
            val consultaExistenteFim = consulta.horarioFim()

            novoHorario < consultaExistenteFim && novoHorarioFim > consultaExistenteInicio
        }
    }

}