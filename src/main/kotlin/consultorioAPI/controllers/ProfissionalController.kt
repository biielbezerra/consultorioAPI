package consultorioAPI.controllers

import com.consultorioAPI.exceptions.InputInvalidoException
import com.consultorioAPI.exceptions.NaoAutorizadoException
import com.consultorioAPI.exceptions.RecursoNaoEncontradoException
import com.consultorioAPI.models.Role
import consultorioAPI.dtos.*
import com.consultorioAPI.models.User
import com.consultorioAPI.repositories.ProfissionalRepository
import com.consultorioAPI.services.AgendaService
import com.consultorioAPI.services.ProfissionalService
import com.consultorioAPI.services.PromocaoService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.datetime.LocalDate
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

class ProfissionalController(
    private val profissionalService: ProfissionalService,
    private val agendaService: AgendaService,
    private val profissionalRepository: ProfissionalRepository,
    private val promocaoService: PromocaoService,
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

    suspend fun definirHorarioExtra(call: ApplicationCall) {
        val usuarioLogado = call.principal<User>()
            ?: throw NaoAutorizadoException("Usuário não autenticado.")

        val profissionalId = call.parameters["id"]
            ?: throw InputInvalidoException("ID do Profissional não fornecido")

        val request = call.receive<IntervaloRequest>()

        if (request.dataHoraInicio >= request.dataHoraFim) {
            throw InputInvalidoException("O horário de início deve ser anterior ao horário de fim.")
        }

        val profissional = profissionalRepository.buscarPorId(profissionalId)
            ?: throw RecursoNaoEncontradoException("Profissional não encontrado")

        if (!usuarioLogado.isSuperAdmin && usuarioLogado.role != Role.RECEPCIONISTA) {
            if (usuarioLogado.role == Role.PROFISSIONAL) {
                val perfilLogado = profissionalRepository.buscarPorUserId(usuarioLogado.idUsuario)
                if (perfilLogado?.idProfissional != profissionalId) {
                    throw NaoAutorizadoException("Profissionais só podem definir horários extras em suas próprias agendas.")
                }
            } else {
                throw NaoAutorizadoException("Usuário não autorizado a realizar esta ação.")
            }
        }

        agendaService.definirHorarioDisponivel(
            agenda = profissional.agenda,
            inicio = request.dataHoraInicio,
            fim = request.dataHoraFim
        )

        profissionalRepository.atualizar(profissional)

        call.respond(HttpStatusCode.OK, mapOf("sucesso" to "Horário extra definido como disponível."))
    }

    suspend fun bloquearIntervalo(call: ApplicationCall) {
        val usuarioLogado = call.principal<User>()
            ?: throw NaoAutorizadoException("Usuário não autenticado.")

        val profissionalId = call.parameters["id"]
            ?: throw InputInvalidoException("ID do Profissional não fornecido")

        val request = call.receive<IntervaloRequest>()

        if (request.dataHoraInicio >= request.dataHoraFim) {
            throw InputInvalidoException("O horário de início deve ser anterior ao horário de fim.")
        }

        val profissional = profissionalRepository.buscarPorId(profissionalId)
            ?: throw RecursoNaoEncontradoException("Profissional não encontrado")

        if (!usuarioLogado.isSuperAdmin && usuarioLogado.role != Role.RECEPCIONISTA) {
            if (usuarioLogado.role == Role.PROFISSIONAL) {
                val perfilLogado = profissionalRepository.buscarPorUserId(usuarioLogado.idUsuario)
                if (perfilLogado?.idProfissional != profissionalId) {
                    throw NaoAutorizadoException("Profissionais só podem bloquear intervalos em suas próprias agendas.")
                }
            } else {
                throw NaoAutorizadoException("Usuário não autorizado a realizar esta ação.")
            }
        }

        // TODO: Verificar se há consultas agendadas nesse intervalo antes de bloquear?

        agendaService.removerHorariosIntervalo(
            profissionalId = profissional.idProfissional,
            agenda = profissional.agenda,
            inicio = request.dataHoraInicio,
            fim = request.dataHoraFim
        )

        profissionalRepository.atualizar(profissional)
        call.respond(HttpStatusCode.OK, mapOf("sucesso" to "Intervalo bloqueado com sucesso."))
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

    @OptIn(ExperimentalTime::class)
    suspend fun criarPromocaoPropria(call: ApplicationCall) {
        val usuarioLogado = call.principal<User>()
            ?: throw NaoAutorizadoException("Usuário não autenticado.")
        val profissionalId = call.parameters["id"]
            ?: throw InputInvalidoException("ID do Profissional não fornecido")

        val perfilLogado = profissionalRepository.buscarPorUserId(usuarioLogado.idUsuario)
        if (!usuarioLogado.isSuperAdmin && perfilLogado?.idProfissional != profissionalId) {
            throw NaoAutorizadoException("Profissionais só podem criar promoções para si mesmos.")
        }

        val request = call.receive<CriarPromocaoRequest>()

        val novaPromocao = promocaoService.criarPromocao(
            descricao = request.descricao,
            percentualDesconto = request.percentualDesconto,
            dataInicio = request.dataInicio,
            dataFim = request.dataFim,
            tipoPromocao = request.tipoPromocao,
            codigoOpcional = request.codigoOpcional,
            profissionalIdAplicavel = profissionalId,
            isCumulativa = request.isCumulativa,
            quantidadeMinima = request.quantidadeMinima,
            usuarioLogado = usuarioLogado
        )
        call.respond(HttpStatusCode.Created, novaPromocao)
    }

    suspend fun deletarPromocaoPropria(call: ApplicationCall) {
        val usuarioLogado = call.principal<User>()
            ?: throw NaoAutorizadoException("Usuário não autenticado.")

        val profissionalId = call.parameters["id"]
            ?: throw InputInvalidoException("ID do Profissional não fornecido")
        val promocaoId = call.parameters["promocaoId"]
            ?: throw InputInvalidoException("ID da Promoção não fornecido")

        val perfilLogado = profissionalRepository.buscarPorUserId(usuarioLogado.idUsuario)
        if (!usuarioLogado.isSuperAdmin && perfilLogado?.idProfissional != profissionalId) {
            throw NaoAutorizadoException("Profissionais só podem deletar promoções de sua própria conta.")
        }

        promocaoService.deletarPromocao(promocaoId, usuarioLogado)
        call.respond(HttpStatusCode.OK, mapOf("sucesso" to "Promoção deletada com sucesso."))
    }

}