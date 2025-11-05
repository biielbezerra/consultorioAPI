package com.consultorioAPI.services

import com.consultorioAPI.exceptions.*
import com.consultorioAPI.models.*
import com.consultorioAPI.repositories.*
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class PromocaoService(
    private val promocaoRepository: PromocaoRepository,
    private val consultaRepository: ConsultaRepository,
    private val pacienteService: PacienteService,
    private val profissionalRepository: ProfissionalRepository
) {

    @OptIn(ExperimentalTime::class)
    suspend fun criarPromocao(
        descricao: String,
        percentualDesconto: Double,
        dataInicio: Instant,
        dataFim: Instant,
        tipoPromocao: TipoPromocao,
        codigoOpcional: String? = null,
        profissionalIdAplicavel: String? = null,
        isCumulativa: Boolean = false,
        quantidadeMinima: Int? = null,
        checarDataDaConsulta: Boolean = false,
        usuarioLogado: User
    ): Promocao {

        if (tipoPromocao == TipoPromocao.PACOTE) {
            if (quantidadeMinima == null || quantidadeMinima <= 1) {
                throw InputInvalidoException("Promoções do tipo PACOTE devem ter uma quantidade mínima de consultas maior que 1.")
            }
        } else if (quantidadeMinima != null) {
            throw InputInvalidoException("Apenas promoções do tipo PACOTE podem ter uma quantidade mínima de consultas.")
        }

        if (percentualDesconto <= 0 || percentualDesconto > 100) {
            throw InputInvalidoException("Percentual de desconto inválido.")
        }
        if (dataInicio >= dataFim) {
            throw InputInvalidoException("Data de início deve ser anterior à data de fim.")
        }
        if (tipoPromocao == TipoPromocao.CODIGO && codigoOpcional.isNullOrBlank()) {
            throw InputInvalidoException("Promoções do tipo CÓDIGO devem ter um código.")
        }
        if (tipoPromocao != TipoPromocao.CODIGO && !codigoOpcional.isNullOrBlank()) {
            throw InputInvalidoException("Apenas promoções do tipo CÓDIGO podem ter um código opcional.")
        }

        val escopoPromocao: PromocaoEscopo
        var idProfissionalFinal: String? = null

        if (usuarioLogado.isSuperAdmin || usuarioLogado.role == Role.RECEPCIONISTA) {
            escopoPromocao = PromocaoEscopo.GLOBAL
            idProfissionalFinal = profissionalIdAplicavel
        } else {
            when (usuarioLogado.role) {
                Role.PROFISSIONAL -> {
                    escopoPromocao = PromocaoEscopo.PROFISSIONAL
                    val perfilProfissional = profissionalRepository.buscarPorUserId(usuarioLogado.idUsuario)
                        ?: throw RecursoNaoEncontradoException("Perfil profissional não encontrado para este usuário.")
                    idProfissionalFinal = perfilProfissional.idProfissional
                }
                Role.PACIENTE -> {
                    throw NaoAutorizadoException("Pacientes não podem criar promoções.")
                }
                else -> {
                    escopoPromocao = PromocaoEscopo.GLOBAL
                    idProfissionalFinal = profissionalIdAplicavel
                }
            }
        }

        val novaPromocao = Promocao(
            descricao = descricao,
            percentualDesconto = percentualDesconto,
            dataInicio = dataInicio,
            dataFim = dataFim,
            tipoPromocao = tipoPromocao,
            codigoOpcional = codigoOpcional?.trim()?.uppercase(),
            profissionalIdAplicavel = idProfissionalFinal,
            criadoPorUserId = usuarioLogado.idUsuario,
            escopo = escopoPromocao,
            quantidadeMinimaConsultas = quantidadeMinima,
            checarDataDaConsulta = checarDataDaConsulta,
            isCumulativa = isCumulativa
        )

        return promocaoRepository.salvar(novaPromocao)
    }

    suspend fun deletarPromocao(promocaoId: String, usuarioLogado: User) {
        val promocao = promocaoRepository.buscarPorId(promocaoId)
            ?: throw RecursoNaoEncontradoException("Promoção não encontrada.")

        if (usuarioLogado.isSuperAdmin || usuarioLogado.role == Role.RECEPCIONISTA) {
        } else if (usuarioLogado.role == Role.PROFISSIONAL) {
            if (promocao.criadoPorUserId != usuarioLogado.idUsuario) {
                throw NaoAutorizadoException("Profissionais só podem deletar promoções criadas por eles mesmos.")
            }
        } else {
            throw NaoAutorizadoException("Usuário não autorizado a deletar promoções.")
        }

        // TODO: O que acontece com consultas futuras que tinham essa promoção?

        promocao.isDeletado = true
        promocao.isAtiva = false
        promocaoRepository.atualizar(promocao)
    }

    @OptIn(ExperimentalTime::class)
    suspend fun buscarMelhorPromocaoAplicavel(
        paciente: Paciente,
        profissional: Profissional,
        dataConsultaProposta: Instant,
        quantidadeConsultasSimultaneas: Int,
        codigoPromocionalInput: String? = null
    ): List<Promocao> {
        val agora = Clock.System.now()
        val promocoesAplicaveis = mutableListOf<Promocao>()

        val candidatasRepo = promocaoRepository.listarTodas()
            .filter { promo ->
                if (promo.checarDataDaConsulta) {
                    // Cenário "Black Friday": A *data da consulta* deve estar na janela
                    promo.isAtiva && dataConsultaProposta >= promo.dataInicio && dataConsultaProposta <= promo.dataFim
                } else {
                    // Cenário "Compre Hoje": A *data de hoje* (compra) deve estar na janela
                    promo.isAtiva && agora >= promo.dataInicio && agora <= promo.dataFim
                }
            }

        val candidatasFiltradas = candidatasRepo.filter { promo ->
            when (promo.escopo) {
                PromocaoEscopo.GLOBAL -> profissional.promocoesAtivadasIds.contains(promo.idPromocao) &&
                        (promo.profissionalIdAplicavel == null || promo.profissionalIdAplicavel == profissional.idProfissional)
                PromocaoEscopo.PROFISSIONAL -> promo.profissionalIdAplicavel == profissional.idProfissional
            }
        }

        val consultasAnteriores = consultaRepository.buscarPorPacienteId(paciente.idPaciente)
        val isFiel = pacienteService.isClienteFiel(paciente)

        val promocoesValidadas = candidatasFiltradas.filter { promo ->
            when (promo.tipoPromocao) {
                TipoPromocao.PRIMEIRA_DUPLA -> consultasAnteriores.isEmpty() && quantidadeConsultasSimultaneas >= 2
                TipoPromocao.CLIENTE_FIEL -> isFiel
                TipoPromocao.PACOTE -> {
                    promo.quantidadeMinimaConsultas != null &&
                            quantidadeConsultasSimultaneas >= promo.quantidadeMinimaConsultas!!
                }
                TipoPromocao.GERAL_PERIODO, TipoPromocao.CODIGO -> true
            }
        }

        val promocaoPorCodigoEncontrada: Promocao? = if (!codigoPromocionalInput.isNullOrBlank()) {
            val codigoLimpo = codigoPromocionalInput.trim().uppercase()

            if (paciente.codigosPromocionaisUsados.contains(codigoLimpo)) {
                null
            } else {
                promocoesValidadas.firstOrNull {
                    it.tipoPromocao == TipoPromocao.CODIGO &&
                            it.codigoOpcional?.trim()?.uppercase() == codigoLimpo
                }
            }
        } else null

        val cumulativasNaoCodigo = promocoesValidadas.filter { it.isCumulativa && it.tipoPromocao != TipoPromocao.CODIGO }
        val naoCumulativasNaoCodigo = promocoesValidadas.filterNot { it.isCumulativa || it.tipoPromocao == TipoPromocao.CODIGO }

        val melhorNaoCumulativaNaoCodigo = naoCumulativasNaoCodigo.maxByOrNull { it.percentualDesconto }

        if (promocaoPorCodigoEncontrada != null) {
            promocoesAplicaveis.add(promocaoPorCodigoEncontrada)
            promocoesAplicaveis.addAll(cumulativasNaoCodigo)

            if (!promocaoPorCodigoEncontrada.isCumulativa && melhorNaoCumulativaNaoCodigo != null) {
                println("Aviso: Código ${promocaoPorCodigoEncontrada.codigoOpcional} não cumulativo sobrepôs outra promoção não cumulativa.")
            }
        } else {
            if (melhorNaoCumulativaNaoCodigo != null) {
                promocoesAplicaveis.add(melhorNaoCumulativaNaoCodigo)
            }
            promocoesAplicaveis.addAll(cumulativasNaoCodigo)
        }

        return promocoesAplicaveis.distinctBy { it.idPromocao }
    }

    suspend fun buscarPromocoesPorIds(ids: List<String>): List<Promocao> {
        return promocaoRepository.listarTodas(incluirDeletados = true)
            .filter { ids.contains(it.idPromocao) }
    }

    suspend fun buscarCodigoOriginalPorIds(ids: List<String>): String? {
        val promocoes = buscarPromocoesPorIds(ids)
        return promocoes.firstOrNull { it.tipoPromocao == TipoPromocao.CODIGO }?.codigoOpcional
    }

    suspend fun listarTodasPromocoes(usuarioLogado: User): List<Promocao> {
        if (!usuarioLogado.isSuperAdmin && usuarioLogado.role != Role.RECEPCIONISTA) {
            throw NaoAutorizadoException("Apenas Admins ou Recepcionistas podem listar todas as promoções.")
        }
        return promocaoRepository.listarTodas(incluirDeletados = false)
    }

}