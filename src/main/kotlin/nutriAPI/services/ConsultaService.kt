package com.nutriAPI.services

import com.nutriAPI.models.Consulta
import com.nutriAPI.models.Paciente
import com.nutriAPI.models.Profissional
import com.nutriAPI.models.StatusConsulta
import com.nutriAPI.repositories.ConsultaRepository
import java.time.LocalDateTime

class ConsultaService(private val pacienteService: PacienteService,
                      private val consultaRepository: ConsultaRepository
) {

    fun reagendarConsulta(
        consulta: Consulta,
        profissional: Profissional,
        paciente: Paciente,
        novaData: LocalDateTime
    ) {
        if (consulta.statusConsulta == StatusConsulta.REALIZADA || consulta.statusConsulta == StatusConsulta.CANCELADA) {
            throw IllegalStateException("Não é possível reagendar uma consulta que já foi realizada ou cancelada.")
        }

        val consultaReagendada = consulta.copy(dataHoraConsulta = novaData)

        val isProfissionalDisponivel =
            profissional.agenda.estaDisponivel(consultaReagendada.dataHoraConsulta, consultaReagendada.duracao)
        val isPacienteDisponivel = pacienteService.isPacienteDisponivel(
            paciente,
            consultaReagendada.dataHoraConsulta,
            consultaReagendada.duracao
        )

        if (!isProfissionalDisponivel || !isPacienteDisponivel) {
            throw IllegalStateException("Operação falhou. O horário solicitado não está disponível.")
        }

        profissional.agenda.liberarHorario(consulta.dataHoraConsulta, consulta.duracao)
        profissional.agenda.bloquearHorario(consultaReagendada.dataHoraConsulta, consultaReagendada)

        consultaRepository.atualizar(consultaReagendada)
    }

    fun cancelarConsulta(consulta: Consulta, paciente: Paciente, profissional: Profissional) {
        if (consulta.statusConsulta == StatusConsulta.REALIZADA || consulta.statusConsulta == StatusConsulta.CANCELADA) {
            throw IllegalStateException("Não é possível cancelar uma consulta que já foi realizada ou cancelada.")
        }

        profissional.agenda.liberarHorario(consulta.dataHoraConsulta, consulta.duracao)

        val consultaCancelada = consulta.copy(statusConsulta = StatusConsulta.CANCELADA)

        consultaRepository.atualizar(consultaCancelada)

    }

}