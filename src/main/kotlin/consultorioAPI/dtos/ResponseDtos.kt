package consultorioAPI.dtos

import com.consultorioAPI.models.PromocaoEscopo
import com.consultorioAPI.models.Role
import com.consultorioAPI.models.StatusConsulta
import com.consultorioAPI.models.StatusUsuario
import com.consultorioAPI.models.TipoPromocao
import kotlinx.serialization.Serializable
import kotlinx.datetime.LocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Serializable
data class AreaAtuacaoResponse(
    val idArea: String,
    val nome: String,
    val nomeRegistro: String?
)

@Serializable
data class ConsultorioResponse(
    val idConsultorio: String,
    val nome: String,
    val endereco: String
)

@OptIn(ExperimentalTime::class)
@Serializable
data class ConsultaResponse(
    val idConsulta: String,
    val dataHoraConsulta: Instant?,
    val statusConsulta: StatusConsulta,
    val duracaoEmMinutos: Int,
    val valorConsulta: Double,
    val nomePaciente: String?,
    val nomeProfissional: String?,
    val consultorio: ConsultorioResponse?,
    val areaAtuacao: AreaAtuacaoResponse?
)

@Serializable
data class PacienteResponse(
    val idPaciente: String,
    val userId: String,
    val email: String,
    val nomePaciente: String,
    val telefone: String?,
    val status: StatusUsuario
)

@Serializable
data class ProfissionalResponse(
    val idProfissional: String,
    val userId: String,
    val email: String,
    val nomeProfissional: String,
    val telefone: String?,
    val valorBaseConsulta: Double,
    val duracaoPadraoMinutos: Int,
    val areaAtuacao: AreaAtuacaoResponse,
    val atributosEspecificos: Map<String, String>,
    val status: StatusUsuario
)

@Serializable
data class RecepcionistaResponse(
    val idRecepcionista: String,
    val userId: String,
    val email: String,
    val nomeRecepcionista: String,
    val telefone: String?,
    val status: StatusUsuario
)

@Serializable
data class MeuPerfilResponse(
    val idUsuario: String,
    val email: String,
    val role: Role,
    val status: StatusUsuario,
    val isSuperAdmin: Boolean,

    val perfilPaciente: PacienteResponse? = null,
    val perfilProfissional: ProfissionalResponse? = null,
    val perfilRecepcionista: RecepcionistaResponse? = null
)

@OptIn(ExperimentalTime::class)
@Serializable
data class PromocaoResponse(
    val idPromocao: String,
    val descricao: String,
    val percentualDesconto: Double,
    val dataInicio: Instant,
    val dataFim: Instant,
    val tipoPromocao: TipoPromocao,
    val profissionalIdAplicavel: String?,
    val escopo: PromocaoEscopo
)