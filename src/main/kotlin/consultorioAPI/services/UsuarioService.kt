package com.consultorioAPI.services

import com.consultorioAPI.config.fusoHorarioPadrao
import com.consultorioAPI.models.Agenda
import com.consultorioAPI.models.Paciente
import com.consultorioAPI.models.Profissional
import com.consultorioAPI.models.Recepcionista
import com.consultorioAPI.models.Role
import com.consultorioAPI.models.StatusUsuario
import com.consultorioAPI.models.User
import com.consultorioAPI.repositories.PacienteRepository
import com.consultorioAPI.repositories.ProfissionalRepository
import com.consultorioAPI.repositories.RecepcionistaRepository
import com.consultorioAPI.repositories.UserRepository
import com.consultorioAPI.utils.HashingUtil
import kotlinx.datetime.*
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import java.util.UUID
import kotlin.time.ExperimentalTime

class UsuarioService(private val userRepository: UserRepository,
                     private val pacienteRepository: PacienteRepository,
                     private val profissionalRepository: ProfissionalRepository,
                     private val recepcionistaRepository: RecepcionistaRepository,
                     private val agendaService: AgendaService
) {
    @OptIn(ExperimentalTime::class)
    suspend fun registrarPaciente(nome: String, email: String, senha: String): Paciente {
        if(userRepository.buscarPorEmail(email) != null){
            throw IllegalArgumentException("Email existente já em uso")
        }

        val senhaHash = HashingUtil.hashSenha(senha)
        val newUser = User(
            email = email,
            senhaHash = senhaHash,
            role = Role.PACIENTE
        )
        val usuarioSalvo = userRepository.salvar(newUser)

        val newPaciente = Paciente(
            nomePaciente = nome,
            userId = usuarioSalvo.idUsuario,
            status = StatusUsuario.ATIVO
        )
        newPaciente.dataCadastro = Clock.System.now().toLocalDateTime(fusoHorarioPadrao)
        return pacienteRepository.salvar(newPaciente)
    }

    suspend fun preCadastrarEquipe(
        nome: String,
        email: String,
        role: Role,
        areaAtuacaoId: String,
        atributos: Map<String, String>? = null,
        usuarioLogado: User
    ): User {
        if (usuarioLogado.role != Role.SUPER_ADMIN) {
            throw SecurityException("Apenas Super Admins podem pré-cadastrar usuários.")
        }
        if (role == Role.PACIENTE || role == Role.SUPER_ADMIN) {
            throw IllegalArgumentException("Esta função só pode cadastrar Profissionais ou Recepcionistas.")
        }
        if (userRepository.buscarPorEmail(email) != null) {
            throw IllegalArgumentException("Este email já está em uso.")
        }

        val senhaTemporaria = HashingUtil.hashSenha(UUID.randomUUID().toString())
        val newUser = User(email = email, senhaHash = senhaTemporaria, role = role)
        val usuarioSalvo = userRepository.salvar(newUser)

        when(role){
            Role.PROFISSIONAL -> {
                if(areaAtuacaoId.isBlank()){
                    throw IllegalArgumentException("Área de atuação é obrigatória para profissionais.")
                }
                val novaAgenda = Agenda(mutableListOf(), mutableListOf())
                val novoProfissional = Profissional(
                    nomeProfissional = nome,
                    userId = usuarioSalvo.idUsuario,
                    areaAtuacaoId = areaAtuacaoId,
                    agenda = novaAgenda,
                    atributosEspecificos = atributos ?: emptyMap()
                )
                profissionalRepository.salvar(novoProfissional)
            }
            Role.RECEPCIONISTA -> {
                val novaRecepcionista = Recepcionista(nomeRecepcionista = nome, userId = usuarioSalvo.idUsuario)
                recepcionistaRepository.salvar(novaRecepcionista)
            }
            else -> {}
        }
        return usuarioSalvo
    }

    suspend fun completaCadastro(email: String, senhaNova: String): User {
        val user = userRepository.buscarPorEmail(email)
            ?: throw IllegalArgumentException("Usuário não encontrado.")

        user.senhaHash = HashingUtil.hashSenha(senhaNova)

        when(user.role){
            Role.PROFISSIONAL -> {
                val perfil = profissionalRepository.buscarPorUserId(user.idUsuario)
                    ?: throw IllegalStateException("Perfil profissional não encontrado para este usuário.")
                if (perfil.status != StatusUsuario.CONVIDADO) {
                    throw IllegalStateException("Este convite não está mais pendente.")
                }
                perfil.status = StatusUsuario.ATIVO
                profissionalRepository.atualizar(perfil)
            }
            Role.RECEPCIONISTA -> {
                val perfil = recepcionistaRepository.buscarPorUserId(user.idUsuario)!!
                perfil.status = StatusUsuario.ATIVO
                recepcionistaRepository.atualizar(perfil)
            }
            else -> throw IllegalStateException("Este usuário não é um perfil de equipe.")
        }

        return userRepository.atualizar(user)
    }

    suspend fun atualizarStatusEquipe(
        userIdAlvo: String,
        novoStatus: StatusUsuario,
        usuarioLogado: User
    ) {
        if (usuarioLogado.role != Role.SUPER_ADMIN) {
            throw SecurityException("Apenas Super Admins podem alterar o status de membros da equipe.")
        }

        val userAlvo = userRepository.buscarPorId(userIdAlvo)
            ?: throw IllegalArgumentException("Usuário alvo não encontrado.")

        if (userAlvo.role == Role.PACIENTE || userAlvo.role == Role.SUPER_ADMIN) {
            throw IllegalArgumentException("Esta função só pode alterar o status de Profissionais ou Recepcionistas.")
        }
        if (novoStatus == StatusUsuario.CONVIDADO) {
            throw IllegalArgumentException("Não é possível definir o status para CONVIDADO manualmente.")
        }

        when (userAlvo.role) {
            Role.PROFISSIONAL -> {
                val perfil = profissionalRepository.buscarPorUserId(userAlvo.idUsuario)
                    ?: throw IllegalStateException("Perfil profissional não encontrado para este usuário.")
                if (perfil.status != novoStatus) {
                    perfil.status = novoStatus
                    profissionalRepository.atualizar(perfil)
                }
            }
            Role.RECEPCIONISTA -> {
                val perfil = recepcionistaRepository.buscarPorUserId(userAlvo.idUsuario)
                    ?: throw IllegalStateException("Perfil de recepcionista não encontrado para este usuário.")
                if (perfil.status != novoStatus) {
                    perfil.status = novoStatus
                    recepcionistaRepository.atualizar(perfil)
                }
            }
            else -> {}
        }
    }

}