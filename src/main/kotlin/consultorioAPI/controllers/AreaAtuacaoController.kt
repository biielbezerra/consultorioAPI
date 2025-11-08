package com.consultorioAPI.controllers

import com.consultorioAPI.exceptions.*
import com.consultorioAPI.models.*
import com.consultorioAPI.repositories.AreaAtuacaoRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable
import java.util.UUID
import consultorioAPI.mappers.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

@Serializable
data class CriarAreaAtuacaoRequest(
    val nome: String,
    val nomeRegistro: String? = null
)

class AreaAtuacaoController(private val areaAtuacaoRepository: AreaAtuacaoRepository) {

    private fun checarPermissao(usuarioLogado: User) {
        if (!usuarioLogado.isSuperAdmin && usuarioLogado.role != Role.RECEPCIONISTA) {
            throw NaoAutorizadoException("Apenas Admins ou Recepcionistas podem gerenciar Áreas de Atuação.")
        }
    }

    suspend fun criarAreaAtuacao(call: ApplicationCall) {
        val usuarioLogado = call.principal<User>() ?: throw NaoAutorizadoException("Não autenticado.")
        checarPermissao(usuarioLogado)

        val request = call.receive<CriarAreaAtuacaoRequest>()
        if (request.nome.isBlank()) {
            throw InputInvalidoException("O nome da área não pode ser vazio.")
        }

        val novaArea = AreaAtuacao(
            idArea = UUID.randomUUID().toString(),
            nome = request.nome,
            nomeRegistro = request.nomeRegistro
        )
        val areaSalva = areaAtuacaoRepository.salvar(novaArea)
        call.respond(HttpStatusCode.Created, areaSalva.toResponse())
    }

    suspend fun listarAreasAtuacao(call: ApplicationCall) {
        val usuarioLogado = call.principal<User>() ?: throw NaoAutorizadoException("Não autenticado.")
        checarPermissao(usuarioLogado)

        val areas = areaAtuacaoRepository.listarTodas()
        val response = areas.map { it.toResponse() }
        call.respond(HttpStatusCode.OK, response)
    }
}