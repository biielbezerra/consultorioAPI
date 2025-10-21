package com.consultorioAPI.models

import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
data class Recepcionista(
    val idRecepcionistaArg: String? = null,
    val nomeRecepcionista: String,
    val userId: String,
    var status: StatusUsuario = StatusUsuario.CONVIDADO,
    var conviteToken: String? = null,
    var isDeletado: Boolean = false
){
    val idRecepcionista: String = idRecepcionistaArg ?: UUID.randomUUID().toString()
}
