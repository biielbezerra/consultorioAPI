// Em Routing.kt
package com

import com.consultorioAPI.config.FirebasePrincipal
import com.consultorioAPI.exceptions.EmailBloqueadoException
import com.consultorioAPI.repositories.impl.* // Importar todas as suas implementações
import com.consultorioAPI.services.* // Importar todos os seus serviços
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
data class OnboardingRequest(val nome: String)

fun Application.configureRouting() {

    val userRepository = SupabaseUserRepository()
    val pacienteRepository = SupabasePacienteRepository()
    val profissionalRepository = SupabaseProfissionalRepository()
    val recepcionistaRepository = SupabaseRecepcionistaRepository()
    val areaAtuacaoRepository = SupabaseAreaAtuacaoRepository()
    val emailBlocklistRepository = SupabaseEmailBlocklistRepository()
    val consultaRepository = SupabaseConsultaRepository()
    val consultorioRepository = SupabaseConsultorioRepository()
    val promocaoRepository = SupabasePromocaoRepository()

    val pacienteService = PacienteService(consultaRepository)
    val agendaService = AgendaService(profissionalRepository)

    val promocaoService = PromocaoService(
        promocaoRepository,
        consultaRepository,
        pacienteService,
        profissionalRepository
    )

    val profissionalService = ProfissionalService(
        profissionalRepository,
        promocaoRepository,
        agendaService
    )

    val manutencaoService = ManutencaoService(
        pacienteRepository,
        profissionalRepository,
        pacienteService,
        agendaService
    )

    val consultaService = ConsultaService(
        pacienteService,
        consultaRepository,
        pacienteRepository,
        profissionalRepository
    )

    val usuarioService = UsuarioService(
        userRepository,
        pacienteRepository,
        profissionalRepository,
        recepcionistaRepository,
        agendaService,
        areaAtuacaoRepository,
        emailBlocklistRepository
    )

    val consultorioService = ConsultorioService(
        consultorioRepository,
        consultaRepository,
        pacienteService,
        agendaService,
        pacienteRepository,
        profissionalRepository,
        promocaoService,
        emailBlocklistRepository,
        userRepository,
        usuarioService
    )

    routing {
        get("/") {
            call.respondText("Hello World!")
        }

        authenticate("auth-firebase") {

            post("/pacientes/onboarding") {
                val principal = call.principal<FirebasePrincipal>()
                    ?: return@post call.respond(HttpStatusCode.Unauthorized)

                val request = call.receive<OnboardingRequest>()

                try {
                    val novoPaciente = usuarioService.criarPerfilPacienteAposAuth(
                        userId = principal.uid,
                        nome = request.nome,
                        email = principal.email ?: ""
                    )
                    call.respond(HttpStatusCode.Created, novoPaciente)
                } catch (e: IllegalStateException) {
                    call.respond(HttpStatusCode.Conflict, mapOf("error" to e.message))
                } catch (e: EmailBloqueadoException) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to e.message))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Erro inesperado: ${e.message}"))
                }
            }

            // Aqui você adicionará suas outras rotas, passando os serviços necessários
            // Ex:
            // consultaRoutes(consultaService, consultorioService)
            // profissionalRoutes(profissionalService)
            // adminRoutes(usuarioService, manutencaoService)
        }
    }
}