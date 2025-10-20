package com.consultorioAPI.models

import java.util.UUID

data class AreaAtuacao(
    val idArea: String = UUID.randomUUID().toString(),
    val nome: String,
    val nomeRegistro: String? = null
)
