package consultorioAPI.dtos

import com.consultorioAPI.models.StatusConsulta
import kotlinx.serialization.Serializable
import kotlinx.datetime.LocalDateTime

@Serializable
data class AgendamentoRequest(
    val profissionalId: String,
    val dataHora: LocalDateTime,
    val pacienteEmail: String,
    val pacienteNome: String? = null,
    val codigoPromocional: String? = null
)

@Serializable
data class AgendamentoDuploRequest(
    val profissionalId: String,
    val dataHora1: LocalDateTime,
    val dataHora2: LocalDateTime,
    val pacienteEmail: String,
    val pacienteNome: String? = null,
    val codigoPromocional: String? = null
)

@Serializable
data class AgendamentoPacoteRequest(
    val profissionalId: String,
    val promocaoIdDoPacote: String,
    val dataPrimeiraConsulta: LocalDateTime,
    val pacienteEmail: String,
    val pacienteNome: String? = null,
    val codigoPromocional: String? = null
)

@Serializable
data class ReagendamentoRequest(
    val novaData: LocalDateTime
)

@Serializable
data class FinalizarConsultaRequest(
    val novoStatus: StatusConsulta
)