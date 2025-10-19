package com.consultorioAPI.models

import java.util.UUID

data class Recepcionista(
    val idRecepcionista: String = UUID.randomUUID().toString(),
    val nomeRecepcionista: String,
    val userId: String,
    var status: StatusUsuario = StatusUsuario.CONVIDADO
)
