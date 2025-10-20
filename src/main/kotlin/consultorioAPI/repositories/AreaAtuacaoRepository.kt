package com.consultorioAPI.repositories

import com.consultorioAPI.models.AreaAtuacao

interface AreaAtuacaoRepository {
    suspend fun salvar(area: AreaAtuacao): AreaAtuacao
    suspend fun buscarPorId(id: String): AreaAtuacao?
    suspend fun listarTodas(): List<AreaAtuacao>
}