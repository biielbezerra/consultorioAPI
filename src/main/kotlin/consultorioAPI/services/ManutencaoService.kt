package com.consultorioAPI.services

import com.consultorioAPI.config.fusoHorarioPadrao
import com.consultorioAPI.models.StatusUsuario
import com.consultorioAPI.repositories.PacienteRepository
import com.consultorioAPI.repositories.ProfissionalRepository
import kotlinx.datetime.*
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime
import org.slf4j.LoggerFactory

class ManutencaoService(private val pacienteRepository: PacienteRepository,
                        private val profissionalRepository: ProfissionalRepository,
                        private val pacienteService: PacienteService,
                        private val agendaService: AgendaService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun executarManutencaoDiaria() {
        log.info("INICIANDO ROTINA DE MANUTENÇÃO DIÁRIA")
        atualizarAgendasFuturas()
        atualizarStatusDePacientesInativos()
        log.info("ROTINA DE MANUTENÇÃO DIÁRIA CONCLUÍDA")
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun atualizarAgendasFuturas() {
        log.info("[Manutenção] Iniciando atualização de agendas futuras...")
        val hoje: LocalDate = Clock.System.todayIn(fusoHorarioPadrao)
        val novoDiaParaAdicionar: LocalDate = hoje.plus(13 * 7, DateTimeUnit.DAY)
        log.info("[Manutenção] Gerando agenda para o dia $novoDiaParaAdicionar")

        val todosProfissionais = profissionalRepository.listarTodos()
        log.info("[Manutenção] Encontrados ${todosProfissionais.size} profissionais para processar.")

        todosProfissionais.forEach { profissional ->
            if (profissional.status != StatusUsuario.ATIVO) {
                log.debug("[Manutenção] Pulando Prof ${profissional.idProfissional}: status ${profissional.status}")
                return@forEach
            }

            try {
                agendaService.gerarDisponibilidadePadrao(
                    agenda = profissional.agenda,
                    regras = profissional.diasDeTrabalho,
                    dataInicio = novoDiaParaAdicionar,
                    dataFim = novoDiaParaAdicionar
                )
                profissionalRepository.atualizar(profissional)
                log.info("[Manutenção] Agenda atualizada para Prof ${profissional.idProfissional}")
            } catch (e: Exception) {
                log.error("[Manutenção] FALHA ao atualizar agenda para Prof ${profissional.idProfissional}", e)
            }
        }
        log.info("[Manutenção] Atualização de agendas concluída.")
    }

    private suspend fun atualizarStatusDePacientesInativos() {
        log.info("[Manutenção] Iniciando verificação de pacientes inativos...")
        val todosPacientes = pacienteRepository.listarTodos()
        log.info("[Manutenção] Encontrados ${todosPacientes.size} pacientes para verificar.")

        var inativosAtualizados = 0

        todosPacientes.forEach { paciente ->
            try {
                if (pacienteService.verificarInatividade(paciente)) {
                    if (paciente.status == StatusUsuario.ATIVO) {
                        paciente.status = StatusUsuario.INATIVO
                        pacienteRepository.atualizar(paciente)
                        inativosAtualizados++
                        log.info("[Manutenção] Paciente ${paciente.idPaciente} atualizado para INATIVO.")
                        //TODO "Mensageria para enviar e-mail para o usuário falando que está com saudade"
                    }
                }
            } catch (e: Exception) {
                log.error("[Manutenção] FALHA ao verificar inatividade do Paciente ${paciente.idPaciente}", e)
            }
        }
        log.info("[Manutenção] Verificação de inatividade concluída. $inativosAtualizados pacientes atualizados.")
    }

}