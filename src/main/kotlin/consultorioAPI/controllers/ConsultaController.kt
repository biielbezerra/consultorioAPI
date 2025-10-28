package com.consultorioAPI.controllers

import consultorioAPI.dtos.*
import com.consultorioAPI.exceptions.EmailBloqueadoException
import com.consultorioAPI.exceptions.PacienteInativoException
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
        try {
            val usuarioLogado = call.principal<User>() ?: return call.respond(HttpStatusCode.Unauthorized)
            val request = call.receive<AgendamentoRequest>()

            val profissional = profissionalRepository.buscarPorId(request.profissionalId)
                ?: return call.respond(HttpStatusCode.NotFound, "Profissional não encontrado")

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

        } catch (e: PacienteInativoException) {
            call.respond(HttpStatusCode.Conflict, mapOf("erro" to e.message, "pacienteId" to e.pacienteId))
        } catch (e: EmailBloqueadoException) {
            call.respond(HttpStatusCode.Forbidden, mapOf("erro" to e.message))
        } catch (e: SecurityException) {
            call.respond(HttpStatusCode.Forbidden, mapOf("erro" to e.message))
        } catch (e: IllegalArgumentException) {
            call.respond(HttpStatusCode.BadRequest, mapOf("erro" to e.message))
        } catch (e: IllegalStateException) {
            call.respond(HttpStatusCode.Conflict, mapOf("erro" to e.message))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("erro" to (e.message ?: "Erro interno")))
        }
    }

    suspend fun agendarConsultaDupla(call: ApplicationCall) {
        try {
            val usuarioLogado = call.principal<User>() ?: return call.respond(HttpStatusCode.Unauthorized)
            val request = call.receive<AgendamentoDuploRequest>()

            val profissional = profissionalRepository.buscarPorId(request.profissionalId)
                ?: return call.respond(HttpStatusCode.NotFound, "Profissional não encontrado")

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

        } catch (e: PacienteInativoException) {
            call.respond(HttpStatusCode.Conflict, mapOf("erro" to e.message, "pacienteId" to e.pacienteId))
        } catch (e: EmailBloqueadoException) {
            call.respond(HttpStatusCode.Forbidden, mapOf("erro" to e.message))
        } catch (e: SecurityException) {
            call.respond(HttpStatusCode.Forbidden, mapOf("erro" to e.message))
        } catch (e: IllegalArgumentException) {
            call.respond(HttpStatusCode.BadRequest, mapOf("erro" to e.message))
        } catch (e: IllegalStateException) {
            call.respond(HttpStatusCode.Conflict, mapOf("erro" to e.message))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("erro" to (e.message ?: "Erro interno")))
        }
    }

    suspend fun reagendarConsulta(call: ApplicationCall) {
        try {
            val usuarioLogado = call.principal<User>() ?: return call.respond(HttpStatusCode.Unauthorized)
            val consultaId = call.parameters["id"] ?: return call.respond(HttpStatusCode.BadRequest, "ID da consulta não fornecido")
            val request = call.receive<ReagendamentoRequest>()

            val consulta = consultaRepository.buscarPorId(consultaId)
                ?: return call.respond(HttpStatusCode.NotFound, "Consulta não encontrada")

            val profissional = profissionalRepository.buscarPorId(consulta.profissionalID!!)
                ?: return call.respond(HttpStatusCode.NotFound, "Profissional da consulta não encontrado")

            val paciente = pacienteRepository.buscarPorId(consulta.pacienteID!!)
                ?: return call.respond(HttpStatusCode.NotFound, "Paciente da consulta não encontrado")

            consultaService.reagendarConsulta(
                consulta = consulta,
                profissional = profissional,
                paciente = paciente,
                novaData = request.novaData,
                usuarioLogado = usuarioLogado
            )

            call.respond(HttpStatusCode.OK, "Consulta reagendada")

        } catch (e: SecurityException) {
            call.respond(HttpStatusCode.Forbidden, mapOf("erro" to e.message))
        } catch (e: IllegalStateException) {
            call.respond(HttpStatusCode.Conflict, mapOf("erro" to e.message))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("erro" to (e.message ?: "Erro interno")))
        }
    }

    suspend fun cancelarConsulta(call: ApplicationCall) {
        try {
            val usuarioLogado = call.principal<User>() ?: return call.respond(HttpStatusCode.Unauthorized)
            val consultaId = call.parameters["id"] ?: return call.respond(HttpStatusCode.BadRequest, "ID da consulta não fornecido")

            val consulta = consultaRepository.buscarPorId(consultaId)
                ?: return call.respond(HttpStatusCode.NotFound, "Consulta não encontrada")

            val profissional = profissionalRepository.buscarPorId(consulta.profissionalID!!)
                ?: return call.respond(HttpStatusCode.NotFound, "Profissional da consulta não encontrado")

            val paciente = pacienteRepository.buscarPorId(consulta.pacienteID!!)
                ?: return call.respond(HttpStatusCode.NotFound, "Paciente da consulta não encontrado")

            consultaService.cancelarConsulta(
                consulta = consulta,
                paciente = paciente,
                profissional = profissional,
                usuarioLogado = usuarioLogado
            )

            call.respond(HttpStatusCode.OK, "Consulta cancelada")

        } catch (e: SecurityException) {
            call.respond(HttpStatusCode.Forbidden, mapOf("erro" to e.message))
        } catch (e: IllegalStateException) {
            call.respond(HttpStatusCode.Conflict, mapOf("erro" to e.message))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("erro" to (e.message ?: "Erro interno")))
        }
    }

    suspend fun finalizarConsulta(call: ApplicationCall) {
        try {
            val usuarioLogado = call.principal<User>() ?: return call.respond(HttpStatusCode.Unauthorized)
            val consultaId = call.parameters["id"] ?: return call.respond(HttpStatusCode.BadRequest, "ID da consulta não fornecido")
            val request = call.receive<FinalizarConsultaRequest>()

            val consulta = consultaRepository.buscarPorId(consultaId)
                ?: return call.respond(HttpStatusCode.NotFound, "Consulta não encontrada")

            consultaService.finalizarConsulta(
                consulta = consulta,
                novoStatus = request.novoStatus,
                usuarioLogado = usuarioLogado
            )

            call.respond(HttpStatusCode.OK, "Consulta finalizada com status: ${request.novoStatus}")

        } catch (e: SecurityException) {
            call.respond(HttpStatusCode.Forbidden, mapOf("erro" to e.message))
        } catch (e: IllegalArgumentException) {
            call.respond(HttpStatusCode.BadRequest, mapOf("erro" to e.message))
        } catch (e: IllegalStateException) {
            call.respond(HttpStatusCode.Conflict, mapOf("erro" to e.message))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("erro" to (e.message ?: "Erro interno")))
        }
    }

    suspend fun listarConsultasProfissional(call: ApplicationCall) {
        try {
            val usuarioLogado = call.principal<User>() ?: return call.respond(HttpStatusCode.Unauthorized)

            val profissionalIdAlvo = call.parameters["id"]
                ?: call.request.queryParameters["profissionalId"]
                ?: return call.respond(HttpStatusCode.BadRequest, "ID do Profissional não fornecido")

            val consultas = consultaService.listarConsultasProfissional(
                profissionalIdAlvo = profissionalIdAlvo,
                usuarioLogado = usuarioLogado
            )

            call.respond(HttpStatusCode.OK, consultas)

        } catch (e: SecurityException) {
            call.respond(HttpStatusCode.Forbidden, mapOf("erro" to e.message))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("erro" to (e.message ?: "Erro interno")))
        }
    }

    suspend fun listarConsultasPaciente(call: ApplicationCall) {
        try {
            val usuarioLogado = call.principal<User>() ?: return call.respond(HttpStatusCode.Unauthorized)

            val pacienteIdAlvo = call.parameters["id"]
                ?: call.request.queryParameters["pacienteId"]
                ?: return call.respond(HttpStatusCode.BadRequest, "ID do Paciente não fornecido")

            val consultas = consultaService.listarConsultasPaciente(
                pacienteIdAlvo = pacienteIdAlvo,
                usuarioLogado = usuarioLogado
            )

            call.respond(HttpStatusCode.OK, consultas)

        } catch (e: SecurityException) {
            call.respond(HttpStatusCode.Forbidden, mapOf("erro" to e.message))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, mapOf("erro" to (e.message ?: "Erro interno")))
        }
    }

}