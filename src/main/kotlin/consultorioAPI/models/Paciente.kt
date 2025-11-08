package com.consultorioAPI.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
@Serializable
data class Paciente(
    var idPaciente: String = UUID.randomUUID().toString(),
    var nomePaciente: String,
    var telefone: String? = null,
    val userId: String,
    var status: StatusUsuario = StatusUsuario.ATIVO,
    var isDeletado: Boolean = false,
    @SerialName("codigos_promocionais_usados")
    var codigosPromocionaisUsados: List<String> = emptyList()
) {
    var dataCadastro: Instant? = null
}