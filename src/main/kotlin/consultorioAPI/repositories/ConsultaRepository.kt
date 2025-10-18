package com.consultorioAPI.repositories

import com.consultorioAPI.models.Consulta
import java.time.LocalDateTime

interface ConsultaRepository {

    fun salvar(consulta: Consulta): Consulta

    fun buscarPorId(id: String): Consulta?

    fun buscarPorPacienteId(pacienteId: String): List<Consulta>

    fun buscarPorProfissionalId(profissionalId: String): List<Consulta>

    fun buscarPorIntervaloDeDatas(inicio: LocalDateTime, fim: LocalDateTime): List<Consulta>

    fun atualizar(consulta: Consulta): Consulta

}