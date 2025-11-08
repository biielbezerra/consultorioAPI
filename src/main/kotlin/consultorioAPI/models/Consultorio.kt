package com.consultorioAPI.models

import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
data class Consultorio(
    var idConsultorio: String = UUID.randomUUID().toString(),
    val nomeConsultorio: String,
    val endereco: String
)