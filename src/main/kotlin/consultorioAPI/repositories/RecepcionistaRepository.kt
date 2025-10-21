package com.consultorioAPI.repositories

import com.consultorioAPI.models.Recepcionista

interface RecepcionistaRepository {

    suspend fun salvar(recepcionista: Recepcionista): Recepcionista

    suspend fun atualizar(recepcionista: Recepcionista): Recepcionista

    suspend fun buscarPorId(id: String): Recepcionista?

    suspend fun buscarPorUserId(userId: String): Recepcionista?

    suspend fun buscarPorNome(nome: String): List<Recepcionista>

    suspend fun listarTodos(): List<Recepcionista>

    suspend fun buscarPorToken(token: String): Recepcionista?

    suspend fun deletarPorId(id: String)

}