package com.consultorioAPI.controllers

import consultorioAPI.dtos.*
import com.consultorioAPI.exceptions.EmailBloqueadoException
import com.consultorioAPI.exceptions.InputInvalidoException
import com.consultorioAPI.exceptions.NaoAutorizadoException
import com.consultorioAPI.exceptions.PacienteInativoException
import com.consultorioAPI.exceptions.RecursoNaoEncontradoException
import com.consultorioAPI.models.User
import com.consultorioAPI.repositories.ConsultaRepository
import com.consultorioAPI.repositories.PacienteRepository
import com.consultorioAPI.repositories.ProfissionalRepository
import com.consultorioAPI.services.ConsultaService
import com.consultorioAPI.services.ConsultorioService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*

class ConsultaController(
    private val consultorioService: ConsultorioService,
    private val consultaService: ConsultaService,
    private val profissionalRepository: ProfissionalRepository,
    private val consultaRepository: ConsultaRepository,
    private val pacienteRepository: PacienteRepository,
) {

    suspend fun agendarConsulta(call: ApplicationCall) {
        val usuarioLogado = call.principal<User>()
            ?: throw NaoAutorizadoException("Usuário não autenticado.")
        val request = call.receive<AgendamentoRequest>()

        val profissional = profissionalRepository.buscarPorId(request.profissionalId)
            ?: throw RecursoNaoEncontradoException("Profissional não encontrado")

        val paciente = consultorioService.buscarOuPreCadastrarPaciente(
            email = request.pacienteEmail,
            nome = request.pacienteNome ?: "",
            usuarioLogado = usuarioLogado
        )

        val novaConsulta = consultorioService.agendarConsultaPaciente(
            paciente = paciente,
            profissional = profissional,
            dataHora = request.dataHora,
            usuarioLogado = usuarioLogado,
            codigoPromocional = request.codigoPromocional
        )
        call.respond(HttpStatusCode.Created, novaConsulta)
    }

    suspend fun agendarConsultaDupla(call: ApplicationCall) {
        val usuarioLogado = call.principal<User>()
            ?: throw NaoAutorizadoException("Usuário não autenticado.")
        val request = call.receive<AgendamentoDuploRequest>()

        val profissional = profissionalRepository.buscarPorId(request.profissionalId)
            ?: throw RecursoNaoEncontradoException("Profissional não encontrado")

        val paciente = consultorioService.buscarOuPreCadastrarPaciente(
            email = request.pacienteEmail,
            nome = request.pacienteNome ?: "",
            usuarioLogado = usuarioLogado
        )

        val novasConsultas = consultorioService.agendarPrimeiraConsultaDupla(
            paciente = paciente,
            profissional = profissional,
            dataHora1 = request.dataHora1,
            dataHora2 = request.dataHora2,
            usuarioLogado = usuarioLogado,
            codigoPromocional = request.codigoPromocional
        )
        call.respond(HttpStatusCode.Created, novasConsultas)
    }

    suspend fun agendarPacote(call: ApplicationCall) {
        val usuarioLogado = call.principal<User>()
            ?: throw NaoAutorizadoException("Usuário não autenticado.")
        val request = call.receive<AgendamentoPacoteRequest>()

        if (request.datas.isEmpty()) {
            throw InputInvalidoException("A lista de datas não pode ser vazia.")
        }

        val profissional = profissionalRepository.buscarPorId(request.profissionalId)
            ?: throw RecursoNaoEncontradoException("Profissional não encontrado")

        val paciente = consultorioService.buscarOuPreCadastrarPaciente(
            email = request.pacienteEmail,
            nome = request.pacienteNome ?: "",
            usuarioLogado = usuarioLogado
        )

        val novasConsultas = consultorioService.agendarConsultasEmPacote(
            paciente = paciente,
            profissional = profissional,
            datas = request.datas,
            usuarioLogado = usuarioLogado,
            codigoPromocional = request.codigoPromocional
        )
        call.respond(HttpStatusCode.Created, novasConsultas)
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
        call.respond(HttpStatusCode.OK, consultas)
    }

    suspend fun listarConsultasPaciente(call: ApplicationCall) {
        val usuarioLogado = call.principal<User>()
            ?: throw NaoAutorizadoException("Usuário não autenticado.")

        val pacienteIdAlvo = call.parameters["id"]
            ?: call.request.queryParameters["pacienteId"]
            ?: throw InputInvalidoException("ID do Paciente não fornecido")

        val consultas = consultaService.listarConsultasPaciente(pacienteIdAlvo, usuarioLogado)
        call.respond(HttpStatusCode.OK, consultas)
    }

}