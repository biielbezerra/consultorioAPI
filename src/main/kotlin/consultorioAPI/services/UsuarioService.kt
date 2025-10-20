package com.consultorioAPI.services

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
import java.time.LocalDateTime
import java.util.UUID

class UsuarioService(private val userRepository: UserRepository,
                     private val pacienteRepository: PacienteRepository,
                     private val profissionalRepository: ProfissionalRepository,
                     private val recepcionistaRepository: RecepcionistaRepository,
                     private val agendaService: AgendaService
) {
    fun registrarPaciente(nome: String, email: String, senha: String): Paciente {
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
        newPaciente.dataCadastro = LocalDateTime.now()
        return pacienteRepository.salvar(newPaciente)
    }

    fun preCadastrarEquipe(
        nome: String,
        email: String,
        role: Role,
        areaAtuacaoId: String?,
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
                if(areaAtuacaoId.isNullOrBlank()){
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

    fun completaCadastro(email: String, senhaNova: String): User {
        val user = userRepository.buscarPorEmail(email)
            ?: throw IllegalArgumentException("Usuário não encontrado.")

        user.senhaHash = HashingUtil.hashSenha(senhaNova)

        when(user.role){
            Role.PROFISSIONAL -> {
                val perfil = profissionalRepository.buscarPorUserId(user.idUsuario)!!
                perfil.status = StatusUsuario.ATIVO
                profissionalRepository.salvar(perfil)
            }
            Role.RECEPCIONISTA -> {
                val perfil = recepcionistaRepository.buscarPorUserId(user.idUsuario)!!
                perfil.status = StatusUsuario.ATIVO
                recepcionistaRepository.salvar(perfil)
            }
            else -> throw IllegalStateException("Este usuário não é um perfil de equipe.")
        }

        return userRepository.salvar(user)
    }

}