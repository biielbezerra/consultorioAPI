package consultorioAPI.controllers

import consultorioAPI.dtos.*
import com.consultorioAPI.models.User
import com.consultorioAPI.repositories.ProfissionalRepository
import com.consultorioAPI.services.AgendaService
import com.consultorioAPI.services.ProfissionalService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.datetime.LocalDate
import kotlin.time.Duration.Companion.minutes

class ProfissionalController(
    private val profissionalService: ProfissionalService,
    private val agendaService: AgendaService,
    private val profissionalRepository: ProfissionalRepository
) {

    suspend fun atualizarValorConsulta(call: ApplicationCall) {
        try {
            val usuarioLogado = call.principal<User>() ?: return call.respond(HttpStatusCode.Unauthorized)
            val profissionalId = call.parameters["id"] ?: return call.respond(HttpStatusCode.BadRequest, "ID do Profissional não fornecido")
            val request = call.receive<AtualizarValorRequest>()

            profissionalService.atualizarValorConsulta(
                profissionalId = profissionalId,
                novoValor = request.novoValor,
                usuarioLogado = usuarioLogado
            )

            call.respond(HttpStatusCode.OK, "Valor da consulta atualizado")

        } catch (e: SecurityException) {
            call.respond(HttpStatusCode.Forbidden, mapOf("erro" to e.message))
        } catch (e: NoSuchElementException) {
            call.respond(HttpStatusCode.NotFound, mapOf("erro" to e.message))
        } catch (e: IllegalArgumentException) {
            call.respond(HttpStatusCode.BadRequest, mapOf("erro" to e.message))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("erro" to (e.message ?: "Erro interno")))
        }
    }

    suspend fun configurarAgenda(call: ApplicationCall) {
        try {
            val usuarioLogado = call.principal<User>() ?: return call.respond(HttpStatusCode.Unauthorized)
            val profissionalId = call.parameters["id"] ?: return call.respond(HttpStatusCode.BadRequest, "ID do Profissional não fornecido")
            val request = call.receive<ConfigurarAgendaRequest>()

            profissionalService.configurarHorarioPadraoEGerarAgendaInicial(
                profissionalId = profissionalId,
                novasRegras = request.novasRegras,
                usuarioLogado = usuarioLogado
            )

            call.respond(HttpStatusCode.OK, "Agenda configurada e horários gerados")

        } catch (e: SecurityException) {
            call.respond(HttpStatusCode.Forbidden, mapOf("erro" to e.message))
        } catch (e: NoSuchElementException) {
            call.respond(HttpStatusCode.NotFound, mapOf("erro" to e.message))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("erro" to (e.message ?: "Erro interno")))
        }
    }

    suspend fun listarPromocoesDisponiveis(call: ApplicationCall) {
        try {
            val usuarioLogado = call.principal<User>() ?: return call.respond(HttpStatusCode.Unauthorized)
            val profissionalId = call.parameters["id"] ?: return call.respond(HttpStatusCode.BadRequest, "ID do Profissional não fornecido")

            val promocoes = profissionalService.listarPromocoesDisponiveisParaAtivar(
                profissionalId = profissionalId,
                usuarioLogado = usuarioLogado
            )

            call.respond(HttpStatusCode.OK, promocoes)

        } catch (e: NoSuchElementException) {
            call.respond(HttpStatusCode.NotFound, mapOf("erro" to e.message))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("erro" to (e.message ?: "Erro interno")))
        }
    }

    suspend fun ativarPromocao(call: ApplicationCall) {
        try {
            val usuarioLogado = call.principal<User>() ?: return call.respond(HttpStatusCode.Unauthorized)
            val profissionalId = call.parameters["id"] ?: return call.respond(HttpStatusCode.BadRequest, "ID do Profissional não fornecido")
            val promocaoId = call.parameters["promocaoId"] ?: return call.respond(HttpStatusCode.BadRequest, "ID da Promoção não fornecido")

            profissionalService.ativarPromocao(
                profissionalId = profissionalId,
                promocaoId = promocaoId,
                usuarioLogado = usuarioLogado
            )

            call.respond(HttpStatusCode.OK, "Promoção ativada")

        } catch (e: NoSuchElementException) {
            call.respond(HttpStatusCode.NotFound, mapOf("erro" to e.message))
        } catch (e: IllegalArgumentException) {
            call.respond(HttpStatusCode.BadRequest, mapOf("erro" to e.message))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("erro" to (e.message ?: "Erro interno")))
        }
    }

    suspend fun desativarPromocao(call: ApplicationCall) {
        try {
            val usuarioLogado = call.principal<User>() ?: return call.respond(HttpStatusCode.Unauthorized)
            val profissionalId = call.parameters["id"] ?: return call.respond(HttpStatusCode.BadRequest, "ID do Profissional não fornecido")
            val promocaoId = call.parameters["promocaoId"] ?: return call.respond(HttpStatusCode.BadRequest, "ID da Promoção não fornecido")

            profissionalService.desativarPromocao(
                profissionalId = profissionalId,
                promocaoId = promocaoId,
                usuarioLogado = usuarioLogado
            )

            call.respond(HttpStatusCode.OK, "Promoção desativada")

        } catch (e: NoSuchElementException) {
            call.respond(HttpStatusCode.NotFound, mapOf("erro" to e.message))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("erro" to (e.message ?: "Erro interno")))
        }
    }

    suspend fun definirFolga(call: ApplicationCall) {
        try {
            val usuarioLogado = call.principal<User>() ?: return call.respond(HttpStatusCode.Unauthorized)
            val profissionalId = call.parameters["id"] ?: return call.respond(HttpStatusCode.BadRequest, "ID do Profissional não fornecido")
            val request = call.receive<DefinirFolgaRequest>()

            val profissional = profissionalRepository.buscarPorId(profissionalId)
                ?: return call.respond(HttpStatusCode.NotFound, "Profissional não encontrado")

            agendaService.definirDiaDeFolga(
                profissional = profissional,
                diaDeFolga = request.diaDeFolga,
                usuarioLogado = usuarioLogado
            )

            call.respond(HttpStatusCode.OK, "Dia de folga definido")

        } catch (e: SecurityException) {
            call.respond(HttpStatusCode.Forbidden, mapOf("erro" to e.message))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("erro" to (e.message ?: "Erro interno")))
        }
    }

    suspend fun listarHorariosDisponiveis(call: ApplicationCall) {
        try {
            // Esta rota pode ser pública ou autenticada, dependendo da sua regra de segurança
            val profissionalId = call.parameters["id"] ?: return call.respond(HttpStatusCode.BadRequest, "ID do Profissional não fornecido")
            val duracaoMinutos = call.request.queryParameters["duracao"]?.toIntOrNull() ?: 60

            val profissional = profissionalRepository.buscarPorId(profissionalId)
                ?: return call.respond(HttpStatusCode.NotFound, "Profissional não encontrado")

            val horarios = agendaService.listarHorariosDisponiveis(
                agenda = profissional.agenda,
                duracao = duracaoMinutos.minutes
            )

            // Converte LocalDateTime para String para serialização JSON
            val horariosString = horarios.map { it.toString() }
            call.respond(HttpStatusCode.OK, HorariosDisponiveisResponse(horarios = horariosString))

        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("erro" to (e.message ?: "Erro interno")))
        }
    }

    suspend fun obterStatusAgenda(call: ApplicationCall) {
        try {
            val usuarioLogado = call.principal<User>() ?: return call.respond(HttpStatusCode.Unauthorized)
            val profissionalId = call.parameters["id"] ?: return call.respond(HttpStatusCode.BadRequest, "ID do Profissional não fornecido")

            // Pega datas da query string, ex: /status?dataInicio=2025-10-27&dataFim=2025-11-02
            val dataInicioStr = call.request.queryParameters["dataInicio"] ?: return call.respond(HttpStatusCode.BadRequest, "Parâmetro 'dataInicio' (YYYY-MM-DD) é obrigatório")
            val dataFimStr = call.request.queryParameters["dataFim"] ?: return call.respond(HttpStatusCode.BadRequest, "Parâmetro 'dataFim' (YYYY-MM-DD) é obrigatório")

            val profissional = profissionalRepository.buscarPorId(profissionalId)
                ?: return call.respond(HttpStatusCode.NotFound, "Profissional não encontrado")

            val statusMap = agendaService.obterStatusDaAgenda(
                profissional = profissional,
                dataInicio = LocalDate.parse(dataInicioStr),
                dataFim = LocalDate.parse(dataFimStr)
            )

            val statusMapString = statusMap.mapKeys { it.key.toString() }
                .mapValues { it.value.name }

            call.respond(HttpStatusCode.OK, StatusAgendaResponse(statusSlots = statusMapString))

        } catch (e: IllegalArgumentException) {
            call.respond(HttpStatusCode.BadRequest, "Formato de data inválido. Use YYYY-MM-DD.")
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("erro" to (e.message ?: "Erro interno")))
        }
    }
}