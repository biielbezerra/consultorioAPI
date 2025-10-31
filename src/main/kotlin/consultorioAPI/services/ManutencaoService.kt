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

class ManutencaoService(private val pacienteRepository: PacienteRepository,
                        private val profissionalRepository: ProfissionalRepository,
                        private val pacienteService: PacienteService,
                        private val agendaService: AgendaService
) {

    suspend fun executarManutencaoDiaria() {
        atualizarAgendasFuturas()
        atualizarStatusDePacientesInativos()
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun atualizarAgendasFuturas() {

        val hoje: LocalDate = Clock.System.todayIn(fusoHorarioPadrao)
        val novoDiaParaAdicionar: LocalDate = hoje.plus(4 * 7, DateTimeUnit.DAY)

        val todosProfissionais = profissionalRepository.listarTodos()

        todosProfissionais.forEach { profissional ->
            if (profissional.status != StatusUsuario.ATIVO) return@forEach

            agendaService.gerarDisponibilidadePadrao(
                agenda = profissional.agenda,
                regras = profissional.diasDeTrabalho,
                dataInicio = novoDiaParaAdicionar,
                dataFim = novoDiaParaAdicionar
            )
            profissionalRepository.atualizar(profissional)
        }
    }

    private suspend fun atualizarStatusDePacientesInativos() {
        val todosPacientes = pacienteRepository.listarTodos()

        todosPacientes.forEach { paciente ->
            if (pacienteService.verificarInatividade(paciente)) {
                if (paciente.status == StatusUsuario.ATIVO) {
                    paciente.status = StatusUsuario.INATIVO
                    pacienteRepository.atualizar(paciente)
                    //TODO "Mensageria para enviar e-mail para o usuário falando que está com saudade"
                }
            }
        }
    }

}