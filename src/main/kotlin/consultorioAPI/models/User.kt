package com.consultorioAPI.models

import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val idUsuario: String,
    var email: String,
    val role: Role,
    var isDeletado: Boolean = false
){
}

@Serializable
enum class StatusUsuario {
    CONVIDADO,
    ATIVO,
    RECUSADO,
    INATIVO
}