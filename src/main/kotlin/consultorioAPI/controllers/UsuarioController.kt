package consultorioAPI.controllers

import com.consultorioAPI.config.FirebasePrincipal
import com.consultorioAPI.exceptions.EmailBloqueadoException
import com.consultorioAPI.exceptions.InputInvalidoException
import com.consultorioAPI.exceptions.NaoAutorizadoException
import consultorioAPI.dtos.AtualizarStatusRequest
import consultorioAPI.dtos.CompletarCadastroRequest
import consultorioAPI.dtos.CriarPacienteRequest
import consultorioAPI.dtos.PreCadastroEquipeRequest
import com.consultorioAPI.models.StatusUsuario
import com.consultorioAPI.models.User
import com.consultorioAPI.services.UsuarioService
import consultorioAPI.dtos.AtualizarMeuPerfilRequest
import consultorioAPI.dtos.AtualizarMinhaSenhaRequest
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

    suspend fun registrarPaciente(call: ApplicationCall) {
        val request = call.receive<RegistroPacienteRequest>()
        val pacienteCriado = usuarioService.registrarNovoPaciente(request)
        call.respond(HttpStatusCode.Created, pacienteCriado)
    }

    suspend fun preCadastrarEquipe(call: ApplicationCall) {
        val usuarioLogado = call.principal<User>()
            ?: throw NaoAutorizadoException("Usuário não autenticado.")
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
    }

    suspend fun completarCadastro(call: ApplicationCall) {
        val request = call.receive<CompletarCadastroRequest>()
        val usuarioAtivado = usuarioService.completarCadastro(request.token, request.senhaNova)
        call.respond(HttpStatusCode.OK, usuarioAtivado)
    }

    suspend fun recusarConvite(call: ApplicationCall) {
        val request = call.receive<RecusarConviteRequest>()
        usuarioService.recusarConvite(request.token)
        call.respond(HttpStatusCode.OK, mapOf("sucesso" to "Convite recusado"))
    }

    suspend fun reenviarConvite(call: ApplicationCall) {
        val usuarioLogado = call.principal<User>()
            ?: throw NaoAutorizadoException("Usuário não autenticado.")

        val userIdAlvo = call.parameters["id"]
            ?: throw InputInvalidoException("ID do usuário não fornecido")

        usuarioService.reenviarConvite(userIdAlvo, usuarioLogado)
        call.respond(HttpStatusCode.OK, mapOf("sucesso" to "Convite reenviado"))
    }

    suspend fun atualizarStatusEquipe(call: ApplicationCall) {
        val usuarioLogado = call.principal<User>()
            ?: throw NaoAutorizadoException("Usuário não autenticado.")

        val userIdAlvo = call.parameters["id"]
            ?: throw InputInvalidoException("ID do usuário não fornecido")

        val request = call.receive<AtualizarStatusRequest>()

        val novoStatusEnum: StatusUsuario
        try {
            novoStatusEnum = StatusUsuario.valueOf(request.novoStatus.uppercase())
        } catch (e: Exception) {
            throw InputInvalidoException("Status inválido: ${request.novoStatus}. Valores permitidos: ${StatusUsuario.entries.joinToString()}")
        }

        usuarioService.atualizarStatusEquipe(
            userIdAlvo = userIdAlvo,
            novoStatus = novoStatusEnum,
            usuarioLogado = usuarioLogado
        )
        call.respond(HttpStatusCode.OK, mapOf("sucesso" to "Status atualizado"))
    }

    suspend fun buscarMeuPerfil(call: ApplicationCall) {
        val usuarioLogado = call.principal<User>()
            ?: throw NaoAutorizadoException("Usuário não autenticado.")

        val perfil = usuarioService.buscarMeuPerfil(usuarioLogado)
        call.respond(HttpStatusCode.OK, perfil)
    }

    suspend fun atualizarMeuPerfil(call: ApplicationCall) {
        val usuarioLogado = call.principal<User>()
            ?: throw NaoAutorizadoException("Usuário não autenticado.")

        val dto = call.receive<AtualizarMeuPerfilRequest>()
        val perfilAtualizado = usuarioService.atualizarMeuPerfil(usuarioLogado, dto)
        call.respond(HttpStatusCode.OK, perfilAtualizado)
    }

    suspend fun atualizarMinhaSenha(call: ApplicationCall) {
        val usuarioLogado = call.principal<User>()
            ?: throw NaoAutorizadoException("Usuário não autenticado.")

        val dto = call.receive<AtualizarMinhaSenhaRequest>()
        usuarioService.atualizarMinhaSenha(usuarioLogado, dto)
        call.respond(HttpStatusCode.OK, mapOf("sucesso" to "Senha atualizada com sucesso."))
    }

    suspend fun deletarUsuario(call: ApplicationCall) {
        val usuarioLogado = call.principal<User>()
            ?: throw NaoAutorizadoException("Usuário não autenticado.")

        val userIdAlvo = call.parameters["id"]
            ?: throw InputInvalidoException("ID do usuário não fornecido")

        val request = call.receive<DeletarUsuarioRequest>()

        usuarioService.deletarUsuario(userIdAlvo, usuarioLogado, request.bloquearEmail)
        call.respond(HttpStatusCode.OK, mapOf("sucesso" to "Usuário deletado"))
    }

    suspend fun deletarMinhaConta(call: ApplicationCall) {
        val usuarioLogado = call.principal<User>()
            ?: throw NaoAutorizadoException("Usuário não autenticado.")

        usuarioService.deletarMinhaConta(usuarioLogado)
        call.respond(HttpStatusCode.OK, mapOf("sucesso" to "Sua conta foi excluída com sucesso."))
    }

    suspend fun desbloquearEmail(call: ApplicationCall) {
        val usuarioLogado = call.principal<User>()
            ?: throw NaoAutorizadoException("Usuário não autenticado.")

        val request = call.receive<EmailRequest>()
        usuarioService.desbloquearEmailPaciente(request.email, usuarioLogado)
        call.respond(HttpStatusCode.OK, mapOf("sucesso" to "Email ${request.email} desbloqueado"))
    }

    suspend fun listarEmailsBloqueados(call: ApplicationCall) {
        val usuarioLogado = call.principal<User>()
            ?: throw NaoAutorizadoException("Usuário não autenticado.")

        val emails = usuarioService.listarEmailsBloqueados(usuarioLogado)
        call.respond(HttpStatusCode.OK, emails)
    }
}