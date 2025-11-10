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
import org.slf4j.LoggerFactory

class AgendaService(private val profissionalRepository: ProfissionalRepository,
                    private val consultaRepository: ConsultaRepository){

    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun gerarDisponibilidadePadrao(
        agenda: Agenda,
        regras: List<HorarioTrabalho>,
        dataInicio: LocalDate,
        dataFim: LocalDate,
        tamanhoSlot: Duration = 30.minutes
    ){
        log.debug("Gerando disponibilidade padrão de $dataInicio até $dataFim")
        var dataAtual = dataInicio

        while (dataAtual <= dataFim){
            val blocosDeTrabalhoDoDia = regras.filter { it.diaDaSemana == dataAtual.dayOfWeek }

            blocosDeTrabalhoDoDia.forEach { bloco ->
                log.trace("Aplicando regra para $dataAtual: ${bloco.horarioInicio}-${bloco.horarioFim} no Consultório ${bloco.consultorioId}")
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
        log.debug("Definindo horários disponíveis de $inicio até $fim")
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
        log.debug("Removendo horários de $inicio até $fim para Prof $profissionalId")

        val inicioInstant = inicio.toInstant(fusoHorarioPadrao)
        val fimInstant = fim.toInstant(fusoHorarioPadrao)

        val consultasAgendadas = consultaRepository.buscarPorProfissionalId(profissionalId)
            .filter {
                it.statusConsulta == StatusConsulta.AGENDADA && it.dataHoraConsulta != null
            }

        val conflitos = consultasAgendadas.any { consulta ->
            val consultaInicio = consulta.dataHoraConsulta!!
            val consultaFim = consulta.horarioFim()!!

            val haSobreposicao = inicioInstant < consultaFim && fimInstant > consultaInicio
            if(haSobreposicao) {
                log.warn("Bloqueio de $inicio-$fim falhou. Conflito com Consulta ${consulta.idConsulta}")
            }
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
        log.debug("Horários removidos com sucesso.")
    }

    suspend fun definirDiaDeFolga(profissional: Profissional, diaDeFolga: LocalDate, usuarioLogado: User) {
        log.info("User ${usuarioLogado.idUsuario} definindo folga em $diaDeFolga para Prof ${profissional.idProfissional}")

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
            log.info("Prof ${profissional.idProfissional} já não trabalha em $diaDeFolga. Nenhuma ação tomada.")
            return
        }

        blocosDeTrabalhoDoDia.forEach { bloco ->
            val inicioDoBloco: LocalDateTime = diaDeFolga.atTime(bloco.horarioInicio)
            val fimDoBloco: LocalDateTime = diaDeFolga.atTime(bloco.horarioFim)
            removerHorariosIntervalo(profissional.idProfissional, profissional.agenda, inicioDoBloco, fimDoBloco)
        }
        log.info("Horários de folga removidos da agenda do Prof ${profissional.idProfissional} para $diaDeFolga")
    }

    @OptIn(ExperimentalTime::class)
    fun listarHorariosDisponiveisPorLocal(
        profissional: Profissional,
        consultorioId: String
    ): List<LocalDateTime> {
        log.debug("Listando horários para Prof ${profissional.idProfissional} filtrando por Consultório $consultorioId")

        val duracao = profissional.duracaoPadraoMinutos.minutes

        val todosHorariosPossiveis = listarHorariosDisponiveis(profissional)

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

    fun listarHorariosDisponiveis(profissional: Profissional): List<LocalDateTime> {
        log.debug("Listando todos horários disponíveis para Prof ${profissional.idProfissional}")
        val agenda = profissional.agenda
        val duracao = profissional.duracaoPadraoMinutos.minutes

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
        log.debug("Obtendo status da agenda para Prof ${profissional.idProfissional} de $dataInicio até $dataFim")
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