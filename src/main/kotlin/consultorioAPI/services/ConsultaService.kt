package com.consultorioAPI.services

import com.consultorioAPI.config.fusoHorarioPadrao
import com.consultorioAPI.exceptions.*
import com.consultorioAPI.models.Consulta
import com.consultorioAPI.models.Paciente
import com.consultorioAPI.models.Profissional
import com.consultorioAPI.models.Role
import com.consultorioAPI.models.StatusConsulta
import com.consultorioAPI.models.StatusUsuario
import com.consultorioAPI.models.TipoPromocao
import com.consultorioAPI.models.User
import com.consultorioAPI.repositories.ConsultaRepository
import com.consultorioAPI.repositories.PacienteRepository
import com.consultorioAPI.repositories.ProfissionalRepository
import consultorioAPI.services.ConsultorioService
import consultorioAPI.services.PromocaoService
import kotlinx.datetime.*
import kotlin.time.Clock
import kotlinx.datetime.LocalDateTime
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import org.slf4j.LoggerFactory

class ConsultaService(
    private val pacienteService: PacienteService,
    private val consultaRepository: ConsultaRepository,
    private val pacienteRepository: PacienteRepository,
    private val profissionalRepository: ProfissionalRepository,
    private val promocaoService: PromocaoService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @OptIn(ExperimentalTime::class)
    suspend fun reagendarConsulta(
        consulta: Consulta,
        profissional: Profissional,
        paciente: Paciente,
        novaData: LocalDateTime,
        usuarioLogado: User
    ) {
        log.info("Iniciando reagendamento da Consulta ${consulta.idConsulta} para $novaData por User ${usuarioLogado.idUsuario}")
        checarPermissaoModificarConsulta(usuarioLogado, consulta, permitePaciente = true)

        if (consulta.statusConsulta != StatusConsulta.AGENDADA && consulta.statusConsulta != StatusConsulta.PENDENTE) {
            throw ConflitoDeEstadoException("Não é possível agendar ou reagendar uma consulta que está ${consulta.statusConsulta}.")
        }

        val isAgendamentoNovo = consulta.statusConsulta == StatusConsulta.PENDENTE
        val duracaoConsulta = consulta.duracaoEmMinutos.minutes

        val isProfissionalDisponivel = profissional.agenda.estaDisponivel(novaData, duracaoConsulta)
        val isPacienteDisponivel = pacienteService.isPacienteDisponivel(paciente, novaData, duracaoConsulta)

        if (!isProfissionalDisponivel || !isPacienteDisponivel) {
            log.warn("Falha no reagendamento ${consulta.idConsulta}: Horário indisponível. Prof: $isProfissionalDisponivel, Pac: $isPacienteDisponivel")
            throw ConflitoDeEstadoException("Operação falhou. O horário solicitado não está disponível.")
        }

        val consultaOriginal = consulta.copy()

        if (!isAgendamentoNovo && consulta.dataHoraConsulta != null) {
            val dataHoraLocalAntiga = consulta.dataHoraConsulta!!.toLocalDateTime(fusoHorarioPadrao)
            profissional.agenda.liberarHorario(dataHoraLocalAntiga, duracaoConsulta)
            log.debug("Horário antigo ${dataHoraLocalAntiga} liberado da agenda do Prof ${profissional.idProfissional}")
        }

        var promocoesFinais = consulta.promocoesAplicadasIds
        var descontoFinal = consulta.descontoPercentual
        var valorConsultaFinal = consulta.valorConsulta

        val promocoesOriginais = if (consulta.promocoesAplicadasIds?.isNotEmpty() == true) {
            promocaoService.buscarPromocoesPorIds(consulta.promocoesAplicadasIds!!)
        } else emptyList()

        val isPromocaoTravada = promocoesOriginais.any {
            it.tipoPromocao == TipoPromocao.PACOTE || it.tipoPromocao == TipoPromocao.PRIMEIRA_DUPLA
        }

        if (!isPromocaoTravada) {
            log.debug("Recalculando promoções para reagendamento da Consulta ${consulta.idConsulta}")
            val codigoOriginal = promocoesOriginais
                .firstOrNull { it.tipoPromocao == TipoPromocao.CODIGO }?.codigoOpcional

            val promocoesRecalculadas = promocaoService.buscarMelhorPromocaoAplicavel(
                paciente = paciente,
                profissional = profissional,
                dataConsultaProposta = novaData.toInstant(fusoHorarioPadrao),
                quantidadeConsultasSimultaneas = 1,
                codigoPromocionalInput = codigoOriginal
            )
            descontoFinal = ConsultorioService.calcularDescontoTotal(promocoesRecalculadas)
            valorConsultaFinal = consulta.valorBase * (1 - descontoFinal / 100)
            promocoesFinais = promocoesRecalculadas.map { it.idPromocao }
        }

        val consultaReagendada = consulta.copy(
            dataHoraConsulta = novaData.toInstant(fusoHorarioPadrao),
            statusConsulta = StatusConsulta.AGENDADA,
            valorBase = consulta.valorBase,
            valorConsulta = valorConsultaFinal,
            descontoPercentual = descontoFinal,
            promocoesAplicadasIds = promocoesFinais
        )

        val dataHoraLocalNova = consultaReagendada.dataHoraConsulta!!.toLocalDateTime(fusoHorarioPadrao)
        profissional.agenda.bloquearHorario(dataHoraLocalNova, consultaReagendada)
        log.debug("Horário novo ${dataHoraLocalNova} bloqueado na agenda do Prof ${profissional.idProfissional}")

        try {
            consultaRepository.atualizar(consultaReagendada)
            log.info("Consulta ${consultaReagendada.idConsulta} atualizada (reagendada) no banco.")

            try {
                profissionalRepository.atualizar(profissional)
                log.info("Agenda do Prof ${profissional.idProfissional} atualizada no banco.")
            } catch (eAgenda: Exception) {
                log.error("ERRO ao salvar agenda do Prof ${profissional.idProfissional}, iniciando rollback da consulta ${consultaReagendada.idConsulta}", eAgenda)
                consultaRepository.atualizar(consultaOriginal)
                log.warn("ROLLBACK: Consulta ${consultaOriginal.idConsulta} revertida ao estado original.")
                throw ConflitoDeEstadoException("Falha ao atualizar a agenda do profissional. A consulta foi revertida ao estado original.")
            }

        } catch (eConsulta: Exception) {
            log.error("Falha ao salvar o reagendamento da consulta ${consultaReagendada.idConsulta}", eConsulta)
            throw ConflitoDeEstadoException("Falha ao salvar o reagendamento da consulta: ${eConsulta.message}")
        }
    }

    @OptIn(ExperimentalTime::class)
    suspend fun cancelarConsulta(
        consulta: Consulta,
        paciente: Paciente,
        profissional: Profissional,
        usuarioLogado: User
    ) {
        log.info("Iniciando cancelamento da Consulta ${consulta.idConsulta} por User ${usuarioLogado.idUsuario}")
        checarPermissaoModificarConsulta(usuarioLogado, consulta, permitePaciente = true)

        if (consulta.statusConsulta == StatusConsulta.REALIZADA || consulta.statusConsulta == StatusConsulta.CANCELADA) {
            throw ConflitoDeEstadoException("Não é possível cancelar uma consulta que já foi realizada ou cancelada.")
        }

        var agendaFoiModificada = false
        if (consulta.statusConsulta == StatusConsulta.AGENDADA && consulta.dataHoraConsulta != null) {
            val duracaoDaConsulta = consulta.duracaoEmMinutos.minutes
            val dataHoraLocal = consulta.dataHoraConsulta!!.toLocalDateTime(fusoHorarioPadrao)
            profissional.agenda.liberarHorario(dataHoraLocal, duracaoDaConsulta)
            agendaFoiModificada = true
            log.debug("Horário ${dataHoraLocal} liberado da agenda do Prof ${profissional.idProfissional}")
        }

        val consultaCancelada = consulta.copy(statusConsulta = StatusConsulta.CANCELADA)

        try {
            consultaRepository.atualizar(consultaCancelada)
            if (agendaFoiModificada) {
                profissionalRepository.atualizar(profissional)
                log.info("Agenda do Prof ${profissional.idProfissional} atualizada (horário liberado).")
            }
            log.info("Consulta ${consulta.idConsulta} atualizada para CANCELADA.")
        } catch (e: Exception) {
            log.error("Falha ao salvar cancelamento da Consulta ${consulta.idConsulta}", e)
            throw ConflitoDeEstadoException("Falha ao salvar cancelamento: ${e.message}")
        }
    }

    @OptIn(ExperimentalTime::class)
    suspend fun finalizarConsulta(
        consulta: Consulta,
        novoStatus: StatusConsulta,
        usuarioLogado: User
    ) {
        log.info("Iniciando finalização da Consulta ${consulta.idConsulta} para status $novoStatus por User ${usuarioLogado.idUsuario}")
        checarPermissaoModificarConsulta(usuarioLogado, consulta, permitePaciente = false)

        if (novoStatus != StatusConsulta.REALIZADA && novoStatus != StatusConsulta.NAO_COMPARECEU) {
            throw InputInvalidoException("Este método só pode ser usado para marcar a consulta como REALIZADA ou NAO_COMPARECEU.")
        }
        if (consulta.statusConsulta != StatusConsulta.AGENDADA) {
            throw ConflitoDeEstadoException("Apenas consultas AGENDADAS podem ser finalizadas.")
        }

        val agora = Clock.System.now()
        val horaConsultaInstant = consulta.dataHoraConsulta!!
        if (horaConsultaInstant > agora) {
            throw ConflitoDeEstadoException("Ainda não é hora de finalizar esta consulta.")
        }

        val consultaFinalizada = consulta.copy(statusConsulta = novoStatus)
        consultaRepository.atualizar(consultaFinalizada)
        log.info("Consulta ${consulta.idConsulta} atualizada para $novoStatus.")

        val paciente = pacienteRepository.buscarPorId(
            consulta.pacienteID
                ?: throw ConflitoDeEstadoException("Consulta sem ID de paciente.")
        )
            ?: throw RecursoNaoEncontradoException("Paciente da consulta não encontrado.")

        if (novoStatus == StatusConsulta.REALIZADA) {
            if (!consulta.promocoesAplicadasIds.isNullOrEmpty()) {
                val codigosUsados = promocaoService.buscarPromocoesPorIds(consulta.promocoesAplicadasIds!!)
                    .filter { it.tipoPromocao == TipoPromocao.CODIGO && !it.codigoOpcional.isNullOrBlank() }
                    .map { it.codigoOpcional!!.trim().uppercase() }

                if (codigosUsados.isNotEmpty()) {
                    val codigosJaUsados = paciente.codigosPromocionaisUsados
                    paciente.codigosPromocionaisUsados = (codigosJaUsados + codigosUsados).distinct()
                    pacienteRepository.atualizar(paciente)
                }
            }
            if (paciente.status == StatusUsuario.INATIVO) {
                log.info("Paciente ${paciente.idPaciente} estava INATIVO, reativando após consulta realizada.")
                paciente.status = StatusUsuario.ATIVO
                pacienteRepository.atualizar(paciente)
            }
        }
    }

    suspend fun listarConsultasProfissional(profissionalIdAlvo: String, usuarioLogado: User): List<Consulta> {
        log.debug("User ${usuarioLogado.idUsuario} (Role ${usuarioLogado.role}) listando consultas do Prof $profissionalIdAlvo")

        if (usuarioLogado.isSuperAdmin || usuarioLogado.role == Role.RECEPCIONISTA) {
        } else {
            when (usuarioLogado.role) {
                Role.PROFISSIONAL -> {
                    val perfilProfissionalLogado = profissionalRepository.buscarPorUserId(usuarioLogado.idUsuario)
                        ?: throw RecursoNaoEncontradoException("Perfil profissional não encontrado para este usuário.")

                    if (perfilProfissionalLogado.idProfissional != profissionalIdAlvo) {
                        throw NaoAutorizadoException("Profissionais só podem listar sua própria agenda.")
                    }
                }

                Role.PACIENTE -> {
                    throw NaoAutorizadoException("Pacientes não podem listar a agenda de profissionais.")
                }

                else -> {}
            }
        }
        return consultaRepository.buscarPorProfissionalId(profissionalIdAlvo)
    }

    suspend fun listarConsultasPaciente(pacienteIdAlvo: String, usuarioLogado: User): List<Consulta> {
        log.debug("User ${usuarioLogado.idUsuario} (Role ${usuarioLogado.role}) listando consultas do Paciente $pacienteIdAlvo")

        if (usuarioLogado.isSuperAdmin || usuarioLogado.role == Role.RECEPCIONISTA) {
        } else {
            when (usuarioLogado.role) {
                Role.PACIENTE -> {
                    val perfilPacienteLogado = pacienteRepository.buscarPorUserId(usuarioLogado.idUsuario)
                        ?: throw RecursoNaoEncontradoException("Perfil de paciente não encontrado para este usuário.")

                    if (perfilPacienteLogado.idPaciente != pacienteIdAlvo) {
                        throw NaoAutorizadoException("Pacientes só podem listar suas próprias consultas.")
                    }
                }

                Role.PROFISSIONAL -> {
                    throw NaoAutorizadoException("Profissionais não podem listar o histórico completo de pacientes por esta função.")
                }

                else -> {}
            }
        }

        return consultaRepository.buscarPorPacienteId(pacienteIdAlvo)
    }

    private suspend fun checarPermissaoModificarConsulta(
        usuarioLogado: User,
        consulta: Consulta,
        permitePaciente: Boolean
    ) {
        log.debug("Checando permissão de modificação da Consulta ${consulta.idConsulta} por User ${usuarioLogado.idUsuario}")

        if (usuarioLogado.isSuperAdmin || usuarioLogado.role == Role.RECEPCIONISTA) return

        when (usuarioLogado.role) {

            Role.PACIENTE -> {
                if (!permitePaciente) throw NaoAutorizadoException("Pacientes não podem executar esta ação.")

                val perfilPacienteLogado = pacienteRepository.buscarPorUserId(usuarioLogado.idUsuario)
                    ?: throw RecursoNaoEncontradoException("Perfil de paciente não encontrado.")

                if (perfilPacienteLogado.idPaciente != consulta.pacienteID) {
                    throw NaoAutorizadoException("Pacientes só podem modificar suas próprias consultas.")
                }
                return
            }

            Role.PROFISSIONAL -> {
                val perfilProfissionalLogado = profissionalRepository.buscarPorUserId(usuarioLogado.idUsuario)
                    ?: throw RecursoNaoEncontradoException("Perfil profissional não encontrado.")

                if (perfilProfissionalLogado.idProfissional != consulta.profissionalID) {
                    throw NaoAutorizadoException("Profissionais só podem modificar suas próprias consultas.")
                }
                return
            }

            else -> {}
        }
    }

}