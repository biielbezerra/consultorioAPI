package com.consultorioAPI.models

import java.util.UUID

data class User(
    val idUsuario: String = UUID.randomUUID().toString(),
    var email: String,
    var senhaHash: String,
    val role: Role
)

enum class StatusUsuario {
    CONVIDADO,
    ATIVO,
    RECUSADO,
    INATIVO
}