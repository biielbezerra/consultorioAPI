package com.consultorioAPI.services

import com.consultorioAPI.config.fusoHorarioPadrao
import com.consultorioAPI.exceptions.*
import com.consultorioAPI.models.*
import com.consultorioAPI.repositories.ProfissionalRepository
import com.consultorioAPI.repositories.PromocaoRepository
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class ProfissionalService (
    private val profissionalRepository: ProfissionalRepository,
    private val promocaoRepository: PromocaoRepository,
    private val agendaService: AgendaService
) {

    suspend fun atualizarValorConsulta(
        profissionalId: String,
        novoValor: Double,
        usuarioLogado: User
    ) {
        val profissional = profissionalRepository.buscarPorId(profissionalId)
            ?: throw RecursoNaoEncontradoException("Profissional não encontrado.")

        var permitido = false
        if (usuarioLogado.isSuperAdmin) {
            permitido = true
        } else if (usuarioLogado.role == Role.PROFISSIONAL) {
            val perfilLogado = profissionalRepository.buscarPorUserId(usuarioLogado.idUsuario)
            if (perfilLogado?.idProfissional == profissionalId) {
                permitido = true
            }
        }

        if (!permitido) {
            throw NaoAutorizadoException("Usuário não autorizado a alterar este valor de consulta.")
        }

        if (novoValor <= 0) throw InputInvalidoException("Valor da consulta deve ser positivo.")

        profissional.valorBaseConsulta = novoValor
        profissionalRepository.atualizar(profissional)
    }

    @OptIn(ExperimentalTime::class)
    suspend fun configurarHorarioPadraoEGerarAgendaInicial(
        profissionalId: String,
        novasRegras: List<HorarioTrabalho>,
        usuarioLogado: User
    ) {
        val profissional = profissionalRepository.buscarPorId(profissionalId)
            ?: throw RecursoNaoEncontradoException("Profissional não encontrado.")

        if (usuarioLogado.isSuperAdmin || usuarioLogado.role == Role.RECEPCIONISTA) {
            // OK
        } else {
            when (usuarioLogado.role) {
                Role.PROFISSIONAL -> {
                    val perfilLogado = profissionalRepository.buscarPorUserId(usuarioLogado.idUsuario)
                    if (perfilLogado?.idProfissional != profissionalId) {
                        throw NaoAutorizadoException("Profissionais só podem configurar sua própria agenda.")
                    }
                }
                Role.PACIENTE -> throw NaoAutorizadoException("Pacientes não podem configurar agendas.")
                else -> {}
            }
        }

        profissional.diasDeTrabalho = novasRegras

        profissional.agenda.horariosDisponiveis.clear()
        profissional.agenda.horariosBloqueados.clear()

        val hoje = Clock.System.todayIn(fusoHorarioPadrao)
        val dataFutura = hoje.plus(4 * 7, DateTimeUnit.DAY)
        agendaService.gerarDisponibilidadePadrao(
            agenda = profissional.agenda,
            regras = novasRegras,
            dataInicio = hoje,
            dataFim = dataFutura
        )

        profissionalRepository.atualizar(profissional)
    }

    @OptIn(ExperimentalTime::class)
    suspend fun listarPromocoesDisponiveisParaAtivar(
        profissionalId: String,
        usuarioLogado: User
    ): List<Promocao> {
        val profissional = profissionalRepository.buscarPorId(profissionalId)
            ?: throw RecursoNaoEncontradoException("Profissional não encontrado.")

        val agora = Clock.System.now()
        val globais = promocaoRepository.buscarAtivasPorData(agora)
            .filter { it.escopo == PromocaoEscopo.GLOBAL }

        val minhasPromocoes = promocaoRepository.listarTodas()
            .filter { it.criadoPorUserId == profissional.userId &&
                    it.escopo == PromocaoEscopo.PROFISSIONAL &&
                    it.isAtiva && !it.isDeletado &&
                    it.dataInicio <= agora &&
                    it.dataFim >= agora}

        return globais + minhasPromocoes
    }

    suspend fun ativarPromocao(
        profissionalId: String,
        promocaoId: String,
        usuarioLogado: User
    ) {
        val profissional = profissionalRepository.buscarPorId(profissionalId)
            ?: throw RecursoNaoEncontradoException("Profissional não encontrado.")

        val promocao = promocaoRepository.buscarPorId(promocaoId)
            ?: throw RecursoNaoEncontradoException("Promoção não encontrada.")

        if (promocao.escopo == PromocaoEscopo.PROFISSIONAL && promocao.profissionalIdAplicavel != profissionalId) {
            throw NaoAutorizadoException("Esta promoção não pode ser ativada por este profissional.")
        }

        if (!promocao.isAtiva || promocao.isDeletado) {
            throw ConflitoDeEstadoException("Esta promoção não está ativa.")
        }

        if (!profissional.promocoesAtivadasIds.contains(promocaoId)) {
            profissional.promocoesAtivadasIds += promocaoId
            profissionalRepository.atualizar(profissional)
        }
    }

    suspend fun desativarPromocao(
        profissionalId: String,
        promocaoId: String,
        usuarioLogado: User
    ) {
        val profissional = profissionalRepository.buscarPorId(profissionalId)
            ?: throw RecursoNaoEncontradoException("Profissional não encontrado.")

        if (profissional.promocoesAtivadasIds.contains(promocaoId)) {
            profissional.promocoesAtivadasIds = profissional.promocoesAtivadasIds.filterNot { it == promocaoId }
            profissionalRepository.atualizar(profissional)
        }
    }

    suspend fun listarProfissionaisAtivos(usuarioLogado: User): List<Profissional> {
        if (!usuarioLogado.isSuperAdmin && usuarioLogado.role != Role.RECEPCIONISTA) {
            throw NaoAutorizadoException("Apenas Admins ou Recepcionistas podem listar todos os profissionais.")
        }

        return profissionalRepository.listarTodosAtivos()
    }

}