package com.consultorioAPI.models

import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
data class Recepcionista(
    val idRecepcionista: String = "",
    var nomeRecepcionista: String,
    var telefone: String? = null,
    val userId: String,
    var status: StatusUsuario = StatusUsuario.CONVIDADO,
    var isDeletado: Boolean = false
)
