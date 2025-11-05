package com.consultorioAPI.services

import com.consultorioAPI.config.SupabaseConfig
import com.consultorioAPI.config.fusoHorarioPadrao
import com.consultorioAPI.exceptions.*
import com.consultorioAPI.models.*
import com.consultorioAPI.repositories.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserRecord
import consultorioAPI.dtos.AtualizarMeuPerfilRequest
import consultorioAPI.dtos.AtualizarMinhaSenhaRequest
import consultorioAPI.dtos.LinkarPerfilRequest
import consultorioAPI.dtos.RegistroPacienteRequest
import consultorioAPI.dtos.TransferirAdminRequest
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.admin.*
import io.github.jan.supabase.exceptions.BadRequestRestException
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Clock.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

class UsuarioService(private val userRepository: UserRepository,
                     private val pacienteRepository: PacienteRepository,
                     private val profissionalRepository: ProfissionalRepository,
                     private val recepcionistaRepository: RecepcionistaRepository,
                     private val agendaService: AgendaService,
                     private val areaAtuacaoRepository: AreaAtuacaoRepository,
                     private val emailBlocklistRepository: EmailBlocklistRepository,
                     private val consultaRepository: ConsultaRepository
) {

    private val firebaseAuth = FirebaseAuth.getInstance()

    suspend fun preCadastrarEquipe(
        nome: String,
        email: String,
        role: Role,
        areaAtuacaoId: String,
        atributos: Map<String, String>? = null,
        usuarioLogado: User
    ): User {
        if (!usuarioLogado.isSuperAdmin) {
            throw NaoAutorizadoException("Apenas Super Admins podem pré-cadastrar usuários.")
        }
        if (role == Role.PACIENTE || role == Role.SUPER_ADMIN) {
            throw InputInvalidoException("Esta função só pode cadastrar Profissionais ou Recepcionistas.")
        }
        if (userRepository.buscarPorEmail(email) != null) {
            throw ConflitoDeEstadoException("Este email já está em uso.")
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
            throw ConflitoDeEstadoException("Falha ao criar usuário no Firebase Auth: ${e.message}")
        }

        val newUserLocal = User(
            idUsuario = idUsuarioFirebase,
            email = email,
            role = role,
            status = StatusUsuario.CONVIDADO,
            isSuperAdmin = false,
            conviteToken = token
        )
        val usuarioSalvoLocal = userRepository.salvar(newUserLocal)

        try {
            when(role){
                Role.PROFISSIONAL -> {
                    if (areaAtuacaoId.isBlank()) {
                        throw InputInvalidoException("areaAtuacaoId é obrigatória para profissionais.")
                    }
                    areaAtuacaoRepository.buscarPorId(areaAtuacaoId)
                        ?: throw RecursoNaoEncontradoException("Area de Atuação não encontrada.")

                    val novoProfissional = Profissional(
                        nomeProfissional = nome,
                        userId = usuarioSalvoLocal.idUsuario,
                        areaAtuacaoId = areaAtuacaoId,
                        agenda = Agenda(mutableListOf(), mutableListOf()),
                        atributosEspecificos = atributos ?: emptyMap(),
                        status = StatusUsuario.CONVIDADO
                    )
                    profissionalRepository.salvar(novoProfissional)
                }
                Role.RECEPCIONISTA -> {
                    val novaRecepcionista = Recepcionista(
                        nomeRecepcionista = nome,
                        userId = usuarioSalvoLocal.idUsuario,
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

       //TODO"Local para disparar e-mail contendo o link com o token para o email do convidado"

        return usuarioSalvoLocal
    }

    suspend fun completarCadastro(token: String, senhaNova: String): User {
        val userEncontrado = userRepository.buscarPorToken(token)
            ?: throw RecursoNaoEncontradoException("Convite inválido ou expirado.")

        if (userEncontrado.status != StatusUsuario.CONVIDADO) {
            throw ConflitoDeEstadoException("Este convite não está mais pendente.")
        }

        validarForcaDaSenha(senhaNova)

        try {
            val request = UserRecord.UpdateRequest(userEncontrado.idUsuario)
                .setPassword(senhaNova)
            firebaseAuth.updateUser(request)
        } catch (e: Exception) {
            throw ConflitoDeEstadoException("Falha ao atualizar senha no Firebase Auth: ${e.message}")
        }

        userEncontrado.status = StatusUsuario.ATIVO
        userEncontrado.conviteToken = null
        userRepository.atualizar(userEncontrado)

        when (userEncontrado.role) {
            Role.PROFISSIONAL -> {
                val perfil = profissionalRepository.buscarPorUserId(userEncontrado.idUsuario)
                    ?: throw RecursoNaoEncontradoException("Perfil profissional não encontrado.")
                perfil.status = StatusUsuario.ATIVO
                profissionalRepository.atualizar(perfil)
            }
            Role.RECEPCIONISTA -> {
                val perfil = recepcionistaRepository.buscarPorUserId(userEncontrado.idUsuario)
                    ?: throw RecursoNaoEncontradoException("Perfil de recepcionista não encontrado.")
                perfil.status = StatusUsuario.ATIVO
                recepcionistaRepository.atualizar(perfil)
            }
            Role.SUPER_ADMIN -> {}
            else -> {}
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
            throw NaoAutorizadoException("Pacientes não podem pré-cadastrar outros pacientes.")
        }
        if (userRepository.buscarPorEmail(email) != null) {
            throw ConflitoDeEstadoException("Este email já está em uso.")
        }
        if (emailBlocklistRepository.buscarPorEmail(email) != null) {
            throw EmailBloqueadoException("Este email está bloqueado.")
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
            throw ConflitoDeEstadoException("Falha ao criar usuário no Firebase Auth: ${e.message}")
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
        newPaciente.dataCadastro = Clock.System.now()

        val pacienteSalvo = pacienteRepository.salvar(newPaciente)

        val link = firebaseAuth.generatePasswordResetLink(email)

        //TODO"Disparar e-mail para o paciente (email) com link para definir a senha acima"

        return pacienteSalvo
    }

    suspend fun recusarConvite(token: String) {
        val userAlvo = userRepository.buscarPorToken(token)
            ?: throw RecursoNaoEncontradoException("Convite inválido ou expirado.")

        if (userAlvo.status != StatusUsuario.CONVIDADO) {
            throw ConflitoDeEstadoException("Este convite não está mais pendente.")
        }

        userAlvo.status = StatusUsuario.RECUSADO
        userAlvo.conviteToken = null
        userRepository.atualizar(userAlvo)

        when (userAlvo.role) {
            Role.PROFISSIONAL -> {
                val perfil = profissionalRepository.buscarPorUserId(userAlvo.idUsuario)
                if (perfil != null) {
                    perfil.status = StatusUsuario.RECUSADO
                    profissionalRepository.atualizar(perfil)
                }
            }
            Role.RECEPCIONISTA -> {
                val perfil = recepcionistaRepository.buscarPorUserId(userAlvo.idUsuario)
                if (perfil != null) {
                    perfil.status = StatusUsuario.RECUSADO
                    recepcionistaRepository.atualizar(perfil)
                }
            }
            else -> {}
        }
    }

    @OptIn(ExperimentalTime::class)
    suspend fun registrarNovoPaciente(dto: RegistroPacienteRequest): Paciente {

        validarForcaDaSenha(dto.senha)

        if (emailBlocklistRepository.buscarPorEmail(dto.email) != null) {
            throw EmailBloqueadoException("Este email está bloqueado.")
        }
        if (userRepository.buscarPorEmail(dto.email) != null) {
            throw ConflitoDeEstadoException("Este email já está em uso.")
        }

        var idUsuarioFirebase: String? = null
        try {
            val request = UserRecord.CreateRequest()
                .setEmail(dto.email)
                .setEmailVerified(false)
                .setPassword(dto.senha)
                .setDisplayName(dto.nome)
                .setDisabled(false)

            val authUser = firebaseAuth.createUser(request)
            idUsuarioFirebase = authUser.uid

        } catch (e: Exception) {
            throw ConflitoDeEstadoException("Falha ao criar usuário no Firebase: ${e.message}")
        }

        try {
            val newUser = User(
                idUsuario = idUsuarioFirebase,
                email = dto.email,
                role = Role.PACIENTE,
                status = StatusUsuario.ATIVO
            )
            userRepository.salvar(newUser)

            val newPaciente = Paciente(
                nomePaciente = dto.nome,
                userId = idUsuarioFirebase,
                status = StatusUsuario.ATIVO
            )
            newPaciente.dataCadastro = Clock.System.now()

            return pacienteRepository.salvar(newPaciente)

        } catch (e: Exception) {
            println("ERRO DETALHADO AO SALVAR NO SUPABASE: ${e.message}")
            e.printStackTrace()
            try {
                if (idUsuarioFirebase != null) {
                    firebaseAuth.deleteUser(idUsuarioFirebase)
                    println("Rollback no Firebase realizado para usuário: $idUsuarioFirebase")
                }
            } catch (authError: Exception) {
                println("ERRO CRÍTICO DE ROLLBACK: Falha ao deletar usuário $idUsuarioFirebase do Firebase: ${authError.message}")
            }
            throw ConflitoDeEstadoException("Falha ao salvar perfil local, registro desfeito. Tente novamente.")
        }
    }

    suspend fun reenviarConvite(
        userIdAlvo: String,
        usuarioLogado: User
    ) {
        if (!usuarioLogado.isSuperAdmin) {
            throw NaoAutorizadoException("Apenas Super Admins podem reenviar convites.")
        }

        val userAlvo = userRepository.buscarPorId(userIdAlvo)
            ?: throw RecursoNaoEncontradoException("Usuário alvo não encontrado.")

        if (userAlvo.role == Role.PACIENTE || userAlvo.role == Role.SUPER_ADMIN) {
            throw InputInvalidoException("Esta função só pode reenviar convites para Profissionais ou Recepcionistas.")
        }

        val novoToken = UUID.randomUUID().toString()

        when (userAlvo.role) {
            Role.PROFISSIONAL -> {
                val perfil = profissionalRepository.buscarPorUserId(userAlvo.idUsuario)
                    ?: throw RecursoNaoEncontradoException("Perfil profissional não encontrado.")
                if (perfil.status != StatusUsuario.CONVIDADO && perfil.status != StatusUsuario.RECUSADO) {
                    throw ConflitoDeEstadoException("Só é possível reenviar convites para usuários com status CONVIDADO ou RECUSADO.")
                }
                perfil.status = StatusUsuario.CONVIDADO
                profissionalRepository.atualizar(perfil)
            }
            Role.RECEPCIONISTA -> {
                val perfil = recepcionistaRepository.buscarPorUserId(userAlvo.idUsuario)
                    ?: throw RecursoNaoEncontradoException("Perfil de recepcionista não encontrado.")
                if (perfil.status != StatusUsuario.CONVIDADO && perfil.status != StatusUsuario.RECUSADO) {
                    throw ConflitoDeEstadoException("Só é possível reenviar convites para usuários com status CONVIDADO ou RECUSADO.")
                }
                perfil.status = StatusUsuario.CONVIDADO
                recepcionistaRepository.atualizar(perfil)
            }
            else -> {}
        }

        userAlvo.conviteToken = novoToken
        userAlvo.status = StatusUsuario.CONVIDADO
        userRepository.atualizar(userAlvo)

        TODO("Local para reenviar e-mail...")

    }

    @OptIn(ExperimentalTime::class)
    suspend fun deletarUsuario(
        userIdAlvo: String,
        usuarioLogado: User,
        bloquearEmail: Boolean = false
    ) {
        if (!usuarioLogado.isSuperAdmin) {
            throw NaoAutorizadoException("Somente SuperAdmins podem deletar usuários")
        }

        val userAlvo = userRepository.buscarPorId(userIdAlvo, incluirDeletados = true)
            ?: throw RecursoNaoEncontradoException("Usuário alvo não encontrado.")

        val emailOriginal = userAlvo.email

        if (userAlvo.idUsuario == usuarioLogado.idUsuario && userAlvo.isSuperAdmin) {
            throw InputInvalidoException("Super Admins não podem se auto-deletar por esta função.")
        }

        val agora = Clock.System.now()

        when (userAlvo.role) {
            Role.PROFISSIONAL -> {
                val perfil = profissionalRepository.buscarPorUserId(userAlvo.idUsuario)
                if (perfil != null) {

                    val consultasFuturas = consultaRepository.buscarPorProfissionalId(perfil.idProfissional)
                        .filter {
                            it.statusConsulta == StatusConsulta.AGENDADA &&
                                    it.dataHoraConsulta != null &&
                                    it.dataHoraConsulta!!.toInstant(fusoHorarioPadrao) > agora
                        }

                    for (consulta in consultasFuturas) {
                        consulta.statusConsulta = StatusConsulta.CANCELADA
                        consultaRepository.atualizar(consulta)
                        // TODO: Notificar PACIENTE (consulta.pacienteID) sobre o cancelamento
                    }
                    if (!perfil.isDeletado) {
                        perfil.isDeletado = true
                        perfil.status = StatusUsuario.INATIVO
                        profissionalRepository.atualizar(perfil)
                    }
                }
            }
            Role.RECEPCIONISTA -> {
                val perfil = recepcionistaRepository.buscarPorUserId(userAlvo.idUsuario)
                if (perfil != null && !perfil.isDeletado) {
                    perfil.isDeletado = true
                    perfil.status = StatusUsuario.INATIVO
                    recepcionistaRepository.atualizar(perfil)
                }
            }
            Role.PACIENTE -> {
                val perfil = pacienteRepository.buscarPorUserId(userAlvo.idUsuario)
                if (perfil != null) {
                    val consultasFuturas = consultaRepository.buscarPorPacienteId(perfil.idPaciente)
                        .filter {
                            it.statusConsulta == StatusConsulta.AGENDADA || it.statusConsulta == StatusConsulta.PENDENTE
                        }

                    for (consulta in consultasFuturas) {
                        consulta.statusConsulta = StatusConsulta.CANCELADA
                        consultaRepository.atualizar(consulta)

                        if (consulta.dataHoraConsulta != null && consulta.profissionalID != null) {
                            val profissional = profissionalRepository.buscarPorId(consulta.profissionalID!!)
                            if (profissional != null) {
                                profissional.agenda.liberarHorario(consulta.dataHoraConsulta!!, consulta.duracaoEmMinutos.minutes)
                                profissionalRepository.atualizar(profissional)
                                // TODO: Notificar PROFISSIONAL sobre o cancelamento
                            }
                        }
                    }

                    if (!perfil.isDeletado) {
                        perfil.isDeletado = true
                        perfil.status = StatusUsuario.INATIVO
                        pacienteRepository.atualizar(perfil)
                    }
                }
            }
            Role.SUPER_ADMIN -> {}
        }

        if (!userAlvo.isDeletado) {
            userAlvo.email = "deleted_${userAlvo.idUsuario}@deleted.user"
            userAlvo.isDeletado = true
            userAlvo.status = StatusUsuario.INATIVO
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

    @OptIn(ExperimentalTime::class)
    suspend fun deletarMinhaConta(usuarioLogado: User) {

        when (usuarioLogado.role) {
            Role.PACIENTE -> {}
            Role.PROFISSIONAL, Role.RECEPCIONISTA -> {
                throw NaoAutorizadoException("Membros da equipe não podem se auto-excluir. Contate um Super Admin.")
            }
            Role.SUPER_ADMIN -> {
                throw NaoAutorizadoException("Super Admins não podem ser excluídos por esta rota. Use a rota /admin/usuarios/{id}")
            }
        }

        val emailOriginal = usuarioLogado.email
        val userId = usuarioLogado.idUsuario

        val perfil = pacienteRepository.buscarPorUserId(userId)
            ?: throw RecursoNaoEncontradoException("Perfil de Paciente não encontrado para exclusão.")

        val agora = Clock.System.now()
        val consultasFuturas = consultaRepository.buscarPorPacienteId(perfil.idPaciente)
            .filter {
                it.statusConsulta == StatusConsulta.AGENDADA || it.statusConsulta == StatusConsulta.PENDENTE
            }

        for (consulta in consultasFuturas) {
            consulta.statusConsulta = StatusConsulta.CANCELADA
            consultaRepository.atualizar(consulta)

            if (consulta.dataHoraConsulta != null && consulta.profissionalID != null) {
                val profissional = profissionalRepository.buscarPorId(consulta.profissionalID!!)
                if (profissional != null) {
                    profissional.agenda.liberarHorario(consulta.dataHoraConsulta!!, consulta.duracaoEmMinutos.minutes)
                    profissionalRepository.atualizar(profissional)
                    // TODO: Notificar profissional sobre o cancelamento
                }
            }
        }

        if (!usuarioLogado.isDeletado) {
            usuarioLogado.email = "deleted_${userId}@deleted.user"
            usuarioLogado.isDeletado = true
            usuarioLogado.status = StatusUsuario.INATIVO
            userRepository.atualizar(usuarioLogado)
        }

        try {
            firebaseAuth.deleteUser(userId)
        } catch (e: Exception) {
            println("AVISO: Falha ao deletar usuário $userId do Firebase Auth durante auto-exclusão: ${e.message}")
        }
    }

    suspend fun buscarMeuPerfil(usuarioLogado: User): Any {
        return when (usuarioLogado.role) {
            Role.PACIENTE -> pacienteRepository.buscarPorUserId(usuarioLogado.idUsuario)
                ?: throw RecursoNaoEncontradoException("Perfil de Paciente não encontrado para o usuário logado.")
            Role.PROFISSIONAL -> profissionalRepository.buscarPorUserId(usuarioLogado.idUsuario)
                ?: throw RecursoNaoEncontradoException("Perfil de Profissional não encontrado para o usuário logado.")
            Role.RECEPCIONISTA -> recepcionistaRepository.buscarPorUserId(usuarioLogado.idUsuario)
                ?: throw RecursoNaoEncontradoException("Perfil de Recepcionista não encontrado para o usuário logado.")
            Role.SUPER_ADMIN -> {
                val perfilProfissional = profissionalRepository.buscarPorUserId(usuarioLogado.idUsuario)
                if (perfilProfissional != null) return perfilProfissional

                usuarioLogado
            }
        }
    }

    suspend fun atualizarMeuPerfil(usuarioLogado: User, dto: AtualizarMeuPerfilRequest): Any {

        return when (usuarioLogado.role) {
            Role.PACIENTE -> {
                if (dto.nome != null) {
                    throw NaoAutorizadoException("Pacientes não podem alterar o nome.")
                }
                val perfil = pacienteRepository.buscarPorUserId(usuarioLogado.idUsuario)
                    ?: throw RecursoNaoEncontradoException("Perfil de Paciente não encontrado.")

                dto.telefone?.let { perfil.telefone = it }
                pacienteRepository.atualizar(perfil)
            }
            Role.PROFISSIONAL -> {
                val perfil = profissionalRepository.buscarPorUserId(usuarioLogado.idUsuario)
                    ?: throw RecursoNaoEncontradoException("Perfil de Profissional não encontrado.")

                dto.nome?.let {
                    if (it.isBlank()) throw InputInvalidoException("Nome não pode ser vazio.")
                    perfil.nomeProfissional = it
                }
                dto.telefone?.let { perfil.telefone = it }
                profissionalRepository.atualizar(perfil)
            }
            Role.RECEPCIONISTA -> {
                val perfil = recepcionistaRepository.buscarPorUserId(usuarioLogado.idUsuario)
                    ?: throw RecursoNaoEncontradoException("Perfil de Recepcionista não encontrado.")

                dto.nome?.let {
                    if (it.isBlank()) throw InputInvalidoException("Nome não pode ser vazio.")
                    perfil.nomeRecepcionista = it
                }
                dto.telefone?.let { perfil.telefone = it }
                recepcionistaRepository.atualizar(perfil)
            }
            Role.SUPER_ADMIN -> {
                if (profissionalRepository.buscarPorUserId(usuarioLogado.idUsuario) != null) {
                    val perfil = profissionalRepository.buscarPorUserId(usuarioLogado.idUsuario)!!
                    dto.nome?.let {
                        if (it.isBlank()) throw InputInvalidoException("Nome não pode ser vazio.")
                        perfil.nomeProfissional = it
                    }
                    dto.telefone?.let { perfil.telefone = it }
                    return profissionalRepository.atualizar(perfil)
                }

                throw InputInvalidoException("O perfil de Super Admin puro não pode ser atualizado por esta rota.")
            }
        }
    }

    suspend fun atualizarMinhaSenha(usuarioLogado: User, dto: AtualizarMinhaSenhaRequest) {
        validarForcaDaSenha(dto.novaSenha)

        try {
            val request = UserRecord.UpdateRequest(usuarioLogado.idUsuario)
                .setPassword(dto.novaSenha)
            firebaseAuth.updateUser(request)
        } catch (e: Exception) {
            e.printStackTrace()
            throw ConflitoDeEstadoException("Falha ao atualizar a senha no Firebase: ${e.message}")
        }
    }

    suspend fun linkarPerfilAUsuario(dto: LinkarPerfilRequest, usuarioLogado: User): Any {

        if (!usuarioLogado.isSuperAdmin) {
            throw NaoAutorizadoException("Apenas Super Admins podem linkar perfis.")
        }

        val userAlvo = userRepository.buscarPorId(dto.userId)
            ?: throw RecursoNaoEncontradoException("Usuário com ID ${dto.userId} não encontrado.")

        if (userAlvo.role != Role.SUPER_ADMIN) {
            when (userAlvo.role) {
                Role.PACIENTE -> throw ConflitoDeEstadoException("Este usuário já é um Paciente.")
                Role.PROFISSIONAL -> throw ConflitoDeEstadoException("Este usuário já é um Profissional.")
                Role.RECEPCIONISTA -> throw ConflitoDeEstadoException("Este usuário já é um Recepcionista.")
                else -> {}
            }
        }

        if (userAlvo.idUsuario == usuarioLogado.idUsuario) {
            userAlvo.role = dto.role
            userRepository.atualizar(userAlvo)
        }

        when (dto.role) {
            Role.PROFISSIONAL -> {
                val areaId = dto.areaAtuacaoId
                    ?: throw InputInvalidoException("areaAtuacaoId é obrigatória para linkar um Profissional.")

                areaAtuacaoRepository.buscarPorId(areaId)
                    ?: throw RecursoNaoEncontradoException("Area de Atuação não encontrada.")

                val novoProfissional = Profissional(
                    nomeProfissional = dto.nome,
                    userId = userAlvo.idUsuario,
                    areaAtuacaoId = areaId,
                    agenda = Agenda(mutableListOf(), mutableListOf()),
                    atributosEspecificos = dto.atributos ?: emptyMap(),
                    status = StatusUsuario.ATIVO
                )
                return profissionalRepository.salvar(novoProfissional)
            }
            Role.RECEPCIONISTA -> {
                val novaRecepcionista = Recepcionista(
                    nomeRecepcionista = dto.nome,
                    userId = userAlvo.idUsuario,
                    status = StatusUsuario.ATIVO
                )
                return recepcionistaRepository.salvar(novaRecepcionista)
            }
            else -> {
                throw InputInvalidoException("Só é possível linkar perfis do tipo PROFISSIONAL ou RECEPCIONISTA.")
            }
        }
    }

    suspend fun transferirSuperAdmin(dto: TransferirAdminRequest, adminAntigo: User): User {
        if (!adminAntigo.isSuperAdmin) {
            throw NaoAutorizadoException("Apenas um Super Admin pode transferir a propriedade.")
        }
        if (adminAntigo.email.equals(dto.novoEmail, ignoreCase = true)) {
            throw InputInvalidoException("Você não pode transferir a propriedade para si mesmo.")
        }

        val adminNovo = userRepository.buscarPorEmail(dto.novoEmail)
        val novoUser: User

        if (adminNovo != null) {
            if (adminNovo.isSuperAdmin) {
                throw ConflitoDeEstadoException("Este usuário já é um Super Admin.")
            }

            adminNovo.isSuperAdmin = true
            userRepository.atualizar(adminNovo)
            novoUser = adminNovo

            println("Propriedade de SuperAdmin transferida para o usuário existente: ${adminNovo.idUsuario}")

        } else {
            val token = UUID.randomUUID().toString()
            var idUsuarioFirebase: String? = null

            try {
                val request = UserRecord.CreateRequest()
                    .setEmail(dto.novoEmail)
                    .setEmailVerified(false)
                    .setDisabled(false)
                val authUser = firebaseAuth.createUser(request)
                idUsuarioFirebase = authUser.uid
            } catch (e: Exception) {
                throw ConflitoDeEstadoException("Falha ao criar usuário SuperAdmin no Firebase: ${e.message}")
            }

            novoUser = User(
                idUsuario = idUsuarioFirebase,
                email = dto.novoEmail,
                role = Role.SUPER_ADMIN,
                status = StatusUsuario.CONVIDADO,
                isSuperAdmin = true,
                conviteToken = token
            )
            userRepository.salvar(novoUser)

            println("Convite de SuperAdmin enviado para o novo usuário: ${novoUser.idUsuario}")
            //TODO"Disparar e-mail de convite para SuperAdmin (com link usando o 'token') para ${dto.novoEmail}"
        }

        processarContaAntiga(adminAntigo, dto.excluirContaAntiga)

        return novoUser
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun processarContaAntiga(adminAntigo: User, excluir: Boolean) {
        if (excluir) {
            // Se o admin antigo também era um Profissional, cancele suas consultas
            if (adminAntigo.role == Role.PROFISSIONAL) {
                val perfilProfissional = profissionalRepository.buscarPorUserId(adminAntigo.idUsuario)
                if (perfilProfissional != null) {

                    val agora = Clock.System.now()
                    val consultasFuturas = consultaRepository.buscarPorProfissionalId(perfilProfissional.idProfissional)
                        .filter {
                            it.statusConsulta == StatusConsulta.AGENDADA &&
                                    it.dataHoraConsulta != null &&
                                    it.dataHoraConsulta!!.toInstant(fusoHorarioPadrao) > agora
                        }

                    // Cancela consultas e avisa pacientes (aqui entra o EmailService)
                    for (consulta in consultasFuturas) {
                        consulta.statusConsulta = StatusConsulta.CANCELADA
                        consultaRepository.atualizar(consulta)
                        // TODO: Disparar e-mail para o paciente (consulta.pacienteID) avisando do cancelamento.
                        println("Consulta ${consulta.idConsulta} cancelada.")
                    }
                }
            }

            deletarUsuario(adminAntigo.idUsuario, adminAntigo, bloquearEmail = false)

        } else {
            adminAntigo.isSuperAdmin = false
            userRepository.atualizar(adminAntigo)
        }
    }

    suspend fun desbloquearEmailPaciente(
        emailAlvo: String,
        usuarioLogado: User
    ) {
        if (!usuarioLogado.isSuperAdmin) {
            throw NaoAutorizadoException("Apenas Super Admins podem desbloquear emails.")
        }
        emailBlocklistRepository.deletarPorEmail(emailAlvo)
    }

    suspend fun listarEmailsBloqueados(usuarioLogado: User): List<String> {
        if (!usuarioLogado.isSuperAdmin) {
            throw NaoAutorizadoException("Apenas Super Admins podem ver a lista de emails bloqueados.")
        }
        return emailBlocklistRepository.listarTodos()
    }

    suspend fun listarPacientes(usuarioLogado: User): List<Paciente> {
        if (!usuarioLogado.isSuperAdmin && usuarioLogado.role != Role.RECEPCIONISTA) {
            throw NaoAutorizadoException("Apenas Admins ou Recepcionistas podem listar todos os pacientes.")
        }
        return pacienteRepository.listarTodos()
    }

    @OptIn(ExperimentalTime::class)
    suspend fun atualizarStatusEquipe(
        userIdAlvo: String,
        novoStatus: StatusUsuario,
        usuarioLogado: User
    ) {
        if (!usuarioLogado.isSuperAdmin) {
            throw NaoAutorizadoException("Apenas Super Admins podem alterar o status de membros da equipe.")
        }

        val userAlvo = userRepository.buscarPorId(userIdAlvo)
            ?: throw RecursoNaoEncontradoException("Usuário alvo não encontrado.")

        if (userAlvo.role == Role.PACIENTE || userAlvo.role == Role.SUPER_ADMIN) {
            throw InputInvalidoException("Esta função só pode alterar o status de Profissionais ou Recepcionistas.")
        }
        if (novoStatus == StatusUsuario.CONVIDADO) {
            throw InputInvalidoException("Não é possível definir o status para CONVIDADO manualmente.")
        }

        if (userAlvo.status == novoStatus) {
            return
        }

        if (novoStatus == StatusUsuario.INATIVO && userAlvo.role == Role.PROFISSIONAL) {
            val perfil = profissionalRepository.buscarPorUserId(userAlvo.idUsuario)
                ?: throw RecursoNaoEncontradoException("Perfil profissional não encontrado para este usuário.")

            val agora = Clock.System.now()
            val consultasFuturas = consultaRepository.buscarPorProfissionalId(perfil.idProfissional)
                .filter {
                    it.statusConsulta == StatusConsulta.AGENDADA &&
                            it.dataHoraConsulta != null &&
                            it.dataHoraConsulta!!.toInstant(fusoHorarioPadrao) > agora
                }

            for (consulta in consultasFuturas) {
                consulta.statusConsulta = StatusConsulta.CANCELADA
                consultaRepository.atualizar(consulta)

                perfil.agenda.liberarHorario(consulta.dataHoraConsulta!!, consulta.duracaoEmMinutos.minutes)

                // TODO: Notificar PACIENTE (consulta.pacienteID) sobre o cancelamento
                println("Consulta ${consulta.idConsulta} cancelada devido à inativação do profissional.")
            }
            profissionalRepository.atualizar(perfil)
        }

        userAlvo.status = novoStatus
        userRepository.atualizar(userAlvo)

        when (userAlvo.role) {
            Role.PROFISSIONAL -> {
                val perfil = profissionalRepository.buscarPorUserId(userAlvo.idUsuario)
                    ?: throw RecursoNaoEncontradoException("Perfil profissional não encontrado para este usuário.")
                if (perfil.status != novoStatus) {
                    perfil.status = novoStatus
                    if (novoStatus != StatusUsuario.INATIVO) {
                        profissionalRepository.atualizar(perfil)
                    }
                }
            }
            Role.RECEPCIONISTA -> {
                val perfil = recepcionistaRepository.buscarPorUserId(userAlvo.idUsuario)
                    ?: throw RecursoNaoEncontradoException("Perfil de recepcionista não encontrado para este usuário.")
                if (perfil.status != novoStatus) {
                    perfil.status = novoStatus
                    recepcionistaRepository.atualizar(perfil)
                }
            }
            else -> {}
        }
    }

    private fun validarForcaDaSenha(senha: String) {
        if (senha.length < 6) {
            throw InputInvalidoException("A senha deve ter pelo menos 6 caracteres.")
        }
        if (!senha.matches(Regex(".*[0-9].*"))) {
            throw InputInvalidoException("A senha deve conter pelo menos um número.")
        }
        if (!senha.matches(Regex(".*[a-z].*"))) {
            throw InputInvalidoException("A senha deve conter pelo menos uma letra minúscula.")
        }
        if (!senha.matches(Regex(".*[A-Z].*"))) {
            throw InputInvalidoException("A senha deve conter pelo menos uma letra maiúscula.")
        }
        if (!senha.matches(Regex(".*[!@#\$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*"))) {
            throw InputInvalidoException("A senha deve conter pelo menos um caractere especial (ex: !@#$).")
        }
    }

}