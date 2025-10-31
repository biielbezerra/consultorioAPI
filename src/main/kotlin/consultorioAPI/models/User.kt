package com.consultorioAPI.models

import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val idUsuario: String,
    var email: String,
    var role: Role,
    var status: StatusUsuario = StatusUsuario.CONVIDADO,
    var isDeletado: Boolean = false,
    var isSuperAdmin: Boolean = false,
    var conviteToken: String? = null
){
}

@Serializable
enum class StatusUsuario {
    CONVIDADO,
    ATIVO,
    RECUSADO,
    INATIVO
}