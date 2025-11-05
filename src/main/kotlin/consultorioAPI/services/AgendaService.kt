package com.consultorioAPI.services

import com.consultorioAPI.config.fusoHorarioPadrao
import com.consultorioAPI.exceptions.*
import com.consultorioAPI.models.Agenda
import com.consultorioAPI.models.HorarioTrabalho
import com.consultorioAPI.models.Profissional
import com.consultorioAPI.models.Role
import com.consultorioAPI.models.StatusConsulta
import com.consultorioAPI.models.User
import com.consultorioAPI.repositories.ConsultaRepository
import com.consultorioAPI.repositories.ProfissionalRepository
import kotlinx.datetime.*
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class AgendaService(private val profissionalRepository: ProfissionalRepository,
                    private val consultaRepository: ConsultaRepository){

    suspend fun gerarDisponibilidadePadrao(
        agenda: Agenda,
        regras: List<HorarioTrabalho>,
        dataInicio: LocalDate,
        dataFim: LocalDate,
        tamanhoSlot: Duration = 30.minutes
    ){
        var dataAtual = dataInicio

        while (dataAtual <= dataFim){
            val blocosDeTrabalhoDoDia = regras.filter { it.diaDaSemana == dataAtual.dayOfWeek }

            blocosDeTrabalhoDoDia.forEach { bloco ->
                val inicioDoBloco: LocalDateTime = dataAtual.atTime(bloco.horarioInicio)
                val fimDoBloco: LocalDateTime = dataAtual.atTime(bloco.horarioFim)

                definirHorarioDisponivel(agenda, inicioDoBloco, fimDoBloco, tamanhoSlot)
            }
            dataAtual = dataAtual.plus(1, DateTimeUnit.DAY)
        }
    }

    @OptIn(ExperimentalTime::class)
    fun definirHorarioDisponivel(
        agenda: Agenda,
        inicio: LocalDateTime,
        fim: LocalDateTime,
        tamanhoSlot: Duration = 30.minutes
    ) {
        var atual = inicio

        while(atual < fim){
            if(!agenda.horariosDisponiveis.contains(atual)){
                agenda.horariosDisponiveis.add(atual)
            }
            atual = atual.toInstant(fusoHorarioPadrao).plus(tamanhoSlot).toLocalDateTime(fusoHorarioPadrao)
        }
    }

    @OptIn(ExperimentalTime::class)
    suspend fun removerHorariosIntervalo(
        profissionalId: String,
        agenda: Agenda,
        inicio: LocalDateTime,
        fim: LocalDateTime,
        tamanhoSlot: Duration = 30.minutes
    ) {

        val consultasAgendadas = consultaRepository.buscarPorProfissionalId(profissionalId)
            .filter {
                it.statusConsulta == StatusConsulta.AGENDADA && it.dataHoraConsulta != null
            }

        val conflitos = consultasAgendadas.any { consulta ->
            val consultaInicio = consulta.dataHoraConsulta!!
            val consultaFim = consulta.horarioFim()!!
            val haSobreposicao = inicio < consultaFim && fim > consultaInicio
            haSobreposicao
        }

        if (conflitos) {
            throw ConflitoDeEstadoException("Não é possível bloquear este intervalo, pois já existem consultas agendadas no período.")
        }

        var horarioAtual = inicio
        while (horarioAtual < fim) {
            agenda.horariosDisponiveis.remove(horarioAtual)
            horarioAtual = horarioAtual.toInstant(fusoHorarioPadrao).plus(tamanhoSlot).toLocalDateTime(fusoHorarioPadrao)
        }
    }

    suspend fun definirDiaDeFolga(profissional: Profissional, diaDeFolga: LocalDate, usuarioLogado: User) {

        when (usuarioLogado.role) {
            Role.SUPER_ADMIN, Role.RECEPCIONISTA -> {}

            Role.PROFISSIONAL -> {
                val perfilProfissionalLogado = profissionalRepository.buscarPorUserId(usuarioLogado.idUsuario)
                    ?: throw RecursoNaoEncontradoException("Perfil profissional não encontrado.")

                if (perfilProfissionalLogado.idProfissional != profissional.idProfissional) {
                    throw NaoAutorizadoException("Profissionais só podem definir folgas para suas próprias agendas.")
                }
            }
            Role.PACIENTE -> throw NaoAutorizadoException("Pacientes não podem definir dias de folga.")
        }

        val blocosDeTrabalhoDoDia = profissional.diasDeTrabalho.filter { it.diaDaSemana == diaDeFolga.dayOfWeek }

        if (blocosDeTrabalhoDoDia.isEmpty()) {
            return
        }

        blocosDeTrabalhoDoDia.forEach { bloco ->
            val inicioDoBloco: LocalDateTime = diaDeFolga.atTime(bloco.horarioInicio)
            val fimDoBloco: LocalDateTime = diaDeFolga.atTime(bloco.horarioFim)
            removerHorariosIntervalo(profissional.idProfissional, profissional.agenda, inicioDoBloco, fimDoBloco)
        }
    }

    @OptIn(ExperimentalTime::class)
    fun listarHorariosDisponiveisPorLocal(
        profissional: Profissional,
        consultorioId: String,
        duracao: Duration = 60.minutes
    ): List<LocalDateTime> {

        val todosHorariosPossiveis = listarHorariosDisponiveis(profissional.agenda, duracao)

        return todosHorariosPossiveis.filter { horario ->
            profissional.diasDeTrabalho.any { regra ->
                val horarioLocal = horario.time
                regra.consultorioId == consultorioId &&
                        regra.diaDaSemana == horario.dayOfWeek &&
                        horarioLocal >= regra.horarioInicio &&
                        horario.toInstant(fusoHorarioPadrao).plus(duracao).toLocalDateTime(fusoHorarioPadrao).time <= regra.horarioFim
            }
        }
    }

    fun listarHorariosDisponiveis(
        agenda: Agenda,
        duracao: Duration = 60.minutes
    ): List<LocalDateTime> {
        val slotsPotenciais = agenda.horariosDisponiveis
            .filter { !agenda.horariosBloqueados.contains(it) }

        return slotsPotenciais.filter { horarioDeInicio ->
            agenda.estaDisponivel(horarioDeInicio, duracao)
        }
    }

enum class StatusSlot { DISPONIVEL, OCUPADO, PASSADO, FORA_DO_HORARIO_DE_TRABALHO }

    @OptIn(ExperimentalTime::class)
    fun obterStatusDaAgenda(
        profissional: Profissional,
        dataInicio: LocalDate,
        dataFim: LocalDate
    ): Map<LocalDateTime, StatusSlot> {
        val statusMap = mutableMapOf<LocalDateTime, StatusSlot>()
        var dataAtual = dataInicio
        val agora: Instant = Clock.System.now()

        while (dataAtual <= dataFim) {
            val blocosDeTrabalho = profissional.diasDeTrabalho.filter { it.diaDaSemana == dataAtual.dayOfWeek }
            if (blocosDeTrabalho.isEmpty()) {
                var slotDoDiaDeFolga: LocalDateTime = dataAtual.atTime(LocalTime(8, 0))
                val fimDoDiaDeFolga: LocalDateTime = dataAtual.atTime(LocalTime(18, 0))
                while (slotDoDiaDeFolga < fimDoDiaDeFolga) {
                    statusMap[slotDoDiaDeFolga] = StatusSlot.FORA_DO_HORARIO_DE_TRABALHO
                    slotDoDiaDeFolga = slotDoDiaDeFolga.toInstant(fusoHorarioPadrao).plus(30.minutes).toLocalDateTime(fusoHorarioPadrao)
                }
            } else {
                blocosDeTrabalho.forEach { bloco ->
                    var slotAtual: LocalDateTime = dataAtual.atTime(bloco.horarioInicio)
                    val fimDoBloco: LocalDateTime = dataAtual.atTime(bloco.horarioFim)
                    while (slotAtual < fimDoBloco) {
                        val slotInstant = slotAtual.toInstant(fusoHorarioPadrao)
                        val status = when {
                            profissional.agenda.horariosBloqueados.contains(slotAtual) -> StatusSlot.OCUPADO
                            !profissional.agenda.horariosDisponiveis.contains(slotAtual) -> StatusSlot.FORA_DO_HORARIO_DE_TRABALHO
                            slotInstant < agora -> StatusSlot.PASSADO
                            else -> StatusSlot.DISPONIVEL
                        }
                        statusMap[slotAtual] = status
                        slotAtual = slotAtual.toInstant(fusoHorarioPadrao).plus(30.minutes).toLocalDateTime(fusoHorarioPadrao)
                    }
                }
            }
            dataAtual = dataAtual.plus(1, DateTimeUnit.DAY)
        }
        return statusMap
    }
}