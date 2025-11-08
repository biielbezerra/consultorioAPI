package com.consultorioAPI.repositories

import com.consultorioAPI.models.Consulta
import kotlinx.datetime.LocalDateTime

interface ConsultaRepository {

    suspend fun salvar(consulta: Consulta): Consulta

    suspend fun buscarPorId(id: String): Consulta?

    suspend fun buscarPorPacienteId(pacienteId: String): List<Consulta>

    suspend fun buscarPorProfissionalId(profissionalId: String): List<Consulta>

    suspend fun buscarPorIntervaloDeDatas(inicio: LocalDateTime, fim: LocalDateTime): List<Consulta>

    suspend fun atualizar(consulta: Consulta): Consulta

    suspend fun deletarPorId(id: String)
}