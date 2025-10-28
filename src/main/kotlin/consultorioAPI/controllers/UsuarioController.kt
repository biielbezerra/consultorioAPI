package consultorioAPI.controllers

import com.consultorioAPI.config.FirebasePrincipal
import com.consultorioAPI.exceptions.EmailBloqueadoException
import consultorioAPI.dtos.AtualizarStatusRequest
import consultorioAPI.dtos.CompletarCadastroRequest
import consultorioAPI.dtos.CriarPacienteRequest
import consultorioAPI.dtos.PreCadastroEquipeRequest
import com.consultorioAPI.models.StatusUsuario
import com.consultorioAPI.models.User
import com.consultorioAPI.services.UsuarioService
import consultorioAPI.dtos.DeletarUsuarioRequest
import consultorioAPI.dtos.EmailRequest
import consultorioAPI.dtos.RecusarConviteRequest
import consultorioAPI.dtos.RegistroPacienteRequest
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*

@kotlinx.serialization.Serializable
data class OnboardingRequest(val nome: String)

class UsuarioController(private val usuarioService: UsuarioService) {

    // criarPerfilPacienteAposAuth da UsuarioService
    suspend fun criarPerfilPaciente(call: ApplicationCall) {
        try {
            val principal = call.principal<FirebasePrincipal>()
                ?: return call.respond(HttpStatusCode.Unauthorized)

            val userEmail = principal.email
            if (userEmail == null || userEmail.isBlank()) {
                return call.respond(HttpStatusCode.BadRequest, "Token não contém e-mail válido.")
            }

            val request = call.receive<OnboardingRequest>()

            val pacienteCriado = usuarioService.criarPerfilPacienteAposAuth(
                userId = principal.uid,
                nome = request.nome,
                email = userEmail
            )

            call.respond(HttpStatusCode.Created, pacienteCriado)

        } catch (e: IllegalStateException) {
            call.respond(HttpStatusCode.Conflict, mapOf("erro" to e.message))
        } catch (e: EmailBloqueadoException) {
            call.respond(HttpStatusCode.Forbidden, mapOf("erro" to e.message))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("erro" to "Erro inesperado: ${e.message}"))
        }
    }

    suspend fun registrarPaciente(call: ApplicationCall) {
        try {
            val request = call.receive<RegistroPacienteRequest>()

            val pacienteCriado = usuarioService.registrarNovoPaciente(request)

            call.respond(HttpStatusCode.Created, pacienteCriado)

        } catch (e: EmailBloqueadoException) {
            call.respond(HttpStatusCode.Forbidden, mapOf("erro" to e.message))
        } catch (e: IllegalArgumentException) {
            call.respond(HttpStatusCode.BadRequest, mapOf("erro" to e.message))
        } catch (e: IllegalStateException) {
            call.respond(HttpStatusCode.Conflict, mapOf("erro" to e.message))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("erro" to (e.message ?: "Erro interno")))
        }
    }

    suspend fun preCadastrarEquipe(call: ApplicationCall) {
        try {
            val usuarioLogado = call.principal<User>()
                ?: return call.respond(HttpStatusCode.Unauthorized, "Apenas usuários logados podem fazer isso")

            val request = call.receive<PreCadastroEquipeRequest>()

            val usuarioCriado = usuarioService.preCadastrarEquipe(
                nome = request.nome,
                email = request.email,
                role = request.role,
                areaAtuacaoId = request.areaAtuacaoId,
                atributos = request.atributos,
                usuarioLogado = usuarioLogado
            )

            call.respond(HttpStatusCode.Created, usuarioCriado)

        } catch (e: SecurityException) {
            call.respond(HttpStatusCode.Forbidden, mapOf("erro" to e.message))
        } catch (e: IllegalArgumentException) {
            call.respond(HttpStatusCode.BadRequest, mapOf("erro" to e.message))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("erro" to (e.message ?: "Erro interno")))
        }
    }

    suspend fun completarCadastro(call: ApplicationCall) {
        try {
            val request = call.receive<CompletarCadastroRequest>()

            val usuarioAtivado = usuarioService.completarCadastro(
                token = request.token,
                senhaNova = request.senhaNova
            )
            call.respond(HttpStatusCode.OK, usuarioAtivado)

        } catch (e: IllegalArgumentException) {
            call.respond(HttpStatusCode.BadRequest, mapOf("erro" to e.message))
        } catch (e: IllegalStateException) {
            call.respond(HttpStatusCode.Conflict, mapOf("erro" to e.message))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("erro" to (e.message ?: "Erro interno")))
        }
    }

    suspend fun recusarConvite(call: ApplicationCall) {
        try {
            // Esta é uma rota pública, não precisa de autenticação
            val request = call.receive<RecusarConviteRequest>()

            usuarioService.recusarConvite(request.token)

            call.respond(HttpStatusCode.OK, mapOf("sucesso" to "Convite recusado"))

        } catch (e: IllegalArgumentException) {
            call.respond(HttpStatusCode.BadRequest, mapOf("erro" to e.message))
        } catch (e: IllegalStateException) {
            call.respond(HttpStatusCode.Conflict, mapOf("erro" to e.message))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("erro" to (e.message ?: "Erro interno")))
        }
    }

    suspend fun reenviarConvite(call: ApplicationCall) {
        try {
            val usuarioLogado = call.principal<User>() ?: return call.respond(HttpStatusCode.Unauthorized)
            val userIdAlvo = call.parameters["id"]
                ?: return call.respond(HttpStatusCode.BadRequest, "ID do usuário não fornecido")

            usuarioService.reenviarConvite(
                userIdAlvo = userIdAlvo,
                usuarioLogado = usuarioLogado
            )

            call.respond(HttpStatusCode.OK, mapOf("sucesso" to "Convite reenviado"))

        } catch (e: SecurityException) {
            call.respond(HttpStatusCode.Forbidden, mapOf("erro" to e.message))
        } catch (e: IllegalArgumentException) {
            call.respond(HttpStatusCode.BadRequest, mapOf("erro" to e.message))
        } catch (e: IllegalStateException) {
            call.respond(HttpStatusCode.Conflict, mapOf("erro" to e.message))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("erro" to (e.message ?: "Erro interno")))
        }
    }

    suspend fun atualizarStatusEquipe(call: ApplicationCall) {
        try {
            val usuarioLogado = call.principal<User>()
                ?: return call.respond(HttpStatusCode.Unauthorized)

            val userIdAlvo = call.parameters["id"]
                ?: return call.respond(HttpStatusCode.BadRequest, "ID do usuário não fornecido")

            val request = call.receive<AtualizarStatusRequest>()

            val novoStatusEnum = try {
                StatusUsuario.valueOf(request.novoStatus.uppercase())
            } catch (e: Exception) {
                return call.respond(HttpStatusCode.BadRequest, "Status inválido: ${request.novoStatus}")
            }

            usuarioService.atualizarStatusEquipe(
                userIdAlvo = userIdAlvo,
                novoStatus = novoStatusEnum,
                usuarioLogado = usuarioLogado
            )

            call.respond(HttpStatusCode.OK, mapOf("sucesso" to "Status atualizado"))

        } catch (e: SecurityException) {
            call.respond(HttpStatusCode.Forbidden, mapOf("erro" to e.message))
        } catch (e: IllegalArgumentException) {
            call.respond(HttpStatusCode.BadRequest, mapOf("erro" to e.message))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("erro" to (e.message ?: "Erro interno")))
        }
    }

    suspend fun deletarUsuario(call: ApplicationCall) {
        try {
            val usuarioLogado = call.principal<User>() ?: return call.respond(HttpStatusCode.Unauthorized)
            val userIdAlvo = call.parameters["id"]
                ?: return call.respond(HttpStatusCode.BadRequest, "ID do usuário não fornecido")

            val request = call.receive<DeletarUsuarioRequest>()

            usuarioService.deletarUsuario(
                userIdAlvo = userIdAlvo,
                usuarioLogado = usuarioLogado,
                bloquearEmail = request.bloquearEmail
            )

            call.respond(HttpStatusCode.OK, mapOf("sucesso" to "Usuário deletado"))

        } catch (e: SecurityException) {
            call.respond(HttpStatusCode.Forbidden, mapOf("erro" to e.message))
        } catch (e: IllegalArgumentException) {
            call.respond(HttpStatusCode.BadRequest, mapOf("erro" to e.message))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("erro" to (e.message ?: "Erro interno")))
        }
    }

    suspend fun desbloquearEmail(call: ApplicationCall) {
        try {
            val usuarioLogado = call.principal<User>() ?: return call.respond(HttpStatusCode.Unauthorized)
            val request = call.receive<EmailRequest>()

            usuarioService.desbloquearEmailPaciente(
                emailAlvo = request.email,
                usuarioLogado = usuarioLogado
            )

            call.respond(HttpStatusCode.OK, mapOf("sucesso" to "Email ${request.email} desbloqueado"))

        } catch (e: SecurityException) {
            call.respond(HttpStatusCode.Forbidden, mapOf("erro" to e.message))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("erro" to (e.message ?: "Erro interno")))
        }
    }

    suspend fun listarEmailsBloqueados(call: ApplicationCall) {
        try {
            val usuarioLogado = call.principal<User>() ?: return call.respond(HttpStatusCode.Unauthorized)

            val emails = usuarioService.listarEmailsBloqueados(usuarioLogado)

            call.respond(HttpStatusCode.OK, emails)

        } catch (e: SecurityException) {
            call.respond(HttpStatusCode.Forbidden, mapOf("erro" to e.message))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("erro" to (e.message ?: "Erro interno")))
        }
    }
}