package consultorioAPI.controllers

import com.consultorioAPI.exceptions.NaoAutorizadoException
import consultorioAPI.dtos.CriarConsultorioRequest
import consultorioAPI.dtos.CriarPromocaoRequest
import com.consultorioAPI.models.User
import com.consultorioAPI.services.ConsultorioService
import com.consultorioAPI.services.ManutencaoService
import com.consultorioAPI.services.ProfissionalService
import com.consultorioAPI.services.PromocaoService
import com.consultorioAPI.services.UsuarioService
import consultorioAPI.dtos.LinkarPerfilRequest
import consultorioAPI.dtos.TransferirAdminRequest
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
    private val manutencaoService: ManutencaoService,
    private val usuarioService: UsuarioService,
    private val profissionalService: ProfissionalService
) {

    suspend fun criarPromocao(call: ApplicationCall) {
        val usuarioLogado = call.principal<User>()
            ?: throw NaoAutorizadoException("Usuário não autenticado.")
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
            quantidadeMinima = request.quantidadeMinima,
            usuarioLogado = usuarioLogado
        )
        call.respond(HttpStatusCode.Created, novaPromocao)
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
        val perfilCriado = usuarioService.linkarPerfilAUsuario(dto, usuarioLogado)

        call.respond(HttpStatusCode.Created, perfilCriado)
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

        val profissionais = profissionalService.listarProfissionaisAtivos(usuarioLogado)
        call.respond(HttpStatusCode.OK, profissionais)
    }

}