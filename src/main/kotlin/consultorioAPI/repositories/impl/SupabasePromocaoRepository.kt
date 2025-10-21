package com.consultorioAPI.repositories.impl

import com.consultorioAPI.config.SupabaseConfig
import com.consultorioAPI.models.Promocao
import com.consultorioAPI.models.TipoPromocao
import com.consultorioAPI.repositories.PromocaoRepository
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.*
import io.github.jan.supabase.postgrest.result.*
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class SupabasePromocaoRepository : PromocaoRepository {

    private val client = SupabaseConfig.client
    private val table = client.postgrest["Promocao"]

    override suspend fun salvar(promocao: Promocao): Promocao {
        return table.upsert(promocao) { select() }.decodeSingle()
    }

    override suspend fun atualizar(promocao: Promocao): Promocao {
        return table.update(promocao) {
            select()
            filter { eq("idPromocao", promocao.idPromocao) }
        }.decodeSingle()
    }

    override suspend fun buscarPorId(id: String, incluirDeletados: Boolean): Promocao? {
        return table.select {
            filter {
                eq("idPromocao", id)
                if (!incluirDeletados) { eq("isDeletado", false) }
            }
        }.decodeAsOrNull<Promocao>()
    }

    override suspend fun listarTodas(incluirDeletados: Boolean): List<Promocao> {
        return table.select {
            if (!incluirDeletados) { filter { eq("isDeletado", false) } }
        }.decodeList()
    }


    @OptIn(ExperimentalTime::class)
    override suspend fun buscarAtivasPorData(data: Instant): List<Promocao> {
        return table.select {
            filter {
                eq("isAtiva", true)
                eq("isDeletado", false)
                lte("dataInicio", data.toString())
                gte("dataFim", data.toString())
            }
        }.decodeList()
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun buscarAtivasPorCodigo(codigo: String, data: Instant): Promocao? {
        return table.select {
            filter {
                eq("isAtiva", true)
                eq("isDeletado", false)
                lte("dataInicio", data.toString())
                gte("dataFim", data.toString())
                eq("tipoPromocao", TipoPromocao.CODIGO.name)
                eq("codigoOpcional", codigo.trim().uppercase())
            }
        }.decodeSingleOrNull<Promocao>()
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun buscarAtivasPorTipo(tipo: TipoPromocao, data: Instant): List<Promocao> {
        return table.select {
            filter {
                eq("isAtiva", true)
                eq("isDeletado", false)
                lte("dataInicio", data.toString())
                gte("dataFim", data.toString())
                eq("tipoPromocao", tipo.name)
            }
        }.decodeList()
    }
}