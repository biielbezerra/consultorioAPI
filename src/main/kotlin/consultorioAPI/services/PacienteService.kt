package com.consultorioAPI.services

import com.consultorioAPI.config.fusoHorarioPadrao
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
import org.slf4j.LoggerFactory

class PacienteService(private val consultaRepository: ConsultaRepository) {

    private val log = LoggerFactory.getLogger(javaClass)

    @OptIn(ExperimentalTime::class)
    suspend fun isClienteFiel(paciente: Paciente): Boolean {
        log.debug("Verificando se Paciente ${paciente.idPaciente} é 'cliente fiel'")
        val agora: Instant = Clock.System.now()
        val noventaDiasAtras: Instant = agora.minus(90.days)

        val consultas = consultaRepository.buscarPorPacienteId(paciente.idPaciente)
        return consultas.any { consulta ->
            val dataHoraConsultaInstant = consulta.dataHoraConsulta ?: return@any false

            consulta.statusConsulta == StatusConsulta.REALIZADA &&
                    dataHoraConsultaInstant > noventaDiasAtras
        }
    }

    @OptIn(ExperimentalTime::class)
    suspend fun verificarInatividade(paciente: Paciente): Boolean {
        log.debug("Verificando inatividade do Paciente ${paciente.idPaciente}")
        val agora: Instant = Clock.System.now()
        val noventaDiasAtras: Instant = agora.minus(90.days)

        val consultas = consultaRepository.buscarPorPacienteId(paciente.idPaciente)
        val inativo = consultas.none { consulta ->
            val dataHoraConsultaInstant = consulta.dataHoraConsulta ?: return@none false

            consulta.statusConsulta == StatusConsulta.REALIZADA &&
                    dataHoraConsultaInstant > noventaDiasAtras
        }
        if (inativo) {
            log.info("Paciente ${paciente.idPaciente} marcado como INATIVO (sem consultas realizadas nos últimos 90 dias).")
        }
        return inativo
    }

    @OptIn(ExperimentalTime::class)
    suspend fun isPacienteDisponivel(paciente: Paciente, novoHorario: LocalDateTime, duracao: Duration): Boolean {
        log.debug("Verificando disponibilidade do Paciente ${paciente.idPaciente} para $novoHorario")
        val consultas = consultaRepository.buscarPorPacienteId(paciente.idPaciente)
        val novoHorarioFim = novoHorario.toInstant(fusoHorarioPadrao).plus(duracao)

        return consultas.none { consulta ->
            if (consulta.statusConsulta == StatusConsulta.CANCELADA || consulta.dataHoraConsulta == null) {
                return@none false
            }
            val consultaExistenteInicio = consulta.dataHoraConsulta!!
            val consultaExistenteFim = consulta.horarioFim()!!

            val conflito = novoHorario.toInstant(fusoHorarioPadrao) < consultaExistenteFim && novoHorarioFim > consultaExistenteInicio
            if (conflito) {
                log.debug("Conflito de horário para Paciente ${paciente.idPaciente}: Consulta existente ${consulta.idConsulta} ($consultaExistenteInicio - $consultaExistenteFim) conflita com $novoHorario")
            }
            conflito
        }
    }
}