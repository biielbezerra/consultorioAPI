package com.consultorioAPI.models

import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class User(
    val idUsuario: String,
    var email: String,
    var role: Role,
    var status: StatusUsuario = StatusUsuario.CONVIDADO,
    var isDeletado: Boolean = false,
    @SerialName("is_super_admin")
    var isSuperAdmin: Boolean = false,
    @SerialName("convite_token")
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