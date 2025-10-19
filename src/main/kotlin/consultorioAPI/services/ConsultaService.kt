package com.consultorioAPI.services

import com.consultorioAPI.models.Consulta
import com.consultorioAPI.models.Paciente
import com.consultorioAPI.models.Profissional
import com.consultorioAPI.models.Role
import com.consultorioAPI.models.StatusConsulta
import com.consultorioAPI.models.StatusUsuario
import com.consultorioAPI.models.User
import com.consultorioAPI.repositories.ConsultaRepository
import com.consultorioAPI.repositories.PacienteRepository
import com.consultorioAPI.repositories.ProfissionalRepository
import java.time.LocalDateTime

class ConsultaService(private val pacienteService: PacienteService,
                      private val consultaRepository: ConsultaRepository,
                      private val pacienteRepository: PacienteRepository,
                      private val profissionalRepository: ProfissionalRepository
) {

    fun reagendarConsulta(
        consulta: Consulta,
        profissional: Profissional,
        paciente: Paciente,
        novaData: LocalDateTime,
        usuarioLogado: User
    ) {
        checarPermissaoModificarConsulta(usuarioLogado, consulta, permitePaciente = true)

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

    fun cancelarConsulta(consulta: Consulta, paciente: Paciente, profissional: Profissional, usuarioLogado: User) {

        checarPermissaoModificarConsulta(usuarioLogado, consulta, permitePaciente = true)

        if (consulta.statusConsulta == StatusConsulta.REALIZADA || consulta.statusConsulta == StatusConsulta.CANCELADA) {
            throw IllegalStateException("Não é possível cancelar uma consulta que já foi realizada ou cancelada.")
        }

        profissional.agenda.liberarHorario(consulta.dataHoraConsulta, consulta.duracao)

        val consultaCancelada = consulta.copy(statusConsulta = StatusConsulta.CANCELADA)

        consultaRepository.atualizar(consultaCancelada)

    }

    fun finalizarConsulta(consulta: Consulta, novoStatus: StatusConsulta, usuarioLogado: User) {

        checarPermissaoModificarConsulta(usuarioLogado, consulta, permitePaciente = false)

        if (novoStatus != StatusConsulta.REALIZADA && novoStatus != StatusConsulta.NAO_COMPARECEU) {
            throw IllegalArgumentException("Este método só pode ser usado para marcar a consulta como REALIZADA ou NAO_COMPARECEU.")
        }

        if (consulta.statusConsulta != StatusConsulta.AGENDADA) {
            throw IllegalStateException("Apenas consultas AGENDADAS podem ser finalizadas.")
        }

        val consultaFinalizada = consulta.copy(statusConsulta = novoStatus)
        consultaRepository.atualizar(consultaFinalizada)

        if (novoStatus == StatusConsulta.REALIZADA) {
            val paciente = pacienteRepository.buscarPorId(consulta.pacienteID
                ?: throw IllegalStateException("Consulta sem ID de paciente."))
                ?: throw IllegalStateException("Paciente não encontrado.")

            if (paciente.status == StatusUsuario.INATIVO) {
                paciente.status = StatusUsuario.ATIVO
                pacienteRepository.atualizar(paciente)
            }
        }
    }

    private fun checarPermissaoModificarConsulta(
        usuarioLogado: User,
        consulta: Consulta,
        permitePaciente: Boolean
    ) {
        when (usuarioLogado.role) {
            Role.SUPER_ADMIN, Role.RECEPCIONISTA -> return

            Role.PACIENTE -> {
                if (!permitePaciente) throw SecurityException("Pacientes não podem executar esta ação.")

                val perfilPacienteLogado = pacienteRepository.buscarPorUserId(usuarioLogado.idUsuario)
                    ?: throw SecurityException("Perfil de paciente não encontrado.")

                if (perfilPacienteLogado.idPaciente != consulta.pacienteID) {
                    throw SecurityException("Pacientes só podem modificar suas próprias consultas.")
                }
                return
            }
            Role.PROFISSIONAL -> {
                val perfilProfissionalLogado = profissionalRepository.buscarPorUserId(usuarioLogado.idUsuario)
                    ?: throw SecurityException("Perfil profissional não encontrado.")

                if (perfilProfissionalLogado.idProfissional != consulta.profissionalID) {
                    throw SecurityException("Profissionais só podem modificar suas próprias consultas.")
                }
                return
            }
        }
    }

}