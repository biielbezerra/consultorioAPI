package consultorioAPI.dtos

import com.consultorioAPI.models.TipoPromocao
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
@Serializable
data class CriarPromocaoRequest(
    val descricao: String,
    val percentualDesconto: Double,
    val dataInicio: Instant,
    val dataFim: Instant,
    val tipoPromocao: TipoPromocao,
    val codigoOpcional: String? = null,
    val profissionalIdAplicavel: String? = null,
    val isCumulativa: Boolean = false,
    val quantidadeMinima: Int? = null
)

@Serializable
data class CriarConsultorioRequest(
    val nome: String,
    val endereco: String
)

@Serializable
data class TransferirAdminRequest(
    val novoEmail: String,
    val excluirContaAntiga: Boolean
)