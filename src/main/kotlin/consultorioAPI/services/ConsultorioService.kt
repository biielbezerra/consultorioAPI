package com.consultorioAPI.services

import com.consultorioAPI.models.Consulta
import com.consultorioAPI.models.Consultorio
import com.consultorioAPI.models.Paciente
import com.consultorioAPI.models.Profissional
import com.consultorioAPI.models.Role
import com.consultorioAPI.models.StatusConsulta
import com.consultorioAPI.models.User
import com.consultorioAPI.repositories.ConsultaRepository
import com.consultorioAPI.repositories.ConsultorioRepository
import com.consultorioAPI.repositories.PacienteRepository
import com.consultorioAPI.repositories.ProfissionalRepository
import kotlinx.datetime.*
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class ConsultorioService (private val consultorioRepository: ConsultorioRepository,
                          private val consultaRepository: ConsultaRepository,
                          private val pacienteService: PacienteService,
                          private val agendaService: AgendaService,
                          private val pacienteRepository: PacienteRepository,
                          private val profissionalRepository: ProfissionalRepository
) {

    val descontoConsultaDupla = 11.76
    val descontoClienteFiel = 11.76

    suspend fun cadastroConsultorio(nome: String, endereco: String, usuarioLogado: User): Consultorio {

        if(usuarioLogado.role != Role.SUPER_ADMIN){
            throw SecurityException("Apenas Super Admins podem cadastrar consultórios.")
        }

        val novoConsultorio = Consultorio(nomeConsultorio = nome, endereco = endereco)
        return consultorioRepository.salvar(novoConsultorio)
    }

    suspend fun agendarPrimeiraConsultaDupla(
        paciente: Paciente,
        profissional: Profissional,
        dataHora1: LocalDateTime,
        dataHora2: LocalDateTime,
        usuarioLogado: User,
        quantidade: Int = 2
    ): List<Consulta> {

        checarPermissaoAgendamento(usuarioLogado, paciente)

        if (usuarioLogado.role == Role.PROFISSIONAL) {
            val perfilLogado = profissionalRepository.buscarPorUserId(usuarioLogado.idUsuario)
                ?: throw SecurityException("Perfil profissional não encontrado.")
            if (perfilLogado.idProfissional != profissional.idProfissional) {
                throw SecurityException("Profissionais só podem agendar em suas próprias agendas.")
            }
        }

        val consultas = mutableListOf<Consulta>()
        val primeiraConsulta = criarEValidarConsulta(paciente, profissional,dataHora1)
        val segundaConsulta = criarEValidarConsulta(paciente, profissional,dataHora2)

        val desconto = calcularDescontoAutomatico(paciente, quantidade)

        //primeira consulta
        primeiraConsulta.aplicarDesconto(desconto)

        registrarConsulta(paciente, profissional, primeiraConsulta)
        consultas.add(primeiraConsulta)

        //segunda consulta
        segundaConsulta.aplicarDesconto(desconto)

        registrarConsulta(paciente, profissional, segundaConsulta)
        consultas.add(segundaConsulta)

        return consultas
    }

    suspend fun agendarConsultaPaciente(
        paciente: Paciente,
        profissional: Profissional,
        dataHora: LocalDateTime,
        usuarioLogado: User,
        quantidade: Int = 1
    ): Consulta {
        checarPermissaoAgendamento(usuarioLogado, paciente)

        if (usuarioLogado.role == Role.PROFISSIONAL) {
            val perfilLogado = profissionalRepository.buscarPorUserId(usuarioLogado.idUsuario)
                ?: throw SecurityException("Perfil profissional não encontrado.")
            if (perfilLogado.idProfissional != profissional.idProfissional) {
                throw SecurityException("Profissionais só podem agendar em suas próprias agendas.")
            }
        }

        val novaConsulta = criarEValidarConsulta(paciente, profissional,dataHora)
        val desconto = calcularDescontoAutomatico(paciente, quantidade)
        novaConsulta.aplicarDesconto(desconto)
        registrarConsulta(paciente, profissional, novaConsulta)

        return novaConsulta
    }

    suspend fun agendarConsultaProfissional(paciente: Paciente,
                                    profissional: Profissional,
                                    dataHora: LocalDateTime,
                                    descontoManual: Boolean = false,
                                    descontoManualValor: Double,
                                    usuarioLogado: User,
                                    quantidade: Int = 1): Consulta {

        checarPermissaoAgendamento(usuarioLogado, paciente)

        if (usuarioLogado.role == Role.PROFISSIONAL) {
            val perfilLogado = profissionalRepository.buscarPorUserId(usuarioLogado.idUsuario)
                ?: throw SecurityException("Perfil profissional não encontrado.")
            if (perfilLogado.idProfissional != profissional.idProfissional) {
                throw SecurityException("Profissionais só podem agendar em suas próprias agendas.")
            }
        }

        val novaConsulta = criarEValidarConsulta(paciente, profissional,dataHora)

        if(descontoManual){
            novaConsulta.aplicarDesconto(descontoManualValor)
            registrarConsulta(paciente, profissional, novaConsulta)
            return novaConsulta
        }

        var desconto = calcularDescontoAutomatico(paciente,quantidade)
        novaConsulta.aplicarDesconto(desconto)
        registrarConsulta(paciente, profissional, novaConsulta)
        return novaConsulta
    }

    suspend fun calcularDescontoAutomatico(paciente: Paciente, quantidade: Int): Double {
        val temConsultasAnteriores = consultaRepository.buscarPorPacienteId(paciente.idPaciente).isNotEmpty()
        return when {
            !temConsultasAnteriores && quantidade == 2 -> descontoConsultaDupla
            pacienteService.isClienteFiel(paciente) -> descontoClienteFiel
            else -> 0.0
        }
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

}