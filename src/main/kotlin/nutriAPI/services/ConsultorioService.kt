package com.nutriAPI.services

import com.nutriAPI.models.Agenda
import com.nutriAPI.models.Consulta
import com.nutriAPI.models.Paciente
import com.nutriAPI.models.Profissional
import com.nutriAPI.models.StatusConsulta
import java.awt.geom.Area
import java.time.Duration
import java.time.LocalDateTime

class ConsultorioService (private val pacienteService: PacienteService) {

    val descontoConsultaDupla = 11.76
    val descontoClienteFiel = 12.0

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
        return when {
            paciente.consultasPaciente.isEmpty() && quantidade == 2 -> descontoConsultaDupla
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
        paciente.consultasPaciente.add(consulta)
        profissional.consultasProfissional.add(consulta)
        profissional.agenda.bloquearHorario(consulta.dataHoraConsulta,consulta)

    }

}