package com.consultorioAPI.models

import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
@Serializable
data class Paciente(
    var idPaciente: String = "",
    val nomePaciente: String,
    var telefone: String? = null,
    val userId: String,
    var status: StatusUsuario = StatusUsuario.ATIVO,
    var isDeletado: Boolean = false,
    var codigosPromocionaisUsados: List<String> = emptyList()
) {
    var dataCadastro: Instant? = null
}