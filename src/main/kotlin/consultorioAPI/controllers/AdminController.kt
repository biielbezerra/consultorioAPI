package consultorioAPI.controllers

import consultorioAPI.dtos.CriarConsultorioRequest
import consultorioAPI.dtos.CriarPromocaoRequest
import com.consultorioAPI.models.User
import com.consultorioAPI.services.ConsultorioService
import com.consultorioAPI.services.ManutencaoService
import com.consultorioAPI.services.PromocaoService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class AdminController(
    private val promocaoService: PromocaoService,
    private val consultorioService: ConsultorioService,
    private val manutencaoService: ManutencaoService
) {

    suspend fun criarPromocao(call: ApplicationCall) {
        try {
            val usuarioLogado = call.principal<User>() ?: return call.respond(HttpStatusCode.Unauthorized)
            val request = call.receive<CriarPromocaoRequest>()

            val novaPromocao = promocaoService.criarPromocao(
                descricao = request.descricao,
                percentualDesconto = request.percentualDesconto,
                dataInicio = request.dataInicio,
                dataFim = request.dataFim,
                tipoPromocao = request.tipoPromocao,
                codigoOpcional = request.codigoOpcional,
                profissionalIdAplicavel = request.profissionalIdAplicavel,
                isCumulativa = request.isCumulativa,
                usuarioLogado = usuarioLogado
            )

            call.respond(HttpStatusCode.Created, novaPromocao)

        } catch (e: SecurityException) {
            call.respond(HttpStatusCode.Forbidden, mapOf("erro" to e.message))
        } catch (e: IllegalArgumentException) {
            call.respond(HttpStatusCode.BadRequest, mapOf("erro" to e.message))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("erro" to (e.message ?: "Erro interno")))
        }
    }

    suspend fun criarConsultorio(call: ApplicationCall) {
        try {
            val usuarioLogado = call.principal<User>() ?: return call.respond(HttpStatusCode.Unauthorized)
            val request = call.receive<CriarConsultorioRequest>()

            val novoConsultorio = consultorioService.cadastroConsultorio(
                nome = request.nome,
                endereco = request.endereco,
                usuarioLogado = usuarioLogado
            )

            call.respond(HttpStatusCode.Created, novoConsultorio)

        } catch (e: SecurityException) {
            call.respond(HttpStatusCode.Forbidden, mapOf("erro" to e.message))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("erro" to (e.message ?: "Erro interno")))
        }
    }

    suspend fun executarManutencao(call: ApplicationCall) {
        try {
            manutencaoService.executarManutencaoDiaria()
            call.respond(HttpStatusCode.OK, mapOf("sucesso" to "Rotina de manutenção executada"))

        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("erro" to (e.message ?: "Erro interno")))
        }
    }
}