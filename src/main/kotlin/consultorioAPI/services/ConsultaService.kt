package com.consultorioAPI.services

import com.consultorioAPI.config.fusoHorarioPadrao
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
import kotlinx.datetime.*
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

class ConsultaService(
    private val pacienteService: PacienteService,
    private val consultaRepository: ConsultaRepository,
    private val pacienteRepository: PacienteRepository,
    private val profissionalRepository: ProfissionalRepository
) {

    suspend fun reagendarConsulta(
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

        val duracaoConsulta = consulta.duracaoEmMinutos.minutes

        val isProfissionalDisponivel = profissional.agenda.estaDisponivel(novaData, duracaoConsulta)

        val isPacienteDisponivel = pacienteService.isPacienteDisponivel(paciente, novaData, duracaoConsulta)

        if (!isProfissionalDisponivel || !isPacienteDisponivel) {
            throw IllegalStateException("Operação falhou. O horário solicitado não está disponível.")
        }

        profissional.agenda.liberarHorario(consulta.dataHoraConsulta, duracaoConsulta)

        val consultaReagendada = consulta.copy(dataHoraConsulta = novaData)

        profissional.agenda.bloquearHorario(consultaReagendada.dataHoraConsulta, consultaReagendada)

        consultaRepository.atualizar(consultaReagendada)
    }

    suspend fun cancelarConsulta(
        consulta: Consulta,
        paciente: Paciente,
        profissional: Profissional,
        usuarioLogado: User
    ) {
        checarPermissaoModificarConsulta(usuarioLogado, consulta, permitePaciente = true)

        if (consulta.statusConsulta == StatusConsulta.REALIZADA || consulta.statusConsulta == StatusConsulta.CANCELADA) {
            throw IllegalStateException("Não é possível cancelar uma consulta que já foi realizada ou cancelada.")
        }

        val duracaoDaConsulta = consulta.duracaoEmMinutos.minutes

        profissional.agenda.liberarHorario(consulta.dataHoraConsulta, duracaoDaConsulta)

        val consultaCancelada = consulta.copy(statusConsulta = StatusConsulta.CANCELADA)

        consultaRepository.atualizar(consultaCancelada)
    }

    @OptIn(ExperimentalTime::class)
    suspend fun finalizarConsulta(
        consulta: Consulta,
        novoStatus: StatusConsulta,
        usuarioLogado: User
    ) {
        checarPermissaoModificarConsulta(usuarioLogado, consulta, permitePaciente = false)

        if (novoStatus != StatusConsulta.REALIZADA && novoStatus != StatusConsulta.NAO_COMPARECEU) {
            throw IllegalArgumentException("Este método só pode ser usado para marcar a consulta como REALIZADA ou NAO_COMPARECEU.")
        }
        if (consulta.statusConsulta != StatusConsulta.AGENDADA) {
            throw IllegalStateException("Apenas consultas AGENDADAS podem ser finalizadas.")
        }

        val agora = Clock.System.now()
        val horaConsultaInstant = consulta.dataHoraConsulta.toInstant(fusoHorarioPadrao)
        if (horaConsultaInstant > agora) {
            throw IllegalStateException("Ainda não é hora de finalizar esta consulta.")
        }

        val consultaFinalizada = consulta.copy(statusConsulta = novoStatus)
        consultaRepository.atualizar(consultaFinalizada)

        if (novoStatus == StatusConsulta.REALIZADA) {
            val paciente = pacienteRepository.buscarPorId(
                consulta.pacienteID
                    ?: throw IllegalStateException("Consulta sem ID de paciente.")
            )
                ?: throw IllegalStateException("Paciente não encontrado.")

            if (paciente.status == StatusUsuario.INATIVO) {
                paciente.status = StatusUsuario.ATIVO
                pacienteRepository.atualizar(paciente)
            }
        }
    }

    suspend fun listarConsultasProfissional(profissionalIdAlvo: String, usuarioLogado: User): List<Consulta> {

        // CHECAGEM DE PERMISSÃO
        when (usuarioLogado.role) {
            Role.SUPER_ADMIN, Role.RECEPCIONISTA -> {
                // Permitido buscar qualquer profissional
            }
            Role.PROFISSIONAL -> {
                val perfilProfissionalLogado = profissionalRepository.buscarPorUserId(usuarioLogado.idUsuario)
                    ?: throw SecurityException("Perfil profissional não encontrado para este usuário.")

                if (perfilProfissionalLogado.idProfissional != profissionalIdAlvo) {
                    throw SecurityException("Profissionais só podem listar sua própria agenda.")
                }
            }
            Role.PACIENTE -> {
                throw SecurityException("Pacientes não podem listar a agenda de profissionais.")
            }
        }
        return consultaRepository.buscarPorProfissionalId(profissionalIdAlvo)
    }

    suspend fun listarConsultasPaciente(pacienteIdAlvo: String, usuarioLogado: User): List<Consulta> {

        when (usuarioLogado.role) {
            Role.SUPER_ADMIN, Role.RECEPCIONISTA -> {

            }
            Role.PACIENTE -> {
                val perfilPacienteLogado = pacienteRepository.buscarPorUserId(usuarioLogado.idUsuario)
                    ?: throw SecurityException("Perfil de paciente não encontrado para este usuário.")

                if (perfilPacienteLogado.idPaciente != pacienteIdAlvo) {
                    throw SecurityException("Pacientes só podem listar suas próprias consultas.")
                }
            }
            Role.PROFISSIONAL -> {
                throw SecurityException("Profissionais não podem listar o histórico completo de pacientes por esta função.")
            }
        }

        return consultaRepository.buscarPorPacienteId(pacienteIdAlvo)
    }

    private suspend fun checarPermissaoModificarConsulta(
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