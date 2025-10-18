package com.consultorioAPI.models

import java.util.UUID

data class Consultorio(
    val idConsultorio: String = UUID.randomUUID().toString(),
    val nomeConsultorio: String,
    val endereco: String
)