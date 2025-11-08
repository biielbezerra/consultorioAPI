package com.consultorioAPI.models

import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
data class AreaAtuacao(
    var idArea: String = UUID.randomUUID().toString(),
    val nome: String,
    val nomeRegistro: String? = null
)
