package com.consultorioAPI.services

import com.consultorioAPI.config.fusoHorarioPadrao
import com.consultorioAPI.exceptions.*
import com.consultorioAPI.models.*
import com.consultorioAPI.repositories.*
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import consultorioAPI.mappers.*
import consultorioAPI.dtos.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class ProfissionalService (
    private val profissionalRepository: ProfissionalRepository,
    private val promocaoRepository: PromocaoRepository,
    private val agendaService: AgendaService,
    private val consultorioRepository: ConsultorioRepository,
    private val userRepository: UserRepository,
    private val areaAtuacaoRepository: AreaAtuacaoRepository
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

    suspend fun atualizarDuracaoConsulta(
        profissionalId: String,
        novaDuracao: Int,
        usuarioLogado: User
    ) {
        val profissional = profissionalRepository.buscarPorId(profissionalId)
            ?: throw RecursoNaoEncontradoException("Profissional não encontrado.")

        if (!usuarioLogado.isSuperAdmin && usuarioLogado.role != Role.RECEPCIONISTA) {
            if (usuarioLogado.role == Role.PROFISSIONAL) {
                val perfilLogado = profissionalRepository.buscarPorUserId(usuarioLogado.idUsuario)
                if (perfilLogado?.idProfissional != profissionalId) {
                    throw NaoAutorizadoException("Profissionais só podem alterar a própria duração.")
                }
            } else {
                throw NaoAutorizadoException("Usuário não autorizado.")
            }
        }

        if (novaDuracao <= 0) throw InputInvalidoException("Duração deve ser positiva.")

        profissional.duracaoPadraoMinutos = novaDuracao
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

        val idsConsultoriosRequisitados = novasRegras.map { it.consultorioId }.toSet()

        val consultoriosEncontrados = consultorioRepository.listarTodos()
            .filter { idsConsultoriosRequisitados.contains(it.idConsultorio) }
            .map { it.idConsultorio }
            .toSet()

        val idsFaltantes = idsConsultoriosRequisitados - consultoriosEncontrados
        if (idsFaltantes.isNotEmpty()) {
            throw RecursoNaoEncontradoException("Um ou mais Consultórios não foram encontrados. IDs inválidos: $idsFaltantes. Operação cancelada.")
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
    ): List<PromocaoResponse> {
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

        val promocoes = globais + minhasPromocoes
        return promocoes.map { it.toResponse() }
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

    suspend fun listarProfissionaisAtivos(usuarioLogado: User): List<ProfissionalResponse> {
        if (!usuarioLogado.isSuperAdmin && usuarioLogado.role != Role.RECEPCIONISTA) {
            throw NaoAutorizadoException("Apenas Admins ou Recepcionistas podem listar todos os profissionais.")
        }

        val profissionais = profissionalRepository.listarTodosAtivos()

        return coroutineScope {
            profissionais.map {
                async { it.toResponse(userRepository, areaAtuacaoRepository) }
            }.awaitAll()
        }
    }

}