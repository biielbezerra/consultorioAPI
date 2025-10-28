package com.consultorioAPI.models

import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
data class Recepcionista(
    val idRecepcionista: String = "",
    val nomeRecepcionista: String,
    val userId: String,
    var status: StatusUsuario = StatusUsuario.CONVIDADO,
    var conviteToken: String? = null,
    var isDeletado: Boolean = false
)
