package com.consultorioAPI.config

import io.ktor.server.request.path

import com.consultorioAPI.exceptions.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(val erro: String)

fun Application.configureStatusPages() {

    install(StatusPages) {

        // 400 Bad Request (Input Inválido)
        exception<InputInvalidoException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(cause.message ?: "Input inválido."))
        }

        // 401 Unauthorized (Pego pela segurança, mas bom ter aqui)
        status(HttpStatusCode.Unauthorized) { call, status ->
            call.respond(status, ErrorResponse("Não autorizado. Token inválido ou ausente."))
        }

        // 403 Forbidden (Sem permissão / E-mail bloqueado)
        exception<NaoAutorizadoException> { call, cause ->
            call.respond(HttpStatusCode.Forbidden, ErrorResponse(cause.message ?: "Acesso negado."))
        }
        exception<EmailBloqueadoException> { call, cause ->
            call.respond(HttpStatusCode.Forbidden, ErrorResponse(cause.message ?: "Este e-mail está bloqueado."))
        }

        // 404 Not Found (Recurso não existe)
        exception<RecursoNaoEncontradoException> { call, cause ->
            call.respond(HttpStatusCode.NotFound, ErrorResponse(cause.message ?: "Recurso não encontrado."))
        }
        status(HttpStatusCode.NotFound) { call, status -> // Pega 404 de rotas não encontradas
            call.respond(status, ErrorResponse("Recurso não encontrado: ${call.request.path()}"))
        }

        // 409 Conflict (Conflito de estado, ex: e-mail já existe)
        exception<ConflitoDeEstadoException> { call, cause ->
            call.respond(HttpStatusCode.Conflict, ErrorResponse(cause.message ?: "Conflito de estado."))
        }
        exception<PacienteInativoException> { call, cause ->
            // Você pode querer um DTO de erro especial aqui para enviar o pacienteId
            call.respond(HttpStatusCode.Conflict, ErrorResponse(cause.message ?: "Paciente inativo."))
        }
        exception<IllegalStateException> { call, cause -> // Pega o erro de rollback
            call.respond(HttpStatusCode.Conflict, ErrorResponse(cause.message ?: "Ação não pôde ser completada."))
        }

        // 500 Internal Server Error (Erros inesperados, ex: NullPointerException)
        exception<Throwable> { call, cause ->
            // Loga o erro completo no seu console (para você ver)
            call.application.log.error("Erro 500 não tratado capturado pelo StatusPages", cause)

            // Responde ao usuário com um JSON limpo
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Erro interno no servidor."))
        }
    }
}