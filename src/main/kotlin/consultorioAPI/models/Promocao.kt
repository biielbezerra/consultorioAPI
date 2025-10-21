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
    private val idPromocaoArg: String? = null,
    val descricao: String,
    val percentualDesconto: Double,
    val dataInicio: Instant,
    val dataFim: Instant,
    val tipoPromocao: TipoPromocao,
    val codigoOpcional: String? = null,
    val profissionalIdAplicavel: String? = null,
    val criadoPorUserId: String,
    val escopo: PromocaoEscopo,
    var isCumulativa: Boolean = false,
    var isAtiva: Boolean = true,
    var isDeletado: Boolean = false
    ){
    val idPromocao: String = idPromocaoArg ?: UUID.randomUUID().toString()
}

@Serializable
enum class TipoPromocao {
    PRIMEIRA_DUPLA,
    CLIENTE_FIEL,
    GERAL_PERIODO,
    CODIGO
}