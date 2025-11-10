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
import org.slf4j.LoggerFactory

class SupabasePromocaoRepository : PromocaoRepository {

    private val log = LoggerFactory.getLogger(javaClass)
    private val client = SupabaseConfig.client
    private val table = client.postgrest["Promocao"]

    override suspend fun salvar(promocao: Promocao): Promocao {
        log.debug("Salvando Promocao: ${promocao.idPromocao}")
        return table.upsert(promocao) { select() }.decodeSingle()
    }

    override suspend fun atualizar(promocao: Promocao): Promocao {
        log.debug("Atualizando Promocao: ${promocao.idPromocao}")
        return table.update(promocao) {
            select()
            filter { eq("idPromocao", promocao.idPromocao) }
        }.decodeSingle()
    }

    override suspend fun buscarPorId(id: String, incluirDeletados: Boolean): Promocao? {
        log.debug("Buscando Promocao por idPromocao: $id")
        return try {
            val response = table.select {
                filter {
                    eq("idPromocao", id)
                    if (!incluirDeletados) { eq("isDeletado", false) }
                }
                limit(1)
            }
            response.decodeList<Promocao>().firstOrNull()
        } catch (e: Exception) {
            log.error("FALHA NA DECODIFICAÇÃO (buscarPorId): ${e.message}", e)
            null
        }
    }

    override suspend fun listarTodas(incluirDeletados: Boolean): List<Promocao> {
        log.debug("Listando todas as promoções")
        return table.select {
            if (!incluirDeletados) { filter { eq("isDeletado", false) } }
        }.decodeList()
    }


    @OptIn(ExperimentalTime::class)
    override suspend fun buscarAtivasPorData(data: Instant): List<Promocao> {
        log.debug("Buscando promoções ativas por data: $data")
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
        log.debug("Buscando promoção ativa por código: $codigo")
        return try {
            val response = table.select {
                filter {
                    eq("isAtiva", true)
                    eq("isDeletado", false)
                    lte("dataInicio", data.toString())
                    gte("dataFim", data.toString())
                    eq("tipoPromocao", TipoPromocao.CODIGO.name)
                    eq("codigoOpcional", codigo.trim().uppercase())
                }
                limit(1)
            }
            response.decodeList<Promocao>().firstOrNull()
        } catch (e: Exception) {
            println("DEBUG [PromocaoRepo] - FALHA NA DECODIFICAÇÃO (buscarAtivasPorCodigo): ${e.message}")
            e.printStackTrace()
            null
        }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun buscarAtivasPorTipo(tipo: TipoPromocao, data: Instant): List<Promocao> {
        log.debug("Buscando promoções ativas por tipo: $tipo")
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