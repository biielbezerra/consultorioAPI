package consultorioAPI.controllers

import consultorioAPI.dtos.*
import com.consultorioAPI.exceptions.*
import com.consultorioAPI.models.*
import com.consultorioAPI.repositories.*
import com.consultorioAPI.services.*
import consultorioAPI.mappers.*
import consultorioAPI.services.ConsultorioService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory

class ConsultaController(
    private val consultorioService: ConsultorioService,
    private val consultaService: ConsultaService,
    private val profissionalRepository: ProfissionalRepository,
    private val consultaRepository: ConsultaRepository,
    private val pacienteRepository: PacienteRepository,
    private val consultorioRepository: ConsultorioRepository,
    private val areaAtuacaoRepository: AreaAtuacaoRepository
) {

    suspend fun agendarConsulta(call: ApplicationCall) {
        val usuarioLogado = call.principal<User>()
            ?: throw NaoAutorizadoException("Usuário não autenticado.")
        val request = call.receive<AgendamentoRequest>()

        val profissional = profissionalRepository.buscarPorId(request.profissionalId)
            ?: throw RecursoNaoEncontradoException("Profissional não encontrado")

        val paciente: Paciente

        if (usuarioLogado.role == Role.PACIENTE) {
            if (!usuarioLogado.email.equals(request.pacienteEmail, ignoreCase = true)) {
                throw NaoAutorizadoException("Pacientes só podem agendar consultas para o seu próprio e-mail.")
            }
            paciente = pacienteRepository.buscarPorUserId(usuarioLogado.idUsuario)
                ?: throw RecursoNaoEncontradoException("Perfil de paciente não encontrado para o usuário logado.")

        } else {
            paciente = consultorioService.buscarOuPreCadastrarPaciente(
                email = request.pacienteEmail,
                nome = request.pacienteNome ?: "",
                usuarioLogado = usuarioLogado
            )
        }

        val novaConsultaResponse = consultorioService.agendarConsultaPaciente(
            paciente = paciente,
            profissional = profissional,
            dataHora = request.dataHora,
            consultorioId = request.consultorioId,
            usuarioLogado = usuarioLogado,
            codigoPromocional = request.codigoPromocional
        )

        call.respond(HttpStatusCode.Created, novaConsultaResponse)
    }

    suspend fun agendarConsultaDupla(call: ApplicationCall) {
        val usuarioLogado = call.principal<User>()
            ?: throw NaoAutorizadoException("Usuário não autenticado.")

        val request = call.receive<AgendamentoDuploRequest>()

        val profissional = profissionalRepository.buscarPorId(request.profissionalId)
            ?: throw RecursoNaoEncontradoException("Profissional não encontrado")

        val paciente: Paciente

        if (usuarioLogado.role == Role.PACIENTE) {
            if (!usuarioLogado.email.equals(request.pacienteEmail, ignoreCase = true)) {
                throw NaoAutorizadoException("Pacientes só podem agendar consultas para o seu próprio e-mail.")
            }
            paciente = pacienteRepository.buscarPorUserId(usuarioLogado.idUsuario)
                ?: throw RecursoNaoEncontradoException("Perfil de paciente não encontrado para o usuário logado.")

        } else {
            paciente = consultorioService.buscarOuPreCadastrarPaciente(
                email = request.pacienteEmail,
                nome = request.pacienteNome ?: "",
                usuarioLogado = usuarioLogado
            )
        }

        val novasConsultasResponse = consultorioService.agendarPrimeiraConsultaDupla(
            paciente = paciente,
            profissional = profissional,
            consultorioId = request.consultorioId,
            dataPrimeiraConsulta = request.dataPrimeiraConsulta,
            usuarioLogado = usuarioLogado,
            codigoPromocional = request.codigoPromocional
        )

        call.respond(HttpStatusCode.Created, novasConsultasResponse)
    }

    suspend fun agendarPacote(call: ApplicationCall) {
        val usuarioLogado = call.principal<User>()
            ?: throw NaoAutorizadoException("Usuário não autenticado.")
        val request = call.receive<AgendamentoPacoteRequest>()

        val profissional = profissionalRepository.buscarPorId(request.profissionalId)
            ?: throw RecursoNaoEncontradoException("Profissional não encontrado")

        val paciente: Paciente
        if (usuarioLogado.role == Role.PACIENTE) {
            if (!usuarioLogado.email.equals(request.pacienteEmail, ignoreCase = true)) {
                throw NaoAutorizadoException("Pacientes só podem agendar pacotes para o seu próprio e-mail.")
            }
            paciente = pacienteRepository.buscarPorUserId(usuarioLogado.idUsuario)
                ?: throw RecursoNaoEncontradoException("Perfil de paciente não encontrado para o usuário logado.")
        } else {
            paciente = consultorioService.buscarOuPreCadastrarPaciente(
                email = request.pacienteEmail,
                nome = request.pacienteNome ?: "",
                usuarioLogado = usuarioLogado
            )
        }

        val novasConsultasResponse = consultorioService.agendarConsultasEmPacote(
            paciente = paciente,
            profissional = profissional,
            consultorioId = request.consultorioId,
            dataPrimeiraConsulta = request.dataPrimeiraConsulta,
            promocaoIdDoPacote = request.promocaoIdDoPacote,
            usuarioLogado = usuarioLogado,
            codigoPromocional = request.codigoPromocional
        )

        call.respond(HttpStatusCode.Created, novasConsultasResponse)
    }

    suspend fun reagendarConsulta(call: ApplicationCall) {
        val usuarioLogado = call.principal<User>()
            ?: throw NaoAutorizadoException("Usuário não autenticado.")
        val consultaId = call.parameters["id"]
            ?: throw InputInvalidoException("ID da consulta não fornecido")
        val request = call.receive<ReagendamentoRequest>()

        val consulta = consultaRepository.buscarPorId(consultaId)
            ?: throw RecursoNaoEncontradoException("Consulta não encontrada")

        val profissional = profissionalRepository.buscarPorId(consulta.profissionalID!!)
            ?: throw RecursoNaoEncontradoException("Profissional da consulta não encontrado")

        val paciente = pacienteRepository.buscarPorId(consulta.pacienteID!!)
            ?: throw RecursoNaoEncontradoException("Paciente da consulta não encontrado")

        consultaService.reagendarConsulta(
            consulta = consulta,
            profissional = profissional,
            paciente = paciente,
            novaData = request.novaData,
            usuarioLogado = usuarioLogado
        )
        call.respond(HttpStatusCode.OK, mapOf("sucesso" to "Consulta reagendada"))
    }

    suspend fun cancelarConsulta(call: ApplicationCall) {
        val usuarioLogado = call.principal<User>()
            ?: throw NaoAutorizadoException("Usuário não autenticado.")
        val consultaId = call.parameters["id"]
            ?: throw InputInvalidoException("ID da consulta não fornecido")

        val consulta = consultaRepository.buscarPorId(consultaId)
            ?: throw RecursoNaoEncontradoException("Consulta não encontrada")

        val profissional = profissionalRepository.buscarPorId(consulta.profissionalID!!)
            ?: throw RecursoNaoEncontradoException("Profissional da consulta não encontrado")

        val paciente = pacienteRepository.buscarPorId(consulta.pacienteID!!)
            ?: throw RecursoNaoEncontradoException("Paciente da consulta não encontrado")

        consultaService.cancelarConsulta(
            consulta = consulta,
            paciente = paciente,
            profissional = profissional,
            usuarioLogado = usuarioLogado
        )
        call.respond(HttpStatusCode.OK, mapOf("sucesso" to "Consulta cancelada"))
    }

    suspend fun finalizarConsulta(call: ApplicationCall) {
        val usuarioLogado = call.principal<User>()
            ?: throw NaoAutorizadoException("Usuário não autenticado.")
        val consultaId = call.parameters["id"]
            ?: throw InputInvalidoException("ID da consulta não fornecido")
        val request = call.receive<FinalizarConsultaRequest>()

        val consulta = consultaRepository.buscarPorId(consultaId)
            ?: throw RecursoNaoEncontradoException("Consulta não encontrada")

        consultaService.finalizarConsulta(
            consulta = consulta,
            novoStatus = request.novoStatus,
            usuarioLogado = usuarioLogado
        )
        call.respond(HttpStatusCode.OK, mapOf("sucesso" to "Consulta finalizada com status: ${request.novoStatus}"))
    }

    suspend fun listarConsultasProfissional(call: ApplicationCall) {
        val usuarioLogado = call.principal<User>()
            ?: throw NaoAutorizadoException("Usuário não autenticado.")

        val profissionalIdAlvo = call.parameters["id"]
            ?: call.request.queryParameters["profissionalId"]
            ?: throw InputInvalidoException("ID do Profissional não fornecido")

        val consultas = consultaService.listarConsultasProfissional(profissionalIdAlvo, usuarioLogado)
        val response = coroutineScope {
            consultas.map {
                async { it.toResponse(consultorioRepository, areaAtuacaoRepository) }
            }.awaitAll()
        }
        call.respond(HttpStatusCode.OK, response)
    }

    suspend fun listarConsultasPaciente(call: ApplicationCall) {
        val usuarioLogado = call.principal<User>()
            ?: throw NaoAutorizadoException("Usuário não autenticado.")

        val pacienteIdAlvo = call.parameters["id"]
            ?: call.request.queryParameters["pacienteId"]
            ?: throw InputInvalidoException("ID do Paciente não fornecido")

        val consultas = consultaService.listarConsultasPaciente(pacienteIdAlvo, usuarioLogado)
        val response = coroutineScope {
            consultas.map {
                async { it.toResponse(consultorioRepository, areaAtuacaoRepository) }
            }.awaitAll()
        }
        call.respond(HttpStatusCode.OK, response)
    }

}