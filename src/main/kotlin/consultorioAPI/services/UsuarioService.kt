package com.consultorioAPI.services

import com.consultorioAPI.config.SupabaseConfig
import com.consultorioAPI.config.fusoHorarioPadrao
import com.consultorioAPI.exceptions.EmailBloqueadoException
import com.consultorioAPI.models.*
import com.consultorioAPI.repositories.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserRecord
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.admin.*
import io.github.jan.supabase.exceptions.BadRequestRestException
import kotlinx.datetime.toLocalDateTime
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class UsuarioService(private val userRepository: UserRepository,
                     private val pacienteRepository: PacienteRepository,
                     private val profissionalRepository: ProfissionalRepository,
                     private val recepcionistaRepository: RecepcionistaRepository,
                     private val agendaService: AgendaService,
                     private val areaAtuacaoRepository: AreaAtuacaoRepository,
                     private val emailBlocklistRepository: EmailBlocklistRepository
) {

    private val firebaseAuth = FirebaseAuth.getInstance()

    @Deprecated("Usar AuthService.registrarNovoPaciente que chama criarPerfilPacienteAposAuth")
    suspend fun registrarPacienteLegado(nome: String, email: String, senha: String): Paciente {
        throw NotImplementedError("Registro de paciente deve ser feito via AuthService.")
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
        var idUsuarioFirebase: String? = null

        try {
            val request = UserRecord.CreateRequest()
                .setEmail(email)
                .setEmailVerified(false)
                .setDisabled(false)

            val authUser = firebaseAuth.createUser(request)
            idUsuarioFirebase = authUser.uid

            // TODO: Enviar link de redefinição de senha do Firebase
            // val link = firebaseAuth.generatePasswordResetLink(email)
            // Enviar 'link' por email

        } catch (e: Exception) {
            throw IllegalArgumentException("Falha ao criar usuário no Firebase Auth: ${e.message}")
        }

        val newUserLocal = User(
            idUsuario = idUsuarioFirebase,
            email = email,
            role = role
        )
        val usuarioSalvoLocal = userRepository.salvar(newUserLocal)

        try {
            when(role){
                Role.PROFISSIONAL -> {
                    if (areaAtuacaoId.isBlank()) {
                        throw IllegalArgumentException("areaAtuacaoId é obrigatória para profissionais.")
                    }
                    areaAtuacaoRepository.buscarPorId(areaAtuacaoId)
                        ?: throw IllegalArgumentException("Area de Atuação não encontrada.")

                    val novoProfissional = Profissional(
                        nomeProfissional = nome,
                        userId = usuarioSalvoLocal.idUsuario,
                        areaAtuacaoId = areaAtuacaoId,
                        agenda = Agenda(mutableListOf(), mutableListOf()),
                        atributosEspecificos = atributos ?: emptyMap(),
                        conviteToken = token,
                        status = StatusUsuario.CONVIDADO
                    )
                    profissionalRepository.salvar(novoProfissional)
                }
                Role.RECEPCIONISTA -> {
                    val novaRecepcionista = Recepcionista(
                        nomeRecepcionista = nome,
                        userId = usuarioSalvoLocal.idUsuario,
                        conviteToken = token,
                        status = StatusUsuario.CONVIDADO
                    )
                    recepcionistaRepository.salvar(novaRecepcionista)
                }
                else -> {}
            }
        } catch (e: Exception) {
            userRepository.deletarPorId(usuarioSalvoLocal.idUsuario)
            try {
                firebaseAuth.deleteUser(usuarioSalvoLocal.idUsuario)
            } catch (authError: Exception) {
            }
            throw e
        }

        TODO("Local para disparar e-mail contendo o link com o token para o email do convidado")

        return usuarioSalvoLocal
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
            ?: throw IllegalStateException("Usuário associado ao perfil não encontrado no DB local.")

        try {
            val request = UserRecord.UpdateRequest(userEncontrado.idUsuario)
                .setPassword(senhaNova)
            firebaseAuth.updateUser(request)

        } catch (e: Exception) {
            throw Exception("Falha ao atualizar senha no Firebase Auth: ${e.message}")
        }

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

        return userEncontrado
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
        if (userRepository.buscarPorEmail(email) != null) {
            throw IllegalArgumentException("Este email já está em uso.")
        }

        var idUsuarioFirebase: String? = null

        try {
            val request = UserRecord.CreateRequest()
                .setEmail(email)
                .setEmailVerified(false)
                .setDisabled(false)

            val authUser = firebaseAuth.createUser(request)
            idUsuarioFirebase = authUser.uid

        } catch (e: Exception) {
            throw IllegalArgumentException("Falha ao criar usuário no Firebase Auth: ${e.message}")
        }

        val newUser = User(
            idUsuario = idUsuarioFirebase,
            email = email,
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

        val link = firebaseAuth.generatePasswordResetLink(email)

        TODO("Disparar e-mail para o paciente (email) com link para definir a senha acima")

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

        val userAlvo = userRepository.buscarPorId(userIdAlvo, incluirDeletados = true)
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
            userAlvo.isDeletado = true
            userRepository.atualizar(userAlvo)
        }

        if (userAlvo.role == Role.PACIENTE && bloquearEmail) {
            if (!emailOriginal.startsWith("deleted_")) {
                emailBlocklistRepository.salvar(emailOriginal)
            }
        }

        try {
            firebaseAuth.deleteUser(userIdAlvo)
        } catch (e: Exception) {
            println("AVISO: Falha ao deletar usuário $userIdAlvo do Firebase Auth: ${e.message}")
        }

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

    @OptIn(ExperimentalTime::class)
    suspend fun criarPerfilPacienteAposAuth(userId: String, nome: String, email: String): Paciente {
        if (userRepository.buscarPorId(userId) != null) {
            throw IllegalStateException("Usuário já existe no DB local.")
        }
        if (pacienteRepository.buscarPorUserId(userId) != null) {
            throw IllegalStateException("Perfil de paciente já existe para este usuário.")
        }
        if (emailBlocklistRepository.buscarPorEmail(email) != null) {
            throw EmailBloqueadoException("Este email está bloqueado.")
        }

        val newUser = User(
            idUsuario = userId,
            email = email,
            role = Role.PACIENTE
        )
        userRepository.salvar(newUser) //

        val newPaciente = Paciente(
            nomePaciente = nome,
            userId = userId,
            status = StatusUsuario.ATIVO
        )
        newPaciente.dataCadastro = Clock.System.now().toLocalDateTime(fusoHorarioPadrao) //
        return pacienteRepository.salvar(newPaciente) //
    }

}