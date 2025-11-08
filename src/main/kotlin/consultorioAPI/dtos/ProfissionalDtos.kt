package consultorioAPI.dtos

import com.consultorioAPI.models.HorarioTrabalho
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable
data class AtualizarValorRequest(
    val novoValor: Double
)

@Serializable
data class AtualizarDuracaoRequest(
    val novaDuracaoMinutos: Int
)

@Serializable
data class ConfigurarAgendaRequest(
    val novasRegras: List<HorarioTrabalho>
)

@Serializable
data class DefinirFolgaRequest(
    val diaDeFolga: LocalDate
)

@Serializable
data class HorariosDisponiveisResponse(
    val horarios: List<String>
)

@Serializable
data class StatusAgendaResponse(
    val statusSlots: Map<String, String>
)

@Serializable
data class IntervaloRequest(
    val dataHoraInicio: LocalDateTime,
    val dataHoraFim: LocalDateTime
)