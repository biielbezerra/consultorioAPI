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
            isCumulativa = isCumulativa
        )

        return promocaoRepository.salvar(novaPromocao)
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
            .filter { it.isAtiva && it.dataInicio <= agora && it.dataFim >= agora }

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
            promocoesValidadas.firstOrNull { it.tipoPromocao == TipoPromocao.CODIGO && it.codigoOpcional?.trim()?.uppercase() == codigoPromocionalInput.trim().uppercase() }
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

}