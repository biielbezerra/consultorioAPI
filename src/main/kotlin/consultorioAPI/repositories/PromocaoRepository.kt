package com.consultorioAPI.repositories

import com.consultorioAPI.models.Profissional
import com.consultorioAPI.models.Promocao
import com.consultorioAPI.models.PromocaoEscopo
import com.consultorioAPI.models.TipoPromocao
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
interface PromocaoRepository {
    suspend fun salvar(promocao: Promocao): Promocao

    suspend fun atualizar(promocao: Promocao): Promocao

    suspend fun buscarPorId(id: String, incluirDeletados: Boolean = false): Promocao?

    suspend fun listarTodas(incluirDeletados: Boolean = false): List<Promocao>

    suspend fun buscarAtivasPorData(data: Instant): List<Promocao>

    suspend fun buscarAtivasPorCodigo(codigo: String, data: Instant): Promocao?

    suspend fun buscarAtivasPorTipo(tipo: TipoPromocao, data: Instant): List<Promocao>

}