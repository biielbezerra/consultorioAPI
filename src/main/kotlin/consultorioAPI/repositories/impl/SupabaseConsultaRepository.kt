package com.consultorioAPI.repositories.impl

import com.consultorioAPI.config.fusoHorarioPadrao
import com.consultorioAPI.config.SupabaseConfig
import com.consultorioAPI.models.Consulta
import com.consultorioAPI.repositories.ConsultaRepository
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toInstant
import kotlin.time.ExperimentalTime
import org.slf4j.LoggerFactory

class SupabaseConsultaRepository : ConsultaRepository {

    private val log = LoggerFactory.getLogger(javaClass)
    private val client = SupabaseConfig.client
    private val table = client.postgrest["Consulta"]

    override suspend fun salvar(consulta: Consulta): Consulta {
        log.debug("Salvando Consulta: ${consulta.idConsulta}")
        return table.upsert(consulta) {
            select()
        }.decodeSingle()
    }

    override suspend fun atualizar(consulta: Consulta): Consulta {
        log.debug("Atualizando Consulta: ${consulta.idConsulta}")
        return table.update(
            value = consulta
        ) {
            select()
            filter {
                eq("idConsulta", consulta.idConsulta)
            }
        }.decodeSingle()
    }

    override suspend fun buscarPorId(id: String): Consulta? {
        log.debug("Buscando Consulta por idConsulta: $id")
        return try {
            val response = table.select {
                filter {
                    eq("idConsulta", id)
                }
                limit(1)
            }
            response.decodeList<Consulta>().firstOrNull()
        } catch (e: Exception) {
            log.error("FALHA NA DECODIFICAÇÃO (buscarPorId): ${e.message}", e)
            null
        }
    }

    override suspend fun buscarPorPacienteId(pacienteId: String): List<Consulta> {
        log.debug("Buscando Consultas por pacienteId: $pacienteId")
        return table.select {
            filter {
                eq("pacienteID", pacienteId)
            }
            order("dataHoraConsulta", Order.DESCENDING)
        }.decodeList()
    }

    override suspend fun buscarPorProfissionalId(profissionalId: String): List<Consulta> {
        log.debug("Buscando Consultas por profissionalID: $profissionalId")
        return table.select {
            filter {
                eq("profissionalID", profissionalId)
            }
            order("dataHoraConsulta", Order.ASCENDING)
        }.decodeList()
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun buscarPorIntervaloDeDatas(inicio: LocalDateTime, fim: LocalDateTime): List<Consulta> {
        log.debug("Buscando Consultas por intervalo: $inicio até $fim")
        val inicioInstant = inicio.toInstant(fusoHorarioPadrao)
        val fimInstant = fim.toInstant(fusoHorarioPadrao)

        return table.select {
            filter {
                gte("dataHoraConsulta", inicioInstant.toString())
                lte("dataHoraConsulta", fimInstant.toString())
            }
            order("dataHoraConsulta", Order.ASCENDING)
        }.decodeList()
    }

    override suspend fun deletarPorId(id: String) {
        log.debug("Deletando Consulta: $id")
        table.delete { filter { eq("idConsulta", id) } }
    }
}