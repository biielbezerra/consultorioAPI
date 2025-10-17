package com.nutriAPI.services

import com.nutriAPI.models.Agenda
import com.nutriAPI.models.Consulta
import com.nutriAPI.models.Paciente
import com.nutriAPI.models.Profissional
import com.nutriAPI.models.StatusConsulta
import java.awt.geom.Area
import java.time.Duration
import java.time.LocalDateTime

class ConsultorioService {

    fun agendarPrimeiraConsultaDupla(
        paciente: Paciente,
        profissional: Profissional,
        dataHora1: LocalDateTime,
        dataHora2: LocalDateTime,
        quantidade: Int = 2
    ): List<Consulta> {
        val consultas = mutableListOf<Consulta>()
        val primeiraConsulta = Consulta(
            pacienteID = paciente.idPaciente,
            nomePaciente = paciente.nomePaciente,
            profissionalID = profissional.idProfissional,
            nomeProfissional = profissional.nomeProfissional,
            area = profissional.areaAtuacao,
            dataHoraConsulta = dataHora1,
            statusConsulta = StatusConsulta.AGENDADA,
            valorBase = profissional.valorBaseConsulta,
            valorConsulta = profissional.valorBaseConsulta,
        )
        val segundaConsulta = Consulta(
            pacienteID = paciente.idPaciente,
            nomePaciente = paciente.nomePaciente,
            profissionalID = profissional.idProfissional,
            nomeProfissional = profissional.nomeProfissional,
            area = profissional.areaAtuacao,
            dataHoraConsulta = dataHora2,
            statusConsulta = StatusConsulta.AGENDADA,
            valorBase = profissional.valorBaseConsulta,
            valorConsulta = profissional.valorBaseConsulta,
        )

        if (!validarHorarioParaAgendamento(profissional.agenda, dataHora1)) {
            throw IllegalArgumentException("Horário do profissional indisponível")
        }
        if (!validarHorarioParaAgendamento(paciente.agenda,dataHora1)){
            throw IllegalArgumentException("Horário do paciente indisponível")
        }
        if (!validarHorarioParaAgendamento(profissional.agenda, dataHora2)) {
            throw IllegalArgumentException("Horário do profissional indisponível")
        }
        if (!validarHorarioParaAgendamento(paciente.agenda,dataHora2)){
            throw IllegalArgumentException("Horário do paciente indisponível")
        }
        if (validarAgendaPaciente(paciente,dataHora1,primeiraConsulta.duracao)) {
            throw IllegalArgumentException("Paciente já possui consulta neste horário")
        }
        if (validarAgendaProfissional(profissional,dataHora1,primeiraConsulta.duracao)){
            throw IllegalArgumentException("Profissional já possui consulta neste horário")
        }
        if (validarAgendaPaciente(paciente,dataHora2,segundaConsulta.duracao)) {
            throw IllegalArgumentException("Paciente já possui consulta neste horário")
        }
        if (validarAgendaProfissional(profissional,dataHora2,segundaConsulta.duracao)){
            throw IllegalArgumentException("Profissional já possui consulta neste horário")
        }

        val desconto = calcularDescontoAutomatico(paciente, quantidade)
        //primeira consulta
        primeiraConsulta.descontoPercentual = desconto
        primeiraConsulta.valorConsulta = calcularValorConsulta(desconto, primeiraConsulta)

        registrarConsulta(paciente, profissional, primeiraConsulta)
        consultas.add(primeiraConsulta)

        //segunda consulta
        segundaConsulta.descontoPercentual = desconto
        segundaConsulta.valorConsulta = calcularValorConsulta(desconto, segundaConsulta)

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

        if (!validarHorarioParaAgendamento(profissional.agenda, dataHora)) {
            throw IllegalArgumentException("Horário do profissional indisponível")
        }
        if (!validarHorarioParaAgendamento(paciente.agenda,dataHora)){
            throw IllegalArgumentException("Horário do paciente indisponível")
        }
        if (validarAgendaPaciente(paciente,dataHora,novaConsulta.duracao)) {
            throw IllegalArgumentException("Paciente já possui consulta neste horário")
        }
        if (validarAgendaProfissional(profissional,dataHora,novaConsulta.duracao)){
            throw IllegalArgumentException("Profissional já possui consulta neste horário")
        }

        val desconto = calcularDescontoAutomatico(paciente, quantidade)
        novaConsulta.descontoPercentual = desconto
        novaConsulta.valorConsulta = calcularValorConsulta(desconto, novaConsulta)

        registrarConsulta(paciente, profissional, novaConsulta)

        return novaConsulta
    }

    fun agendarConsultaProfissional(paciente: Paciente,
                                    profissional: Profissional,
                                    dataHora: LocalDateTime,
                                    descontoManual: Boolean = false,
                                    descontoManualValor: Double,
                                    quantidade: Int = 1): Consulta {

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

        if (!validarHorarioParaAgendamento(profissional.agenda, dataHora) ||
            !validarHorarioParaAgendamento(paciente.agenda,dataHora)) {
            throw IllegalArgumentException("Horário indisponível")
        }

        if (validarAgendaPaciente(paciente,dataHora,novaConsulta.duracao) ||
            validarAgendaProfissional(profissional,dataHora,novaConsulta.duracao)) {
            throw IllegalArgumentException("Paciente já possui consulta neste horário")
        }

        if(descontoManual){
            novaConsulta.valorConsulta = aplicarDescontoManual(novaConsulta,descontoManualValor)
            registrarConsulta(paciente, profissional, novaConsulta)
            return novaConsulta
        }

        var desconto = calcularDescontoAutomatico(paciente,quantidade)
        novaConsulta.descontoPercentual = desconto
        novaConsulta.valorConsulta = calcularValorConsulta(desconto,novaConsulta)
        registrarConsulta(paciente, profissional, novaConsulta)
        return novaConsulta
    }

    fun calcularDescontoAutomatico(paciente: Paciente, quantidade: Int): Double {
        return when {
            paciente.consultasPaciente.isEmpty() && quantidade == 2 -> 11.76
            isClienteFiel(paciente) -> 12.0
            else -> 0.0
        }
    }

    fun verificarInatividade(paciente: Paciente): Boolean {
        val noventaDiasAtras = LocalDateTime.now().minusDays(90)
        return paciente.consultasPaciente.none() { consulta ->
            consulta.statusConsulta == StatusConsulta.REALIZADA &&
                    consulta.dataHoraConsulta.isAfter(noventaDiasAtras)
        }
    }

    fun isClienteFiel(paciente: Paciente): Boolean {
        val noventaDiasAtras = LocalDateTime.now().minusDays(90)
        return paciente.consultasPaciente.any() { consulta ->
            consulta.statusConsulta == StatusConsulta.REALIZADA &&
                    consulta.dataHoraConsulta.isAfter(noventaDiasAtras)
        }
    }

    fun calcularValorConsulta(desconto: Double, consulta: Consulta): Double {
        consulta.valorConsulta = consulta.valorBase * (1 - (desconto / 100))
        return consulta.valorConsulta
    }


    fun validarHorarioParaAgendamento(agenda: Agenda, horario: LocalDateTime): Boolean {
        return agenda.estaDisponivel(horario)
    }

    fun validarAgendaPaciente(paciente: Paciente, novaData: LocalDateTime, duracao: Duration): Boolean {
        val novaConsultaFim = novaData.plus(duracao)
        return paciente.consultasPaciente.any { consulta ->
            consulta.statusConsulta == StatusConsulta.AGENDADA &&
                    novaData.isBefore(consulta.horarioFim()) &&
                    consulta.dataHoraConsulta.isBefore(novaConsultaFim)
        }
    }

    fun validarAgendaProfissional(profissional: Profissional, novaData: LocalDateTime, duracao: Duration): Boolean {
        val novaConsultaFim = novaData.plus(duracao)
        return profissional.consultasProfissional.any { consulta ->
            consulta.statusConsulta == StatusConsulta.AGENDADA &&
                    novaData.isBefore(consulta.horarioFim()) &&
                    consulta.dataHoraConsulta.isBefore(novaConsultaFim)
        }
    }


    fun reagendarConsulta(consulta: Consulta, novaData: LocalDateTime) {
        TODO()
    }

    fun aplicarDescontoManual(consulta: Consulta, desconto: Double): Double {
        consulta.descontoPercentual = desconto
        consulta.valorConsulta = consulta.valorBase * (1 - desconto / 100)
        return consulta.valorConsulta
    }

    fun cancelarConsulta() {
        TODO()
    }

    fun registrarConsulta(paciente: Paciente, profissional: Profissional, consulta: Consulta) {
        paciente.consultasPaciente.add(consulta)
        paciente.agenda.horariosBloqueados.add((consulta.dataHoraConsulta))
        profissional.consultasProfissional.add(consulta)
        profissional.agenda.horariosBloqueados.add(consulta.dataHoraConsulta)
    }


}