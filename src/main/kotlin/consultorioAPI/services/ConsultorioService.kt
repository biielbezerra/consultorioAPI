package com.consultorioAPI.services

import com.consultorioAPI.config.fusoHorarioPadrao
import com.consultorioAPI.exceptions.EmailBloqueadoException
import com.consultorioAPI.exceptions.PacienteInativoException
import com.consultorioAPI.models.Consulta
import com.consultorioAPI.models.Consultorio
import com.consultorioAPI.models.Paciente
import com.consultorioAPI.models.Profissional
import com.consultorioAPI.models.Promocao
import com.consultorioAPI.models.Role
import com.consultorioAPI.models.StatusConsulta
import com.consultorioAPI.models.StatusUsuario
import com.consultorioAPI.models.User
import com.consultorioAPI.repositories.ConsultaRepository
import com.consultorioAPI.repositories.ConsultorioRepository
import com.consultorioAPI.repositories.EmailBlocklistRepository
import com.consultorioAPI.repositories.PacienteRepository
import com.consultorioAPI.repositories.ProfissionalRepository
import com.consultorioAPI.repositories.UserRepository
import com.consultorioAPI.services.UsuarioService
import kotlinx.datetime.*
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.math.min
import kotlin.time.ExperimentalTime

class ConsultorioService (private val consultorioRepository: ConsultorioRepository,
                          private val consultaRepository: ConsultaRepository,
                          private val pacienteService: PacienteService,
                          private val agendaService: AgendaService,
                          private val pacienteRepository: PacienteRepository,
                          private val profissionalRepository: ProfissionalRepository,
                          private val promocaoService: PromocaoService,
                          private val emailBlocklistRepository: EmailBlocklistRepository,
                          private val userRepository: UserRepository,
                          private val usuarioService: UsuarioService,
) {

    suspend fun cadastroConsultorio(nome: String, endereco: String, usuarioLogado: User): Consultorio {

        if(usuarioLogado.role != Role.SUPER_ADMIN){
            throw SecurityException("Apenas Super Admins podem cadastrar consultórios.")
        }

        val novoConsultorio = Consultorio(nomeConsultorio = nome, endereco = endereco)
        return consultorioRepository.salvar(novoConsultorio)
    }

    @OptIn(ExperimentalTime::class)
    suspend fun agendarPrimeiraConsultaDupla(
        paciente: Paciente,
        profissional: Profissional,
        dataHora1: LocalDateTime,
        dataHora2: LocalDateTime,
        usuarioLogado: User,
        codigoPromocional: String? = null,
        quantidade: Int = 2
    ): List<Consulta> {
        verificarLimiteAgendamentosFuturos(paciente.idPaciente, isAgendamentoDuplo = true)
        checarPermissaoAgendamento(usuarioLogado, paciente)

        if (usuarioLogado.role == Role.PROFISSIONAL) {
            val perfilLogado = profissionalRepository.buscarPorUserId(usuarioLogado.idUsuario)
                ?: throw SecurityException("Perfil profissional não encontrado.")
            if (perfilLogado.idProfissional != profissional.idProfissional) {
                throw SecurityException("Profissionais só podem agendar em suas próprias agendas.")
            }
        }

        val consulta1 = criarEValidarConsulta(paciente, profissional, dataHora1)
        val consulta2 = criarEValidarConsulta(paciente, profissional, dataHora2)

        val promocoesAplicadas = promocaoService.buscarMelhorPromocaoAplicavel(
            paciente = paciente,
            profissional = profissional,
            dataConsultaProposta = consulta1.dataHoraConsulta.toInstant(fusoHorarioPadrao),
            quantidadeConsultasSimultaneas = quantidade,
            codigoPromocionalInput = codigoPromocional
        )

        val descontoTotal = calcularDescontoTotal(promocoesAplicadas)
        val idsPromocoesAplicadas = promocoesAplicadas.map { it.idPromocao }

        consulta1.aplicarDesconto(descontoTotal)
        consulta1.promocoesAplicadasIds = idsPromocoesAplicadas
        registrarConsulta(paciente, profissional, consulta1)

        consulta2.aplicarDesconto(descontoTotal)
        consulta2.promocoesAplicadasIds = idsPromocoesAplicadas
        registrarConsulta(paciente, profissional, consulta2)

        return listOf(consulta1, consulta2)
    }

    @OptIn(ExperimentalTime::class)
    suspend fun agendarConsultaPaciente(
        paciente: Paciente,
        profissional: Profissional,
        dataHora: LocalDateTime,
        usuarioLogado: User,
        codigoPromocional: String? = null,
        quantidade: Int = 1
    ): Consulta {
        verificarLimiteAgendamentosFuturos(paciente.idPaciente, isAgendamentoDuplo = false)
        checarPermissaoAgendamento(usuarioLogado, paciente)

        if (usuarioLogado.role == Role.PROFISSIONAL) {
            val perfilLogado = profissionalRepository.buscarPorUserId(usuarioLogado.idUsuario)
                ?: throw SecurityException("Perfil profissional não encontrado.")
            if (perfilLogado.idProfissional != profissional.idProfissional) {
                throw SecurityException("Profissionais só podem agendar em suas próprias agendas.")
            }
        }

        val novaConsulta = criarEValidarConsulta(paciente, profissional, dataHora)

        val promocoesAplicadas = promocaoService.buscarMelhorPromocaoAplicavel(
            paciente = paciente,
            profissional = profissional,
            dataConsultaProposta = novaConsulta.dataHoraConsulta.toInstant(fusoHorarioPadrao),
            quantidadeConsultasSimultaneas = quantidade,
            codigoPromocionalInput = codigoPromocional
        )

        val descontoTotal = calcularDescontoTotal(promocoesAplicadas)
        novaConsulta.aplicarDesconto(descontoTotal)
        novaConsulta.promocoesAplicadasIds = promocoesAplicadas.map { it.idPromocao }

        registrarConsulta(paciente, profissional, novaConsulta)
        return novaConsulta
    }

    @OptIn(ExperimentalTime::class)
    suspend fun agendarConsultaProfissional(
        paciente: Paciente,
        profissional: Profissional,
        dataHora: LocalDateTime,
        usuarioLogado: User,
        codigoPromocional: String? = null,
        quantidade: Int = 1): Consulta
    {
        verificarLimiteAgendamentosFuturos(paciente.idPaciente, isAgendamentoDuplo = false)

        checarPermissaoAgendamento(usuarioLogado, paciente)

        if (usuarioLogado.role == Role.PROFISSIONAL) {
            val perfilLogado = profissionalRepository.buscarPorUserId(usuarioLogado.idUsuario)
                ?: throw SecurityException("Perfil profissional não encontrado.")
            if (perfilLogado.idProfissional != profissional.idProfissional) {
                throw SecurityException("Profissionais só podem agendar em suas próprias agendas.")
            }
        }

        val novaConsulta = criarEValidarConsulta(paciente, profissional, dataHora)

        val promocoesAplicadas = promocaoService.buscarMelhorPromocaoAplicavel(
            paciente = paciente,
            profissional = profissional,
            dataConsultaProposta = novaConsulta.dataHoraConsulta.toInstant(fusoHorarioPadrao),
            quantidadeConsultasSimultaneas = quantidade,
            codigoPromocionalInput = codigoPromocional
        )

        val descontoTotal = calcularDescontoTotal(promocoesAplicadas)
        novaConsulta.aplicarDesconto(descontoTotal)
        novaConsulta.promocoesAplicadasIds = promocoesAplicadas.map { it.idPromocao }

        registrarConsulta(paciente, profissional, novaConsulta)
        return novaConsulta
    }

    private fun calcularDescontoTotal(promocoesAplicadas: List<Promocao>): Double {
        val cumulativas = promocoesAplicadas.filter { it.isCumulativa }
        val naoCumulativas = promocoesAplicadas.filterNot { it.isCumulativa }

        val melhorNaoCumulativa = naoCumulativas.maxByOrNull { it.percentualDesconto }

        var descontoTotal = cumulativas.sumOf { it.percentualDesconto }
        if (melhorNaoCumulativa != null) {
            descontoTotal += melhorNaoCumulativa.percentualDesconto
        }

        return min(descontoTotal, 100.0)
    }

    private suspend fun criarEValidarConsulta(
        paciente: Paciente,
        profissional: Profissional,
        dataHora: LocalDateTime
    ): Consulta {
        val novaConsulta = Consulta(
            pacienteID = paciente.idPaciente,
            nomePaciente = paciente.nomePaciente,
            profissionalID = profissional.idProfissional,
            nomeProfissional = profissional.nomeProfissional,
            area = profissionalRepository.buscarPorId(profissional.idProfissional)?.areaAtuacaoId ?: "Desconhecida",
            dataHoraConsulta = dataHora,
            statusConsulta = StatusConsulta.AGENDADA,
            valorBase = profissional.valorBaseConsulta,
            valorConsulta = profissional.valorBaseConsulta
        )
        val duracaoDaConsulta = novaConsulta.duracaoEmMinutos.minutes

        if (!profissional.agenda.estaDisponivel(dataHora, duracaoDaConsulta)) {
            throw IllegalArgumentException("Horário do profissional indisponível")
        }
        if (!pacienteService.isPacienteDisponivel(paciente, dataHora, duracaoDaConsulta)) {
            throw IllegalArgumentException("Horário do paciente indisponível")
        }

        return novaConsulta
    }

    private suspend fun registrarConsulta(paciente: Paciente, profissional: Profissional, consulta: Consulta) {
        consultaRepository.salvar(consulta)
        profissional.agenda.bloquearHorario(consulta.dataHoraConsulta,consulta)
    }

    private suspend fun checarPermissaoAgendamento(
        usuarioLogado: User,
        pacienteDaConsulta: Paciente
    ) {
        when (usuarioLogado.role) {
            Role.SUPER_ADMIN, Role.RECEPCIONISTA -> return
            Role.PACIENTE -> {
                val perfilPacienteLogado = pacienteRepository.buscarPorUserId(usuarioLogado.idUsuario)
                    ?: throw SecurityException("Perfil de paciente não encontrado para este usuário.")
                if (perfilPacienteLogado.idPaciente != pacienteDaConsulta.idPaciente) {
                    throw SecurityException("Pacientes só podem agendar consultas para si mesmos.")
                }
                return
            }
            Role.PROFISSIONAL -> return
        }
    }

    suspend fun buscarOuPreCadastrarPaciente(
        email: String,
        nome: String,
        usuarioLogado: User
    ): Paciente {
        if (usuarioLogado.role == Role.PACIENTE) {
            throw SecurityException("Pacientes não podem usar esta função de busca/cadastro.")
        }
        if (emailBlocklistRepository.buscarPorEmail(email) != null) {
            throw EmailBloqueadoException("Este email está bloqueado e não pode ser usado para agendamento ou cadastro.")
        }

        val userExistente = userRepository.buscarPorEmail(email)

        if (userExistente != null) {
            if (userExistente.role != Role.PACIENTE) {
                throw IllegalArgumentException("Este email pertence a um membro da equipe, não a um paciente.")
            }
            // 5. Buscar o perfil Paciente associado
            val pacienteExistente = pacienteRepository.buscarPorUserId(userExistente.idUsuario)
                ?: throw IllegalStateException("Usuário encontrado, mas perfil de paciente associado não existe.")

            // 6. Verificar se o paciente existente está ATIVO
            if (pacienteExistente.status == StatusUsuario.INATIVO) {
                // Lança exceção específica com o ID do paciente
                throw PacienteInativoException(
                    "Este paciente existe mas está INATIVO. Reative-o para agendar.",
                    pacienteExistente.idPaciente
                )
                // O frontend pode capturar PacienteInativoException e mostrar o botão "Reativar?" (chamando atualizarStatusEquipe)
            } else if (pacienteExistente.status != StatusUsuario.ATIVO) {
                // Outros status não ativos (CONVIDADO, RECUSADO - não deveriam acontecer para Paciente, mas por segurança)
                throw IllegalStateException("Este paciente existe mas não está ativo no sistema (${pacienteExistente.status}).")
            }
            return pacienteExistente
        } else {
            if (nome.isBlank()) {
                throw IllegalArgumentException("Nome do paciente é obrigatório para pré-cadastro.")
            }
            return usuarioService.preCadastrarPacientePeloStaff(nome, email, usuarioLogado)
        }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun verificarLimiteAgendamentosFuturos(pacienteId: String, isAgendamentoDuplo: Boolean) {
        val agora = Clock.System.now()
        val consultasFuturasAgendadas = consultaRepository.buscarPorPacienteId(pacienteId)
            .filter {
                it.statusConsulta == StatusConsulta.AGENDADA &&
                        it.dataHoraConsulta.toInstant(fusoHorarioPadrao) > agora
            }

        if (isAgendamentoDuplo) {
            if (consultasFuturasAgendadas.isNotEmpty()) {
                throw IllegalStateException("Não é possível agendar consulta dupla inicial se já existem agendamentos futuros.")
            }
        } else {
            if (consultasFuturasAgendadas.isNotEmpty()) {
                throw IllegalStateException("Você já possui uma consulta futura agendada. Aguarde a realização ou cancele-a para agendar uma nova.")
            }
        }
    }

}