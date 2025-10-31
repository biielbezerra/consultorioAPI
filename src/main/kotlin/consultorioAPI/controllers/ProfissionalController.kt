package consultorioAPI.controllers

import com.consultorioAPI.exceptions.InputInvalidoException
import com.consultorioAPI.exceptions.NaoAutorizadoException
import com.consultorioAPI.exceptions.RecursoNaoEncontradoException
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
        val usuarioLogado = call.principal<User>()
            ?: throw NaoAutorizadoException("Usuário não autenticado.")
        val profissionalId = call.parameters["id"]
            ?: throw InputInvalidoException("ID do Profissional não fornecido")
        val request = call.receive<AtualizarValorRequest>()

        profissionalService.atualizarValorConsulta(profissionalId, request.novoValor, usuarioLogado)
        call.respond(HttpStatusCode.OK, mapOf("sucesso" to "Valor da consulta atualizado"))
    }

    suspend fun configurarAgenda(call: ApplicationCall) {
        val usuarioLogado = call.principal<User>()
            ?: throw NaoAutorizadoException("Usuário não autenticado.")
        val profissionalId = call.parameters["id"]
            ?: throw InputInvalidoException("ID do Profissional não fornecido")
        val request = call.receive<ConfigurarAgendaRequest>()

        profissionalService.configurarHorarioPadraoEGerarAgendaInicial(profissionalId, request.novasRegras, usuarioLogado)
        call.respond(HttpStatusCode.OK, mapOf("sucesso" to "Agenda configurada e horários gerados"))
    }

    suspend fun listarPromocoesDisponiveis(call: ApplicationCall) {
        val usuarioLogado = call.principal<User>()
            ?: throw NaoAutorizadoException("Usuário não autenticado.")
        val profissionalId = call.parameters["id"]
            ?: throw InputInvalidoException("ID do Profissional não fornecido")

        val promocoes = profissionalService.listarPromocoesDisponiveisParaAtivar(profissionalId, usuarioLogado)
        call.respond(HttpStatusCode.OK, promocoes)
    }

    suspend fun ativarPromocao(call: ApplicationCall) {
        val usuarioLogado = call.principal<User>()
            ?: throw NaoAutorizadoException("Usuário não autenticado.")
        val profissionalId = call.parameters["id"]
            ?: throw InputInvalidoException("ID do Profissional não fornecido")
        val promocaoId = call.parameters["promocaoId"]
            ?: throw InputInvalidoException("ID da Promoção não fornecido")

        profissionalService.ativarPromocao(profissionalId, promocaoId, usuarioLogado)
        call.respond(HttpStatusCode.OK, mapOf("sucesso" to "Promoção ativada"))
    }

    suspend fun desativarPromocao(call: ApplicationCall) {
        val usuarioLogado = call.principal<User>()
            ?: throw NaoAutorizadoException("Usuário não autenticado.")
        val profissionalId = call.parameters["id"]
            ?: throw InputInvalidoException("ID do Profissional não fornecido")
        val promocaoId = call.parameters["promocaoId"]
            ?: throw InputInvalidoException("ID da Promoção não fornecido")

        profissionalService.desativarPromocao(profissionalId, promocaoId, usuarioLogado)
        call.respond(HttpStatusCode.OK, mapOf("sucesso" to "Promoção desativada"))
    }

    suspend fun definirFolga(call: ApplicationCall) {
        val usuarioLogado = call.principal<User>()
            ?: throw NaoAutorizadoException("Usuário não autenticado.")
        val profissionalId = call.parameters["id"]
            ?: throw InputInvalidoException("ID do Profissional não fornecido")
        val request = call.receive<DefinirFolgaRequest>()

        val profissional = profissionalRepository.buscarPorId(profissionalId)
            ?: throw RecursoNaoEncontradoException("Profissional não encontrado")

        agendaService.definirDiaDeFolga(profissional, request.diaDeFolga, usuarioLogado)
        call.respond(HttpStatusCode.OK, mapOf("sucesso" to "Dia de folga definido"))
    }

    suspend fun listarHorariosPorLocal(call: ApplicationCall) {
        val profissionalId = call.parameters["id"]
            ?: throw InputInvalidoException("ID do Profissional não fornecido")
        val consultorioId = call.parameters["consultorioId"]
            ?: throw InputInvalidoException("ID do Consultório não fornecido")
        val duracaoMinutos = call.request.queryParameters["duracao"]?.toIntOrNull() ?: 60

        val profissional = profissionalRepository.buscarPorId(profissionalId)
            ?: throw RecursoNaoEncontradoException("Profissional não encontrado")

        val horarios = agendaService.listarHorariosDisponiveisPorLocal(
            profissional = profissional,
            consultorioId = consultorioId,
            duracao = duracaoMinutos.minutes
        )

        val horariosString = horarios.map { it.toString() }
        call.respond(HttpStatusCode.OK, HorariosDisponiveisResponse(horarios = horariosString))
    }

    suspend fun listarHorariosDisponiveis(call: ApplicationCall) {
        val profissionalId = call.parameters["id"]
            ?: throw InputInvalidoException("ID do Profissional não fornecido")
        val duracaoMinutos = call.request.queryParameters["duracao"]?.toIntOrNull() ?: 60

        val profissional = profissionalRepository.buscarPorId(profissionalId)
            ?: throw RecursoNaoEncontradoException("Profissional não encontrado")

        val horarios = agendaService.listarHorariosDisponiveis(profissional.agenda, duracaoMinutos.minutes)

        val horariosString = horarios.map { it.toString() }
        call.respond(HttpStatusCode.OK, HorariosDisponiveisResponse(horarios = horariosString))
    }

    suspend fun obterStatusAgenda(call: ApplicationCall) {
        val usuarioLogado = call.principal<User>()
            ?: throw NaoAutorizadoException("Usuário não autenticado.")
        val profissionalId = call.parameters["id"]
            ?: throw InputInvalidoException("ID do Profissional não fornecido")

        val dataInicioStr = call.request.queryParameters["dataInicio"]
            ?: throw InputInvalidoException("Parâmetro 'dataInicio' (YYYY-MM-DD) é obrigatório")
        val dataFimStr = call.request.queryParameters["dataFim"]
            ?: throw InputInvalidoException("Parâmetro 'dataFim' (YYYY-MM-DD) é obrigatório")

        val profissional = profissionalRepository.buscarPorId(profissionalId)
            ?: throw RecursoNaoEncontradoException("Profissional não encontrado")

        val statusMap = agendaService.obterStatusDaAgenda(
            profissional = profissional,
            dataInicio = LocalDate.parse(dataInicioStr),
            dataFim = LocalDate.parse(dataFimStr)
        )

        val statusMapString = statusMap.mapKeys { it.key.toString() }
            .mapValues { it.value.name }

        call.respond(HttpStatusCode.OK, StatusAgendaResponse(statusSlots = statusMapString))
    }
}