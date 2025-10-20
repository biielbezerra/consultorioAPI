package com.consultorioAPI.models

import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val idUsuarioArg: String? = null,
    var email: String,
    var senhaHash: String,
    val role: Role
){
    val idUsuario: String = idUsuarioArg ?: UUID.randomUUID().toString()
}

@Serializable
enum class StatusUsuario {
    CONVIDADO,
    ATIVO,
    RECUSADO,
    INATIVO
}