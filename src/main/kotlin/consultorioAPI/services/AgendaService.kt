package com.consultorioAPI.services

import com.consultorioAPI.models.Agenda
import com.consultorioAPI.models.HorarioTrabalho
import com.consultorioAPI.models.Profissional
import com.consultorioAPI.models.Role
import com.consultorioAPI.models.User
import com.consultorioAPI.repositories.ProfissionalRepository
import java.time.LocalDateTime
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

class AgendaService(private val profissionalRepository: ProfissionalRepository) {

    fun gerarDisponibilidadePadrao(
        agenda: Agenda,
        regras: List<HorarioTrabalho>,
        dataInicio: LocalDate,
        dataFim: LocalDate,
        tamanhoSlot: Duration = Duration.ofMinutes(30)
        ){

        var dataAtual = dataInicio

        while (!dataAtual.isAfter(dataFim)){
            val blocosDeTrabalhoDoDia = regras.filter { it.diaDaSemana == dataAtual.dayOfWeek }

            blocosDeTrabalhoDoDia.forEach { bloco ->
                val inicioDoBloco = LocalDateTime.of(dataAtual, bloco.horarioInicio)
                val fimDoBloco = LocalDateTime.of(dataAtual, bloco.horarioFim)

                definirHorarioDisponivel(agenda, inicioDoBloco, fimDoBloco, tamanhoSlot)
            }
            dataAtual = dataAtual.plusDays(1)
        }
    }

    fun definirHorarioDisponivel(
        agenda: Agenda,
        horario: LocalDateTime,
        fim: LocalDateTime,
        tamanhoSlot: Duration = Duration.ofMinutes(30)
    ) {
        var atual = horario

        while(atual.isBefore(fim)){
            if(!agenda.horariosDisponiveis.contains(atual)){
                agenda.horariosDisponiveis.add(atual)
            }
            atual = atual.plus(tamanhoSlot)
        }

    }

    fun inicializarAgenda(profissional: Profissional) {
        val hoje = LocalDate.now()
        val dataFutura = hoje.plusWeeks(4)

        gerarDisponibilidadePadrao(
            agenda = profissional.agenda,
            regras = profissional.diasDeTrabalho,
            dataInicio = hoje,
            dataFim = dataFutura
        )
    }

    fun removerHorariosIntervalo(agenda: Agenda, inicio: LocalDateTime, fim: LocalDateTime, tamanhoSlot: Duration = Duration.ofMinutes(30) ) {
        var horarioAtual = inicio
        while (horarioAtual.isBefore(fim)) {
            agenda.horariosDisponiveis.remove(horarioAtual)
            horarioAtual = horarioAtual.plus(tamanhoSlot)
        }
    }

    fun definirDiaDeFolga(profissional: Profissional, diaDeFolga: LocalDate, usuarioLogado: User) {

        when (usuarioLogado.role) {
            Role.SUPER_ADMIN, Role.RECEPCIONISTA -> {}

            Role.PROFISSIONAL -> {
                val perfilProfissionalLogado = profissionalRepository.buscarPorUserId(usuarioLogado.idUsuario)
                    ?: throw SecurityException("Perfil profissional não encontrado.")

                if (perfilProfissionalLogado.idProfissional != profissional.idProfissional) {
                    throw SecurityException("Profissionais só podem definir folgas para suas próprias agendas.")
                }
            }
            Role.PACIENTE -> throw SecurityException("Pacientes não podem definir dias de folga.")
        }

        val blocosDeTrabalhoDoDia = profissional.diasDeTrabalho.filter { it.diaDaSemana == diaDeFolga.dayOfWeek }

        if (blocosDeTrabalhoDoDia.isEmpty()) {
            return
        }

        blocosDeTrabalhoDoDia.forEach { bloco ->
            val inicioDoBloco = LocalDateTime.of(diaDeFolga, bloco.horarioInicio)
            val fimDoBloco = LocalDateTime.of(diaDeFolga, bloco.horarioFim)

            removerHorariosIntervalo(profissional.agenda, inicioDoBloco, fimDoBloco)
        }
    }

    fun listarHorariosDisponiveisPorLocal(
        profissional: Profissional,
        consultorioId: String,
        duracao: Duration = Duration.ofMinutes(60)
    ): List<LocalDateTime> {

        val todosHorariosPossiveis = listarHorariosDisponiveis(profissional.agenda, duracao)

        return todosHorariosPossiveis.filter { horario ->
            profissional.diasDeTrabalho.any { regra ->
                regra.consultorioId == consultorioId &&
                        regra.diaDaSemana == horario.dayOfWeek &&
                        !horario.toLocalTime().isBefore(regra.horarioInicio) &&
                        !horario.toLocalTime().plus(duracao).isAfter(regra.horarioFim)
            }
        }
    }

    fun listarHorariosDisponiveis(
        agenda: Agenda,
        duracao: Duration = Duration.ofMinutes(60)
    ): List<LocalDateTime> {
        return agenda.horariosDisponiveis
            .filter { !agenda.horariosBloqueados.contains(it) }
            .filter { horarioDeInicio -> agenda.estaDisponivel(horarioDeInicio, duracao) }
    }

}

enum class StatusSlot { DISPONIVEL, OCUPADO, PASSADO, FORA_DO_HORARIO_DE_TRABALHO }

fun obterStatusDaAgenda(
    profissional: Profissional,
    dataInicio: LocalDate,
    dataFim: LocalDate
): Map<LocalDateTime, StatusSlot> {
    val statusMap = mutableMapOf<LocalDateTime, StatusSlot>()
    var dataAtual = dataInicio

    while (!dataAtual.isAfter(dataFim)) {
        val blocosDeTrabalho = profissional.diasDeTrabalho.filter { it.diaDaSemana == dataAtual.dayOfWeek }
        if (blocosDeTrabalho.isEmpty()) {
            var slotDoDiaDeFolga = LocalDateTime.of(dataAtual, LocalTime.of(8, 0))
            val fimDoDiaDeFolga = LocalDateTime.of(dataAtual, LocalTime.of(18, 0))
            while (slotDoDiaDeFolga.isBefore(fimDoDiaDeFolga)) {
                statusMap[slotDoDiaDeFolga] = StatusSlot.FORA_DO_HORARIO_DE_TRABALHO
                slotDoDiaDeFolga = slotDoDiaDeFolga.plusMinutes(30)
            }
        } else {
            blocosDeTrabalho.forEach { bloco ->
                var slotAtual = LocalDateTime.of(dataAtual, bloco.horarioInicio)
                val fimDoBloco = LocalDateTime.of(dataAtual, bloco.horarioFim)
                while (slotAtual.isBefore(fimDoBloco)) {
                    val status = when {
                        profissional.agenda.horariosBloqueados.contains(slotAtual) -> StatusSlot.OCUPADO
                        !profissional.agenda.horariosDisponiveis.contains(slotAtual) -> StatusSlot.FORA_DO_HORARIO_DE_TRABALHO
                        slotAtual.isBefore(LocalDateTime.now()) -> StatusSlot.PASSADO
                        else -> StatusSlot.DISPONIVEL
                    }
                    statusMap[slotAtual] = status
                    slotAtual = slotAtual.plusMinutes(30)
                }
            }
        }
        dataAtual = dataAtual.plusDays(1)
    }
    return statusMap
}