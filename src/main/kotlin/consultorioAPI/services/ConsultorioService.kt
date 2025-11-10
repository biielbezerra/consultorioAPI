package consultorioAPI.services

import com.consultorioAPI.config.fusoHorarioPadrao
import com.consultorioAPI.exceptions.*
import com.consultorioAPI.models.*
import com.consultorioAPI.repositories.*
import com.consultorioAPI.services.AgendaService
import com.consultorioAPI.services.PacienteService
import consultorioAPI.dtos.*
import consultorioAPI.mappers.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.*
import kotlin.time.Clock
import kotlinx.datetime.LocalDateTime
import java.util.UUID
import kotlin.time.Duration.Companion.minutes
import kotlin.math.min
import kotlin.time.ExperimentalTime
import org.slf4j.LoggerFactory

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
                          private val areaAtuacaoRepository: AreaAtuacaoRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun cadastroConsultorio(nome: String, endereco: String, usuarioLogado: User): ConsultorioResponse {
        log.info("Admin ${usuarioLogado.idUsuario} cadastrando consultório: $nome")

        if(!usuarioLogado.isSuperAdmin){
            throw NaoAutorizadoException("Apenas Super Admins podem cadastrar consultórios.")
        }

        val novoConsultorio = Consultorio(nomeConsultorio = nome, endereco = endereco)
        val consultorioSalvo = consultorioRepository.salvar(novoConsultorio)

        return consultorioSalvo.toResponse()
    }

    @OptIn(ExperimentalTime::class)
    suspend fun agendarPrimeiraConsultaDupla(
        paciente: Paciente,
        profissional: Profissional,
        consultorioId: String,
        dataPrimeiraConsulta: LocalDateTime,
        usuarioLogado: User,
        codigoPromocional: String? = null,
        quantidade: Int = 2
    ): List<ConsultaResponse> {
        log.info("Iniciando agendamento de consulta dupla para Paciente ${paciente.idPaciente} com Prof ${profissional.idProfissional}")
        verificarLimiteAgendamentosFuturos(paciente.idPaciente, isAgendamentoDuplo = true)
        checarPermissaoAgendamento(usuarioLogado, paciente)

        if (usuarioLogado.role == Role.PROFISSIONAL) {
            val perfilLogado = profissionalRepository.buscarPorUserId(usuarioLogado.idUsuario)
                ?: throw RecursoNaoEncontradoException("Perfil profissional não encontrado.")
            if (perfilLogado.idProfissional != profissional.idProfissional) {
                throw NaoAutorizadoException("Profissionais só podem agendar em suas próprias agendas.")
            }
        }

        val consulta1 = criarEValidarConsulta(paciente, profissional, dataPrimeiraConsulta, consultorioId)
        consulta1.statusConsulta = StatusConsulta.AGENDADA

        val promocoesAplicadas = promocaoService.buscarMelhorPromocaoAplicavel(
            paciente = paciente,
            profissional = profissional,
            dataConsultaProposta = consulta1.dataHoraConsulta!!,
            quantidadeConsultasSimultaneas = quantidade,
            codigoPromocionalInput = codigoPromocional
        )

        val descontoTotal = calcularDescontoTotal(promocoesAplicadas)
        val idsPromocoesAplicadas = promocoesAplicadas.map { it.idPromocao }

        consulta1.aplicarDesconto(descontoTotal)
        consulta1.promocoesAplicadasIds = idsPromocoesAplicadas

        val consulta2 = Consulta(
            idConsulta = UUID.randomUUID().toString(),
            pacienteID = paciente.idPaciente,
            nomePaciente = paciente.nomePaciente,
            profissionalID = profissional.idProfissional,
            nomeProfissional = profissional.nomeProfissional,
            consultorioId = consultorioId,
            area = profissional.areaAtuacaoId,
            dataHoraConsulta = null,
            statusConsulta = StatusConsulta.PENDENTE,
            valorBase = profissional.valorBaseConsulta,
            valorConsulta = profissional.valorBaseConsulta * (1 - descontoTotal / 100),
            descontoPercentual = descontoTotal,
            duracaoEmMinutos = profissional.duracaoPadraoMinutos,
            promocoesAplicadasIds = idsPromocoesAplicadas
        )
        var consulta1Salva: Consulta? = null
        var consulta2Salva: Consulta? = null
        try {
            consulta1Salva = consultaRepository.salvar(consulta1)
            consulta2Salva = consultaRepository.salvar(consulta2)

            val dataHoraLocal1 = consulta1Salva.dataHoraConsulta!!.toLocalDateTime(fusoHorarioPadrao)
            profissional.agenda.bloquearHorario(dataHoraLocal1, consulta1Salva)

            profissionalRepository.atualizar(profissional)
            log.info("Consulta dupla ${consulta1Salva.idConsulta} e ${consulta2Salva.idConsulta} salvas. Agenda do Prof ${profissional.idProfissional} atualizada.")

            val consultasSalvas = listOf(consulta1Salva!!, consulta2Salva!!)

            return coroutineScope {
                consultasSalvas.map {
                    async { it.toResponse(consultorioRepository, areaAtuacaoRepository) }
                }.awaitAll()
            }

        } catch (e: Exception) {
            log.error("Falha ao registrar agendamento duplo para Paciente ${paciente.idPaciente}. Iniciando rollback.", e)
            if (consulta1Salva != null) {
                try {
                    consultaRepository.deletarPorId(consulta1Salva.idConsulta)
                    log.warn("ROLLBACK: Consulta ${consulta1Salva.idConsulta} deletada.")
                } catch (rbError: Exception) {
                    log.error("ERRO CRÍTICO DE ROLLBACK: Falha ao deletar consulta ${consulta1Salva.idConsulta}", rbError)
                }
            }
            if (consulta2Salva != null) {
                try {
                    consultaRepository.deletarPorId(consulta2Salva.idConsulta)
                    log.warn("ROLLBACK: Consulta ${consulta2Salva.idConsulta} deletada.")
                } catch (rbError: Exception) {
                    log.error("ERRO CRÍTICO DE ROLLBACK: Falha ao deletar consulta ${consulta2Salva.idConsulta}", rbError)
                }
            }
            throw ConflitoDeEstadoException("Falha ao registrar agendamento duplo: ${e.message}")
        }
    }

    @OptIn(ExperimentalTime::class)
    suspend fun agendarConsultasEmPacote(
        paciente: Paciente,
        profissional: Profissional,
        consultorioId: String,
        dataPrimeiraConsulta: LocalDateTime,
        promocaoIdDoPacote: String,
        usuarioLogado: User,
        codigoPromocional: String? = null
    ): List<ConsultaResponse> {
        log.info("Iniciando agendamento de pacote para Paciente ${paciente.idPaciente} com Prof ${profissional.idProfissional}")

        val promocaoPacote = promocaoService.buscarPromocoesPorIds(listOf(promocaoIdDoPacote)).firstOrNull()
            ?: throw RecursoNaoEncontradoException("Promoção (Pacote) não encontrada.")

        if (promocaoPacote.tipoPromocao != TipoPromocao.PACOTE || promocaoPacote.quantidadeMinimaConsultas == null) {
            throw InputInvalidoException("Esta promoção não é um pacote válido.")
        }

        val quantidadeTotal = promocaoPacote.quantidadeMinimaConsultas!!
        log.debug("Pacote detectado: $quantidadeTotal consultas.")

        checarPermissaoAgendamento(usuarioLogado, paciente)

        val primeiraConsulta = criarEValidarConsulta(paciente, profissional, dataPrimeiraConsulta, consultorioId)
        primeiraConsulta.statusConsulta = StatusConsulta.AGENDADA

        val promocoesAplicadas = promocaoService.buscarMelhorPromocaoAplicavel(
            paciente = paciente,
            profissional = profissional,
            dataConsultaProposta = primeiraConsulta.dataHoraConsulta!!,
            quantidadeConsultasSimultaneas = quantidadeTotal,
            codigoPromocionalInput = codigoPromocional
        )

        if (!promocoesAplicadas.any { it.idPromocao == promocaoIdDoPacote }) {
            throw ConflitoDeEstadoException("O pacote selecionado não é válido para este agendamento (ex: expirou, código incorreto, etc).")
        }

        val descontoTotal = calcularDescontoTotal(promocoesAplicadas)
        val idsPromocoesAplicadas = promocoesAplicadas.map { it.idPromocao }

        primeiraConsulta.aplicarDesconto(descontoTotal)
        primeiraConsulta.promocoesAplicadasIds = idsPromocoesAplicadas

        val consultasCredito = List(quantidadeTotal - 1) {
            Consulta(
                pacienteID = paciente.idPaciente,
                nomePaciente = paciente.nomePaciente,
                profissionalID = profissional.idProfissional,
                nomeProfissional = profissional.nomeProfissional,
                consultorioId = consultorioId,
                area = profissional.areaAtuacaoId,
                dataHoraConsulta = null,
                statusConsulta = StatusConsulta.PENDENTE,
                valorBase = profissional.valorBaseConsulta,
                valorConsulta = profissional.valorBaseConsulta * (1 - descontoTotal / 100),
                descontoPercentual = descontoTotal,
                duracaoEmMinutos = profissional.duracaoPadraoMinutos,
                promocoesAplicadasIds = idsPromocoesAplicadas
            )
        }
        log.debug("Criado ${consultasCredito.size} consultas 'crédito'.")

        var consultaAgendadaSalva: Consulta? = null
        val consultasCreditoSalvas = mutableListOf<Consulta>()
        try {
            consultaAgendadaSalva = consultaRepository.salvar(primeiraConsulta)
            log.info("Consulta agendada ${consultaAgendadaSalva.idConsulta} salva.")

            consultasCredito.forEach { credito ->
                consultasCreditoSalvas.add(consultaRepository.salvar(credito))
            }
            log.info("${consultasCreditoSalvas.size} consultas crédito salvas.")

            val dataHoraLocal = consultaAgendadaSalva.dataHoraConsulta!!.toLocalDateTime(fusoHorarioPadrao)
            profissional.agenda.bloquearHorario(dataHoraLocal, consultaAgendadaSalva)

            profissionalRepository.atualizar(profissional)
            log.info("Agenda do Prof ${profissional.idProfissional} atualizada.")

            val consultasSalvas = listOf(consultaAgendadaSalva!!) + consultasCreditoSalvas

            return coroutineScope {
                consultasSalvas.map {
                    async { it.toResponse(consultorioRepository, areaAtuacaoRepository) }
                }.awaitAll()
            }

        } catch (e: Exception) {
            log.error("Falha ao registrar pacote para Paciente ${paciente.idPaciente}. Iniciando rollback.", e)
            if (consultaAgendadaSalva != null) {
                try {
                    consultaRepository.deletarPorId(consultaAgendadaSalva.idConsulta)
                    log.warn("ROLLBACK: Consulta ${consultaAgendadaSalva.idConsulta} deletada.")
                } catch (rbError: Exception) {
                    log.error("ERRO CRÍTICO DE ROLLBACK: Falha ao deletar consulta ${consultaAgendadaSalva.idConsulta}", rbError)
                }
            }
            consultasCreditoSalvas.forEach { creditoSalvo ->
                try {
                    consultaRepository.deletarPorId(creditoSalvo.idConsulta)
                    log.warn("ROLLBACK: Consulta crédito ${creditoSalvo.idConsulta} deletada.")
                } catch (rbError: Exception) {
                    log.error("ERRO CRÍTICO DE ROLLBACK: Falha ao deletar consulta crédito ${creditoSalvo.idConsulta}", rbError)
                }
            }
            throw ConflitoDeEstadoException("Falha ao registrar pacote: ${e.message}")
        }
    }

    @OptIn(ExperimentalTime::class)
    suspend fun agendarConsultaPaciente(
        paciente: Paciente,
        profissional: Profissional,
        dataHora: LocalDateTime,
        consultorioId: String,
        usuarioLogado: User,
        codigoPromocional: String? = null,
        quantidade: Int = 1
    ): ConsultaResponse {
        log.info("Iniciando agendamento de consulta avulsa para Paciente ${paciente.idPaciente} com Prof ${profissional.idProfissional} às $dataHora")
        verificarLimiteAgendamentosFuturos(paciente.idPaciente, isAgendamentoDuplo = false)
        checarPermissaoAgendamento(usuarioLogado, paciente)

        if (usuarioLogado.role == Role.PROFISSIONAL) {
            val perfilLogado = profissionalRepository.buscarPorUserId(usuarioLogado.idUsuario)
                ?: throw RecursoNaoEncontradoException("Perfil profissional não encontrado.")
            if (perfilLogado.idProfissional != profissional.idProfissional) {
                throw NaoAutorizadoException("Profissionais só podem agendar em suas próprias agendas.")
            }
        }

        val novaConsulta = criarEValidarConsulta(paciente, profissional, dataHora, consultorioId)
        log.debug("Objeto de consulta preliminar criado: ${novaConsulta.idConsulta}")

        val promocoesAplicadas = promocaoService.buscarMelhorPromocaoAplicavel(
            paciente = paciente,
            profissional = profissional,
            dataConsultaProposta = novaConsulta.dataHoraConsulta!!,
            quantidadeConsultasSimultaneas = quantidade,
            codigoPromocionalInput = codigoPromocional
        )
        log.debug("Promoções aplicáveis encontradas: ${promocoesAplicadas.size}")

        val descontoTotal = calcularDescontoTotal(promocoesAplicadas)
        novaConsulta.aplicarDesconto(descontoTotal)
        novaConsulta.promocoesAplicadasIds = promocoesAplicadas.map { it.idPromocao }

        var consultaSalva: Consulta? = null
        try {
            consultaSalva = consultaRepository.salvar(novaConsulta)
            log.info("Consulta ${consultaSalva.idConsulta} salva no banco.")

            if (consultaSalva.dataHoraConsulta != null) {
                val dataHoraLocal = consultaSalva.dataHoraConsulta!!.toLocalDateTime(fusoHorarioPadrao)
                profissional.agenda.bloquearHorario(dataHoraLocal, consultaSalva)

                profissionalRepository.atualizar(profissional)
                log.info("Agenda do Prof ${profissional.idProfissional} atualizada.")
            }

            return consultaSalva!!.toResponse(consultorioRepository, areaAtuacaoRepository)

        } catch (e: Exception) {
            log.error("Falha ao registrar agendamento para Paciente ${paciente.idPaciente}. Iniciando rollback.", e)
            if (consultaSalva != null) {
                try {
                    consultaRepository.deletarPorId(consultaSalva.idConsulta)
                    log.warn("ROLLBACK: Consulta ${consultaSalva.idConsulta} deletada devido a falha no bloqueio da agenda.")
                } catch (rbError: Exception) {
                    log.error("ERRO CRÍTICO DE ROLLBACK: Falha ao deletar consulta ${consultaSalva.idConsulta}", rbError)
                }
            }
            throw ConflitoDeEstadoException("Falha ao registrar agendamento: ${e.message}")
        }
    }

    companion object{
        fun calcularDescontoTotal(promocoesAplicadas: List<Promocao>): Double {
            val cumulativas = promocoesAplicadas.filter { it.isCumulativa }
            val naoCumulativas = promocoesAplicadas.filterNot { it.isCumulativa }

            val melhorNaoCumulativa = naoCumulativas.maxByOrNull { it.percentualDesconto }

            var descontoTotal = cumulativas.sumOf { it.percentualDesconto }
            if (melhorNaoCumulativa != null) {
                descontoTotal += melhorNaoCumulativa.percentualDesconto
            }

            return min(descontoTotal, 100.0)
        }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun criarEValidarConsulta(
        paciente: Paciente,
        profissional: Profissional,
        dataHora: LocalDateTime,
        consultorioId: String
    ): Consulta {
        log.debug("Validando criação de consulta: Pac=${paciente.idPaciente}, Prof=${profissional.idProfissional}, Data=$dataHora, Local=$consultorioId")

        profissional.diasDeTrabalho.firstOrNull {
            it.diaDaSemana == dataHora.dayOfWeek &&
                    it.consultorioId == consultorioId &&
                    dataHora.time >= it.horarioInicio &&
                    dataHora.time < it.horarioFim
        } ?: throw ConflitoDeEstadoException("O profissional não atende neste consultório ($consultorioId) neste dia/horário.")
        log.debug("Validação de local de atendimento OK.")

        val novaConsulta = Consulta(
            pacienteID = paciente.idPaciente,
            nomePaciente = paciente.nomePaciente,
            profissionalID = profissional.idProfissional,
            nomeProfissional = profissional.nomeProfissional,
            consultorioId = consultorioId,
            area = profissionalRepository.buscarPorId(profissional.idProfissional)?.areaAtuacaoId ?: "Desconhecida",
            dataHoraConsulta = dataHora.toInstant(fusoHorarioPadrao),
            statusConsulta = StatusConsulta.AGENDADA,
            valorBase = profissional.valorBaseConsulta,
            valorConsulta = profissional.valorBaseConsulta,
            duracaoEmMinutos = profissional.duracaoPadraoMinutos
        )
        val duracaoDaConsulta = profissional.duracaoPadraoMinutos.minutes

        if (!profissional.agenda.estaDisponivel(dataHora, duracaoDaConsulta)) {
            throw ConflitoDeEstadoException("Horário do profissional indisponível")
        }
        if (!pacienteService.isPacienteDisponivel(paciente, dataHora, duracaoDaConsulta)) {
            throw ConflitoDeEstadoException("Horário do paciente indisponível")
        }
        log.debug("Validação de disponibilidade OK.")

        return novaConsulta
    }

    private suspend fun checarPermissaoAgendamento(
        usuarioLogado: User,
        pacienteDaConsulta: Paciente
    ) {
        log.debug("Checando permissão de agendamento: User ${usuarioLogado.idUsuario} (Role ${usuarioLogado.role}) agendando para Paciente ${pacienteDaConsulta.idPaciente}")

        if (usuarioLogado.isSuperAdmin || usuarioLogado.role == Role.RECEPCIONISTA) return

        when (usuarioLogado.role) {
            Role.PACIENTE -> {
                val perfilPacienteLogado = pacienteRepository.buscarPorUserId(usuarioLogado.idUsuario)
                    ?: throw RecursoNaoEncontradoException("Perfil de paciente não encontrado para este usuário.")
                if (perfilPacienteLogado.idPaciente != pacienteDaConsulta.idPaciente) {
                    throw NaoAutorizadoException("Pacientes só podem agendar consultas para si mesmos.")
                }
                return
            }
            Role.PROFISSIONAL -> return
            else -> {}
        }
    }

    suspend fun buscarOuPreCadastrarPaciente(
        email: String,
        nome: String,
        usuarioLogado: User
    ): Paciente {
        log.info("Staff ${usuarioLogado.idUsuario} buscando ou pré-cadastrando paciente: $email")
        if (usuarioLogado.role == Role.PACIENTE) {
            throw NaoAutorizadoException("Pacientes não podem usar esta função de busca/cadastro.")
        }
        if (emailBlocklistRepository.buscarPorEmail(email) != null) {
            throw EmailBloqueadoException("Este email está bloqueado...")
        }

        val userExistente = userRepository.buscarPorEmail(email)

        if (userExistente != null) {
            log.debug("Usuário existente encontrado para $email. Verificando perfil de paciente.")
            if (userExistente.role != Role.PACIENTE) {
                throw InputInvalidoException("Este email pertence a um membro da equipe, não a um paciente.")
            }
            val pacienteExistente = pacienteRepository.buscarPorUserId(userExistente.idUsuario)
                ?: throw RecursoNaoEncontradoException("Usuário encontrado, mas perfil de paciente associado não existe.")

            if (pacienteExistente.status == StatusUsuario.INATIVO) {
                log.warn("Tentativa de agendamento para paciente INATIVO: ${pacienteExistente.idPaciente}")
                throw PacienteInativoException(
                    "Este paciente existe mas está INATIVO. Reative-o para agendar.",
                    pacienteExistente.idPaciente
                )
            } else if (pacienteExistente.status != StatusUsuario.ATIVO) {
                throw ConflitoDeEstadoException("Este paciente existe mas não está ativo no sistema (${pacienteExistente.status}).")
            }
            log.info("Paciente existente ${pacienteExistente.idPaciente} encontrado e ATIVO.")
            return pacienteExistente
        } else {
            if (nome.isBlank()) {
                throw InputInvalidoException("Nome do paciente é obrigatório para pré-cadastro.")
            }
            log.info("Paciente não encontrado. Iniciando pré-cadastro pelo staff para: $email")
            return usuarioService.preCadastrarPacientePeloStaff(nome, email, usuarioLogado)
        }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun verificarLimiteAgendamentosFuturos(pacienteId: String, isAgendamentoDuplo: Boolean) {
        val agora = Clock.System.now()
        val consultasFuturasAgendadas = consultaRepository.buscarPorPacienteId(pacienteId)
            .filter {
                it.statusConsulta == StatusConsulta.AGENDADA &&
                        it.dataHoraConsulta != null &&
                        it.dataHoraConsulta!! > agora
            }

        if (isAgendamentoDuplo) {
            if (consultasFuturasAgendadas.isNotEmpty()) {
                log.warn("Agendamento duplo bloqueado para Paciente $pacienteId: já possui ${consultasFuturasAgendadas.size} consultas futuras.")
                throw ConflitoDeEstadoException("Não é possível agendar consulta dupla inicial se já existem agendamentos futuros.")
            }
        } else {
            if (consultasFuturasAgendadas.isNotEmpty()) {
                log.warn("Agendamento avulso bloqueado para Paciente $pacienteId: já possui ${consultasFuturasAgendadas.size} consultas futuras.")
                throw ConflitoDeEstadoException("Você já possui uma consulta futura agendada. Aguarde a realização ou cancele-a para agendar uma nova.")
            }
        }
    }

}