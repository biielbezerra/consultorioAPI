package com.consultorioAPI.models

import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
data class Consultorio(
    private val idConsultorioArg: String? = null,
    val nomeConsultorio: String,
    val endereco: String
){
    val idConsultorio: String = idConsultorioArg ?: UUID.randomUUID().toString()
}