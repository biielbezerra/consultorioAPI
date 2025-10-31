package consultorioAPI.dtos

import com.consultorioAPI.models.Role
import kotlinx.serialization.Serializable

@Serializable
data class PreCadastroEquipeRequest(
    val nome: String,
    val email: String,
    val role: Role,
    val areaAtuacaoId: String,
    val atributos: Map<String, String>? = null
)

@Serializable
data class CompletarCadastroRequest(
    val token: String,
    val senhaNova: String
)

@Serializable
data class CriarPacienteRequest(
    val userId: String,
    val nome: String,
    val email: String
)

@Serializable
data class AtualizarStatusRequest(
    val novoStatus: String
)

@Serializable
data class RecusarConviteRequest(
    val token: String
)

@Serializable
data class DeletarUsuarioRequest(
    val bloquearEmail: Boolean = false
)

@Serializable
data class EmailRequest(
    val email: String
)

@Serializable
data class RegistroPacienteRequest(
    val nome: String,
    val email: String,
    val senha: String
)

data class AtualizarMeuPerfilRequest(
    val nome: String? = null,
    val telefone: String? = null
)

@Serializable
data class AtualizarMinhaSenhaRequest(
    val novaSenha: String
)

@Serializable
data class LinkarPerfilRequest(
    val userId: String,
    val role: Role,

    val nome: String,
    val areaAtuacaoId: String? = null,
    val atributos: Map<String, String>? = null
)