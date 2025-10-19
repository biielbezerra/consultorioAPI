package com.consultorioAPI.services

import com.consultorioAPI.models.StatusUsuario
import com.consultorioAPI.repositories.PacienteRepository
import com.consultorioAPI.repositories.ProfissionalRepository
import java.time.LocalDate

class ManutencaoService(private val pacienteRepository: PacienteRepository,
                        private val profissionalRepository: ProfissionalRepository,
                        private val pacienteService: PacienteService,
                        private val agendaService: AgendaService
) {

    fun executarManutencaoDiaria() {
        atualizarAgendasFuturas()
        atualizarStatusDePacientesInativos()
    }

    private fun atualizarAgendasFuturas() {

        val novoDiaParaAdicionar = LocalDate.now().plusWeeks(4)

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

    private fun atualizarStatusDePacientesInativos() {
        val todosPacientes = pacienteRepository.listarTodos()

        todosPacientes.forEach { paciente ->
            if (pacienteService.verificarInatividade(paciente)) {
                if (paciente.status == StatusUsuario.ATIVO) {
                    paciente.status = StatusUsuario.INATIVO
                    pacienteRepository.atualizar(paciente)
                    TODO("Mensageria para enviar e-mail para o usuário falando que está com saudade")
                }
            }
        }
    }

}