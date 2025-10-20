package com.consultorioAPI.models

import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
data class AreaAtuacao(
    val idAreaArg: String? = null,
    val nome: String,
    val nomeRegistro: String? = null
){
    val idArea: String = idAreaArg ?: UUID.randomUUID().toString()
}
