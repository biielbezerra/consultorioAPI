package com.consultorioAPI.models

import kotlin.time.Instant
import kotlinx.serialization.Serializable
import java.util.UUID
import kotlin.time.ExperimentalTime

@Serializable
enum class PromocaoEscopo { GLOBAL, PROFISSIONAL }

@OptIn(ExperimentalTime::class)
@Serializable
data class Promocao(
    var idPromocao: String = UUID.randomUUID().toString(),
    val descricao: String,
    val percentualDesconto: Double,
    val dataInicio: Instant,
    val dataFim: Instant,
    val tipoPromocao: TipoPromocao,
    val codigoOpcional: String? = null,
    val profissionalIdAplicavel: String? = null,
    val criadoPorUserId: String,
    val escopo: PromocaoEscopo,
    var quantidadeMinimaConsultas: Int? = null,
    var checarDataDaConsulta: Boolean = false,
    var isCumulativa: Boolean = false,
    var isAtiva: Boolean = true,
    var isDeletado: Boolean = false
    )

@Serializable
enum class TipoPromocao {
    PRIMEIRA_DUPLA,
    CLIENTE_FIEL,
    GERAL_PERIODO,
    CODIGO,
    PACOTE
}