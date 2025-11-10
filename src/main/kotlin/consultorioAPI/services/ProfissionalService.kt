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
import org.slf4j.LoggerFactory

class ProfissionalService (
    private val profissionalRepository: ProfissionalRepository,
    private val promocaoRepository: PromocaoRepository,
    private val agendaService: AgendaService,
    private val consultorioRepository: ConsultorioRepository,
    private val userRepository: UserRepository,
    private val areaAtuacaoRepository: AreaAtuacaoRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun atualizarValorConsulta(
        profissionalId: String,
        novoValor: Double,
        usuarioLogado: User
    ) {
        log.info("User ${usuarioLogado.idUsuario} atualizando valor da consulta para Prof $profissionalId")
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
            log.warn("Acesso negado: User ${usuarioLogado.idUsuario} (Role ${usuarioLogado.role}) tentou alterar valor para Prof $profissionalId")
            throw NaoAutorizadoException("Usuário não autorizado a alterar este valor de consulta.")
        }

        if (novoValor <= 0) throw InputInvalidoException("Valor da consulta deve ser positivo.")

        profissional.valorBaseConsulta = novoValor
        profissionalRepository.atualizar(profissional)
        log.info("Valor da consulta do Prof $profissionalId atualizado para $novoValor")
    }

    suspend fun atualizarDuracaoConsulta(
        profissionalId: String,
        novaDuracao: Int,
        usuarioLogado: User
    ) {
        log.info("User ${usuarioLogado.idUsuario} atualizando duração da consulta para $novaDuracao min for Prof $profissionalId")
        val profissional = profissionalRepository.buscarPorId(profissionalId)
            ?: throw RecursoNaoEncontradoException("Profissional não encontrado.")

        if (!usuarioLogado.isSuperAdmin && usuarioLogado.role != Role.RECEPCIONISTA) {
            if (usuarioLogado.role == Role.PROFISSIONAL) {
                val perfilLogado = profissionalRepository.buscarPorUserId(usuarioLogado.idUsuario)
                if (perfilLogado?.idProfissional != profissionalId) {
                    log.warn("Acesso negado: Prof ${perfilLogado?.idProfissional} tentou alterar duração para Prof $profissionalId")
                    throw NaoAutorizadoException("Profissionais só podem alterar a própria duração.")
                }
            } else {
                throw NaoAutorizadoException("Usuário não autorizado.")
            }
        }

        if (novaDuracao <= 0) throw InputInvalidoException("Duração deve ser positiva.")

        profissional.duracaoPadraoMinutos = novaDuracao
        profissionalRepository.atualizar(profissional)
        log.info("Duração da consulta do Prof $profissionalId atualizada para $novaDuracao")
    }

    @OptIn(ExperimentalTime::class)
    suspend fun configurarHorarioPadraoEGerarAgendaInicial(
        profissionalId: String,
        novasRegras: List<HorarioTrabalho>,
        usuarioLogado: User
    ) {
        log.info("User ${usuarioLogado.idUsuario} configurando agenda para Prof $profissionalId")
        val profissional = profissionalRepository.buscarPorId(profissionalId)
            ?: throw RecursoNaoEncontradoException("Profissional não encontrado.")

        if (usuarioLogado.isSuperAdmin || usuarioLogado.role == Role.RECEPCIONISTA) {
            // OK
        } else {
            when (usuarioLogado.role) {
                Role.PROFISSIONAL -> {
                    val perfilLogado = profissionalRepository.buscarPorUserId(usuarioLogado.idUsuario)
                    if (perfilLogado?.idProfissional != profissionalId) {
                        log.warn("Acesso negado: Prof ${perfilLogado?.idProfissional} tentou configurar agenda do Prof $profissionalId")
                        throw NaoAutorizadoException("Profissionais só podem configurar sua própria agenda.")
                    }
                }
                Role.PACIENTE -> throw NaoAutorizadoException("Pacientes não podem configurar agendas.")
                else -> {}
            }
        }

        val idsConsultoriosRequisitados = novasRegras.map { it.consultorioId }.toSet()
        log.debug("Validando ${idsConsultoriosRequisitados.size} IDs de consultório...")

        val consultoriosEncontrados = consultorioRepository.listarTodos()
            .filter { idsConsultoriosRequisitados.contains(it.idConsultorio) }
            .map { it.idConsultorio }
            .toSet()

        val idsFaltantes = idsConsultoriosRequisitados - consultoriosEncontrados
        if (idsFaltantes.isNotEmpty()) {
            log.warn("Configuração de agenda falhou: IDs de consultório não encontrados: $idsFaltantes")
            throw RecursoNaoEncontradoException("Um ou mais Consultórios não foram encontrados. IDs inválidos: $idsFaltantes. Operação cancelada.")
        }
        log.debug("Validação de consultório OK.")

        profissional.diasDeTrabalho = novasRegras
        profissional.agenda.horariosDisponiveis.clear()
        profissional.agenda.horariosBloqueados.clear()

        val hoje = Clock.System.todayIn(fusoHorarioPadrao)
        val dataFutura = hoje.plus(4 * 7, DateTimeUnit.DAY)

        log.info("Gerando disponibilidade inicial de $hoje até $dataFutura para Prof $profissionalId")
        agendaService.gerarDisponibilidadePadrao(
            agenda = profissional.agenda,
            regras = novasRegras,
            dataInicio = hoje,
            dataFim = dataFutura
        )

        profissionalRepository.atualizar(profissional)
        log.info("Agenda do Prof $profissionalId configurada e salva.")
    }

    @OptIn(ExperimentalTime::class)
    suspend fun listarPromocoesDisponiveisParaAtivar(
        profissionalId: String,
        usuarioLogado: User
    ): List<PromocaoResponse> {
        log.debug("User ${usuarioLogado.idUsuario} listando promoções disponíveis para Prof $profissionalId")
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
        log.debug("Encontradas ${promocoes.size} promoções disponíveis para Prof $profissionalId")
        return promocoes.map { it.toResponse() }
    }

    suspend fun ativarPromocao(
        profissionalId: String,
        promocaoId: String,
        usuarioLogado: User
    ) {
        log.info("User ${usuarioLogado.idUsuario} ativando Promoção $promocaoId para Prof $profissionalId")
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
            log.info("Promoção $promocaoId ativada para Prof $profissionalId")
        } else {
            log.info("Promoção $promocaoId já estava ativa para Prof $profissionalId. Nenhuma ação tomada.")
        }
    }

    suspend fun desativarPromocao(
        profissionalId: String,
        promocaoId: String,
        usuarioLogado: User
    ) {
        log.info("User ${usuarioLogado.idUsuario} desativando Promoção $promocaoId para Prof $profissionalId")
        val profissional = profissionalRepository.buscarPorId(profissionalId)
            ?: throw RecursoNaoEncontradoException("Profissional não encontrado.")

        if (profissional.promocoesAtivadasIds.contains(promocaoId)) {
            profissional.promocoesAtivadasIds = profissional.promocoesAtivadasIds.filterNot { it == promocaoId }
            profissionalRepository.atualizar(profissional)
            log.info("Promoção $promocaoId desativada para Prof $profissionalId")
        } else {
            log.info("Promoção $promocaoId já estava desativada para Prof $profissionalId. Nenhuma ação tomada.")
        }
    }

    suspend fun listarProfissionaisAtivos(usuarioLogado: User): List<ProfissionalResponse> {
        log.info("User ${usuarioLogado.idUsuario} listando todos os profissionais ativos.")
        if (!usuarioLogado.isSuperAdmin && usuarioLogado.role != Role.RECEPCIONISTA) {
            throw NaoAutorizadoException("Apenas Admins ou Recepcionistas podem listar todos os profissionais.")
        }

        val profissionais = profissionalRepository.listarTodosAtivos()
        log.debug("Encontrados ${profissionais.size} profissionais ativos.")

        return coroutineScope {
            profissionais.map {
                async { it.toResponse(userRepository, areaAtuacaoRepository) }
            }.awaitAll()
        }
    }

}