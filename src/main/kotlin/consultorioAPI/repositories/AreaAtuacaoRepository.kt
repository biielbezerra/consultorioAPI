package com.consultorioAPI.repositories

import com.consultorioAPI.models.AreaAtuacao

interface AreaAtuacaoRepository {
    fun salvar(area: AreaAtuacao): AreaAtuacao
    fun buscarPorId(id: String): AreaAtuacao?
    fun listarTodas(): List<AreaAtuacao>
}