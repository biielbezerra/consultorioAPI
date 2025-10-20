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

class SupabaseConsultaRepository : ConsultaRepository {

    private val client = SupabaseConfig.client
    private val table = client.postgrest["Consulta"]

    override suspend fun salvar(consulta: Consulta): Consulta {
        return table.upsert(consulta) {
            select()
        }.decodeSingle()
    }

    override suspend fun atualizar(consulta: Consulta): Consulta {
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
        return table.select {
            filter {
                eq("idConsulta", id)
            }
        }.decodeAsOrNull<Consulta>()
    }

    override suspend fun buscarPorPacienteId(pacienteId: String): List<Consulta> {
        return table.select {
            filter {
                eq("pacienteID", pacienteId)
            }
            order("dataHoraConsulta", Order.DESCENDING)
        }.decodeList()
    }

    override suspend fun buscarPorProfissionalId(profissionalId: String): List<Consulta> {
        return table.select {
            filter {
                eq("profissionalID", profissionalId)
            }
            order("dataHoraConsulta", Order.ASCENDING)
        }.decodeList()
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun buscarPorIntervaloDeDatas(inicio: LocalDateTime, fim: LocalDateTime): List<Consulta> {
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
}