package com.consultorioAPI.services

import com.consultorioAPI.config.fusoHorarioPadrao
import com.consultorioAPI.models.Agenda
import com.consultorioAPI.models.Paciente
import com.consultorioAPI.models.Profissional
import com.consultorioAPI.models.Recepcionista
import com.consultorioAPI.models.Role
import com.consultorioAPI.models.StatusUsuario
import com.consultorioAPI.models.User
import com.consultorioAPI.repositories.AreaAtuacaoRepository
import com.consultorioAPI.repositories.EmailBlocklistRepository
import com.consultorioAPI.repositories.PacienteRepository
import com.consultorioAPI.repositories.ProfissionalRepository
import com.consultorioAPI.repositories.RecepcionistaRepository
import com.consultorioAPI.repositories.UserRepository
import com.consultorioAPI.utils.HashingUtil
import kotlinx.datetime.*
import kotlin.time.Clock
import java.util.UUID
import kotlin.time.ExperimentalTime

class UsuarioService(private val userRepository: UserRepository,
                     private val pacienteRepository: PacienteRepository,
                     private val profissionalRepository: ProfissionalRepository,
                     private val recepcionistaRepository: RecepcionistaRepository,
                     private val agendaService: AgendaService,
                     private val areaAtuacaoRepository: AreaAtuacaoRepository,
                     private val emailBlocklistRepository: EmailBlocklistRepository
) {
    @OptIn(ExperimentalTime::class)
    suspend fun registrarPaciente(nome: String, email: String, senha: String): Paciente {
        if (emailBlocklistRepository.buscarPorEmail(email) != null) {
            throw IllegalArgumentException("Este email está bloqueado e não pode ser registrado.")
        }
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

        val token = UUID.randomUUID().toString()

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
                    atributosEspecificos = atributos ?: emptyMap(),
                    conviteToken = token
                )
                profissionalRepository.salvar(novoProfissional)
            }
            Role.RECEPCIONISTA -> {
                val novaRecepcionista = Recepcionista(
                    nomeRecepcionista = nome,
                    userId = usuarioSalvo.idUsuario,
                    conviteToken = token
                )
                recepcionistaRepository.salvar(novaRecepcionista)
            }
            else -> {}
        }

        TODO("Local para disparar e-mail contendo o link com o token para o email do convidado")

        return usuarioSalvo
    }

    suspend fun completarCadastro(token: String, senhaNova: String): User {
        val profissional = profissionalRepository.buscarPorToken(token)
        val recepcionista = recepcionistaRepository.buscarPorToken(token)

        val perfilUserId: String
        val perfil: Any

        when {
            profissional != null -> {
                if (profissional.status != StatusUsuario.CONVIDADO) {
                    throw IllegalStateException("Este convite não está mais pendente (Profissional).")
                }
                perfilUserId = profissional.userId
                perfil = profissional
            }
            recepcionista != null -> {
                if (recepcionista.status != StatusUsuario.CONVIDADO) {
                    throw IllegalStateException("Este convite não está mais pendente (Recepcionista).")
                }
                perfilUserId = recepcionista.userId
                perfil = recepcionista
            }
            else -> {
                throw IllegalArgumentException("Convite inválido ou expirado.")
            }
        }

        val userEncontrado = userRepository.buscarPorId(perfilUserId)
            ?: throw IllegalStateException("Usuário associado ao perfil não encontrado.")

        userEncontrado.senhaHash = HashingUtil.hashSenha(senhaNova)
        val usuarioAtualizado = userRepository.atualizar(userEncontrado)

        when (perfil) {
            is Profissional -> {
                perfil.status = StatusUsuario.ATIVO
                perfil.conviteToken = null
                profissionalRepository.atualizar(perfil)
            }
            is Recepcionista -> {
                perfil.status = StatusUsuario.ATIVO
                perfil.conviteToken = null
                recepcionistaRepository.atualizar(perfil)
            }
        }

        return usuarioAtualizado
    }

    @OptIn(ExperimentalTime::class)
    suspend fun preCadastrarPacientePeloStaff(
        nome: String,
        email: String,
        usuarioLogado: User
    ): Paciente {
        if (usuarioLogado.role == Role.PACIENTE) {
            throw SecurityException("Pacientes não podem pré-cadastrar outros pacientes.")
        }

        val senhaTemporaria = HashingUtil.hashSenha(UUID.randomUUID().toString())
        val newUser = User(
            email = email,
            senhaHash = senhaTemporaria,
            role = Role.PACIENTE
        )
        val usuarioSalvo = userRepository.salvar(newUser)

        val newPaciente = Paciente(
            nomePaciente = nome,
            userId = usuarioSalvo.idUsuario,
            status = StatusUsuario.ATIVO
        )
        newPaciente.dataCadastro = Clock.System.now().toLocalDateTime(fusoHorarioPadrao)

        val pacienteSalvo = pacienteRepository.salvar(newPaciente)

        TODO("Disparar e-mail para o paciente (email) com link para definir a senha")

        return pacienteSalvo
    }

    suspend fun recusarConvite(token: String) {
        val profissional = profissionalRepository.buscarPorToken(token)
        val recepcionista = recepcionistaRepository.buscarPorToken(token)

        when {
            profissional != null -> {
                if (profissional.status != StatusUsuario.CONVIDADO) {
                    throw IllegalStateException("Este convite não está mais pendente (Profissional).")
                }
                profissional.status = StatusUsuario.RECUSADO
                profissional.conviteToken = null
                profissionalRepository.atualizar(profissional)
            }
            recepcionista != null -> {
                if (recepcionista.status != StatusUsuario.CONVIDADO) {
                    throw IllegalStateException("Este convite não está mais pendente (Recepcionista).")
                }
                recepcionista.status = StatusUsuario.RECUSADO
                recepcionista.conviteToken = null
                recepcionistaRepository.atualizar(recepcionista)
            }
            else -> {
                throw IllegalArgumentException("Convite inválido ou expirado.")
            }
        }
    }

    suspend fun reenviarConvite(
        userIdAlvo: String,
        usuarioLogado: User
    ) {
        if (usuarioLogado.role != Role.SUPER_ADMIN) {
            throw SecurityException("Apenas Super Admins podem reenviar convites.")
        }

        val userAlvo = userRepository.buscarPorId(userIdAlvo)
            ?: throw IllegalArgumentException("Usuário alvo não encontrado.")

        if (userAlvo.role == Role.PACIENTE || userAlvo.role == Role.SUPER_ADMIN) {
            throw IllegalArgumentException("Esta função só pode reenviar convites para Profissionais ou Recepcionistas.")
        }

        val novoToken = UUID.randomUUID().toString()

        when (userAlvo.role) {
            Role.PROFISSIONAL -> {
                val perfil = profissionalRepository.buscarPorUserId(userAlvo.idUsuario)
                    ?: throw IllegalStateException("Perfil profissional não encontrado para este usuário.")

                if (perfil.status != StatusUsuario.CONVIDADO && perfil.status != StatusUsuario.RECUSADO) {
                    throw IllegalStateException("Só é possível reenviar convites para usuários com status CONVIDADO ou RECUSADO.")
                }

                perfil.conviteToken = novoToken
                perfil.status = StatusUsuario.CONVIDADO
                profissionalRepository.atualizar(perfil)
            }
            Role.RECEPCIONISTA -> {
                val perfil = recepcionistaRepository.buscarPorUserId(userAlvo.idUsuario)
                    ?: throw IllegalStateException("Perfil de recepcionista não encontrado para este usuário.")

                if (perfil.status != StatusUsuario.CONVIDADO && perfil.status != StatusUsuario.RECUSADO) {
                    throw IllegalStateException("Só é possível reenviar convites para usuários com status CONVIDADO ou RECUSADO.")
                }

                perfil.conviteToken = novoToken
                perfil.status = StatusUsuario.CONVIDADO
                recepcionistaRepository.atualizar(perfil)
            }
            else -> {}
        }

        TODO("Local para reenviar e-mail contendo o link com o token para o email do convidado")
    }

    suspend fun deletarUsuario(
        userIdAlvo: String,
        usuarioLogado: User,
        bloquearEmail: Boolean = false
    ) {
        if (usuarioLogado.role != Role.SUPER_ADMIN) {
            throw SecurityException("Somente SuperAdmins podem deletar usuários")
        }

        val userAlvo = userRepository.buscarPorId(userIdAlvo)
            ?: throw IllegalArgumentException("Usuário alvo não encontrado.")

        val emailOriginal = userAlvo.email

        if (userAlvo.idUsuario == usuarioLogado.idUsuario && userAlvo.role == Role.SUPER_ADMIN) {
            throw IllegalArgumentException("Super Admins não podem se auto-deletar por esta função.")
        }

        when (userAlvo.role) {
            Role.PROFISSIONAL -> {
                val perfil = profissionalRepository.buscarPorUserId(userAlvo.idUsuario)
                if (perfil != null && !perfil.isDeletado) {
                    perfil.isDeletado = true
                    profissionalRepository.atualizar(perfil)
                }
            }
            Role.RECEPCIONISTA -> {
                val perfil = recepcionistaRepository.buscarPorUserId(userAlvo.idUsuario)
                if (perfil != null && !perfil.isDeletado) {
                    perfil.isDeletado = true
                    recepcionistaRepository.atualizar(perfil)
                }
            }
            Role.PACIENTE -> {
                val perfil = pacienteRepository.buscarPorUserId(userAlvo.idUsuario)
                if (perfil != null && !perfil.isDeletado) {
                    perfil.isDeletado = true
                    pacienteRepository.atualizar(perfil)
                }
            }
            Role.SUPER_ADMIN -> {}
        }

        if (!userAlvo.isDeletado) {
            userAlvo.email = "deleted_${userAlvo.idUsuario}@deleted.user"
            userAlvo.senhaHash = UUID.randomUUID().toString()
            userAlvo.isDeletado = true
            userRepository.atualizar(userAlvo)
        }

        if (userAlvo.role == Role.PACIENTE && bloquearEmail) {
            if (!emailOriginal.startsWith("deleted_")) {
                emailBlocklistRepository.salvar(emailOriginal)
            }
        }

        userRepository.deletarPorId(userAlvo.idUsuario)

        // TODO: Invalidar tokens de sessão/autenticação deste usuário, se houver.
    }

    suspend fun desbloquearEmailPaciente(
        emailAlvo: String,
        usuarioLogado: User
    ) {
        if (usuarioLogado.role != Role.SUPER_ADMIN) {
            throw SecurityException("Apenas Super Admins podem desbloquear emails.")
        }
        emailBlocklistRepository.deletarPorEmail(emailAlvo)
    }

    suspend fun listarEmailsBloqueados(usuarioLogado: User): List<String> {
        if (usuarioLogado.role != Role.SUPER_ADMIN) {
            throw SecurityException("Apenas Super Admins podem ver a lista de emails bloqueados.")
        }
        return emailBlocklistRepository.listarTodos()
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