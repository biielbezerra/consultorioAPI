package com.consultorioAPI.services

import com.consultorioAPI.models.Agenda
import com.consultorioAPI.models.Consulta
import com.consultorioAPI.models.Consultorio
import com.consultorioAPI.models.HorarioTrabalho
import com.consultorioAPI.models.Nutricionista
import com.consultorioAPI.models.Paciente
import com.consultorioAPI.models.Profissional
import com.consultorioAPI.models.StatusConsulta
import com.consultorioAPI.repositories.ConsultaRepository
import com.consultorioAPI.repositories.ConsultorioRepository
import com.consultorioAPI.repositories.PacienteRepository
import com.consultorioAPI.repositories.ProfissionalRepository
import java.time.LocalDateTime

class ConsultorioService (private val pacienteRepository: PacienteRepository,
                          private val profissionalRepository: ProfissionalRepository,
                          private val consultorioRepository: ConsultorioRepository,
                          private val consultaRepository: ConsultaRepository,
                          private val pacienteService: PacienteService,
                          private val agendaService: AgendaService
) {

    val descontoConsultaDupla = 11.76
    val descontoClienteFiel = 12.0

    fun cadastroConsultorio(nome: String, endereco: String): Consultorio {
        val novoConsultorio = Consultorio(nomeConsultorio = nome, endereco = endereco)
        return consultorioRepository.salvar(novoConsultorio)
    }

    fun adicionarPaciente(nome: String, email: String, senha: String): Paciente {
        val novoPaciente = Paciente(
            nomePaciente = nome,
            email = email,
            senha = senha
        )
        novoPaciente.dataCadastro = LocalDateTime.now()

        return pacienteRepository.salvar(novoPaciente)
    }

    fun adicionarNutri(
        nome: String,
        email: String,
        senha: String,
        diasDeTrabalho: List<HorarioTrabalho>
    ): Nutricionista {
        val novaAgenda = Agenda(mutableListOf(), mutableListOf())
        val novoNutricionista = Nutricionista(
            nomeNutri = nome,
            email = email,
            senha = senha,
            agenda = novaAgenda,
            diasDeTrabalho = diasDeTrabalho
        )
        agendaService.inicializarAgenda(novoNutricionista)
        return profissionalRepository.salvar(novoNutricionista) as Nutricionista
    }

    fun adicionarProfissional(
        nome: String,
        email: String,
        senha: String,
        areaAtuacao: String,
        diasDeTrabalho: List<HorarioTrabalho>
    ): Profissional {
        val novaAgenda = Agenda(
            mutableListOf(),
            mutableListOf()
        )
        val novoProfissional = Profissional(
            nomeProfissional = nome,
            email = email,
            senha = senha,
            areaAtuacao = areaAtuacao,
            agenda = novaAgenda,
            diasDeTrabalho = diasDeTrabalho
        )
        agendaService.inicializarAgenda(novoProfissional)

        return profissionalRepository.salvar(novoProfissional)
    }

    fun agendarPrimeiraConsultaDupla(
        paciente: Paciente,
        profissional: Profissional,
        dataHora1: LocalDateTime,
        dataHora2: LocalDateTime,
        quantidade: Int = 2
    ): List<Consulta> {
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

    fun agendarConsultaPaciente(
        paciente: Paciente,
        profissional: Profissional,
        dataHora: LocalDateTime,
        quantidade: Int = 1
    ): Consulta {

        val novaConsulta = criarEValidarConsulta(paciente, profissional,dataHora)

        val desconto = calcularDescontoAutomatico(paciente, quantidade)
        novaConsulta.aplicarDesconto(desconto)

        registrarConsulta(paciente, profissional, novaConsulta)

        return novaConsulta
    }

    fun agendarConsultaProfissional(paciente: Paciente,
                                    profissional: Profissional,
                                    dataHora: LocalDateTime,
                                    descontoManual: Boolean = false,
                                    descontoManualValor: Double,
                                    quantidade: Int = 1): Consulta {

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

    fun calcularDescontoAutomatico(paciente: Paciente, quantidade: Int): Double {
        val temConsultasAnteriores = consultaRepository.buscarPorPacienteId(paciente.idPaciente).isNotEmpty()
        return when {
            !temConsultasAnteriores && quantidade == 2 -> descontoConsultaDupla
            pacienteService.isClienteFiel(paciente) -> descontoClienteFiel
            else -> 0.0
        }
    }

    private fun criarEValidarConsulta(
        paciente: Paciente,
        profissional: Profissional,
        dataHora: LocalDateTime
    ): Consulta {
        val novaConsulta = Consulta(
            pacienteID = paciente.idPaciente,
            nomePaciente = paciente.nomePaciente,
            profissionalID = profissional.idProfissional,
            nomeProfissional = profissional.nomeProfissional,
            area = profissional.areaAtuacao,
            dataHoraConsulta = dataHora,
            statusConsulta = StatusConsulta.AGENDADA,
            valorBase = profissional.valorBaseConsulta,
            valorConsulta = profissional.valorBaseConsulta
        )

        if (!profissional.agenda.estaDisponivel(dataHora,novaConsulta.duracao)) {
            throw IllegalArgumentException("Horário do profissional indisponível")
        }
        if (!pacienteService.isPacienteDisponivel(paciente, dataHora, novaConsulta.duracao)) {
            throw IllegalArgumentException("Horário do paciente indisponível")
        }

        return novaConsulta
    }

    private fun registrarConsulta(paciente: Paciente, profissional: Profissional, consulta: Consulta) {
        consultaRepository.salvar(consulta)
        profissional.agenda.bloquearHorario(consulta.dataHoraConsulta,consulta)

    }

}