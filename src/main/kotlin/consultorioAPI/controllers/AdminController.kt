package consultorioAPI.controllers

import com.consultorioAPI.exceptions.*
import com.consultorioAPI.models.Profissional
import com.consultorioAPI.models.Recepcionista
import consultorioAPI.dtos.*
import com.consultorioAPI.models.User
import com.consultorioAPI.repositories.*
import com.consultorioAPI.services.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import consultorioAPI.mappers.*
import consultorioAPI.services.ConsultorioService
import consultorioAPI.services.PromocaoService
import consultorioAPI.services.UsuarioService
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class AdminController(
    private val promocaoService: PromocaoService,
    private val consultorioService: ConsultorioService,
    private val manutencaoService: ManutencaoService,
    private val usuarioService: UsuarioService,
    private val profissionalService: ProfissionalService,
    private val userRepository: UserRepository,
    private val areaAtuacaoRepository: AreaAtuacaoRepository
) {

    suspend fun criarPromocao(call: ApplicationCall) {
        val usuarioLogado = call.principal<User>()
            ?: throw NaoAutorizadoException("Usuário não autenticado.")
        val request = call.receive<CriarPromocaoRequest>()

        val novaPromocaoResponse = promocaoService.criarPromocao(
            descricao = request.descricao,
            percentualDesconto = request.percentualDesconto,
            dataInicio = request.dataInicio,
            dataFim = request.dataFim,
            tipoPromocao = request.tipoPromocao,
            codigoOpcional = request.codigoOpcional,
            profissionalIdAplicavel = request.profissionalIdAplicavel,
            isCumulativa = request.isCumulativa,
            quantidadeMinima = request.quantidadeMinima,
            usuarioLogado = usuarioLogado
        )
        call.respond(HttpStatusCode.Created, novaPromocaoResponse)
    }

    suspend fun deletarPromocaoAdmin(call: ApplicationCall) {
        val usuarioLogado = call.principal<User>()
            ?: throw NaoAutorizadoException("Usuário não autenticado.")

        val promocaoId = call.parameters["id"]
            ?: throw InputInvalidoException("ID da Promoção não fornecido")

        promocaoService.deletarPromocao(promocaoId, usuarioLogado)
        call.respond(HttpStatusCode.OK, mapOf("sucesso" to "Promoção deletada com sucesso."))
    }

    suspend fun criarConsultorio(call: ApplicationCall) {
        val usuarioLogado = call.principal<User>()
            ?: throw NaoAutorizadoException("Usuário não autenticado.")
        val request = call.receive<CriarConsultorioRequest>()

        val novoConsultorio = consultorioService.cadastroConsultorio(
            nome = request.nome,
            endereco = request.endereco,
            usuarioLogado = usuarioLogado
        )

        call.respond(HttpStatusCode.Created, novoConsultorio)
    }

    suspend fun executarManutencao(call: ApplicationCall) {
        val usuarioLogado = call.principal<User>()
            ?: throw NaoAutorizadoException("Usuário não autenticado.")

        manutencaoService.executarManutencaoDiaria()
        call.respond(HttpStatusCode.OK, mapOf("sucesso" to "Rotina de manutenção executada"))
    }

    suspend fun linkarPerfil(call: ApplicationCall) {
        val usuarioLogado = call.principal<User>()
            ?: throw NaoAutorizadoException("Usuário não autenticado.")

        val dto = call.receive<LinkarPerfilRequest>()
        val perfilCriado = usuarioService.linkarPerfilAUsuario(
            dto = dto,
            usuarioLogado = usuarioLogado
        )

        when (perfilCriado) {
            is Profissional -> {
                val response = perfilCriado.toResponse(userRepository, areaAtuacaoRepository)
                call.respond(HttpStatusCode.Created, response)
            }
            is Recepcionista -> {
                val response = perfilCriado.toResponse(userRepository)
                call.respond(HttpStatusCode.Created, response)
            }
            else -> {
                call.respond(HttpStatusCode.InternalServerError, mapOf("erro" to "Falha ao mapear resposta do perfil linkado."))
            }
        }
    }

    suspend fun transferirSuperAdmin(call: ApplicationCall) {
        val usuarioLogado = call.principal<User>()
            ?: throw NaoAutorizadoException("Usuário não autenticado.")

        val dto = call.receive<TransferirAdminRequest>()
        val novoAdmin = usuarioService.transferirSuperAdmin(dto, usuarioLogado)

        call.respond(HttpStatusCode.OK, mapOf("sucesso" to "Propriedade transferida para ${novoAdmin.email}"))
    }

    suspend fun listarProfissionais(call: ApplicationCall) {
        val usuarioLogado = call.principal<User>()
            ?: throw NaoAutorizadoException("Usuário não autenticado.")

        val profissionaisResponse = profissionalService.listarProfissionaisAtivos(usuarioLogado)
        call.respond(HttpStatusCode.OK, profissionaisResponse)
    }

    suspend fun listarPacientes(call: ApplicationCall) {
        val usuarioLogado = call.principal<User>()
            ?: throw NaoAutorizadoException("Usuário não autenticado.")

        val pacientesResponse = usuarioService.listarPacientes(usuarioLogado)
        call.respond(HttpStatusCode.OK, pacientesResponse)
    }

    suspend fun listarTodasPromocoes(call: ApplicationCall) {
        val usuarioLogado = call.principal<User>()
            ?: throw NaoAutorizadoException("Usuário não autenticado.")

        val promocoesResponse = promocaoService.listarTodasPromocoes(usuarioLogado)
        call.respond(HttpStatusCode.OK, promocoesResponse)
    }

}