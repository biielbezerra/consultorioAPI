package com.consultorioAPI.repositories

interface EmailBlocklistRepository {
    suspend fun salvar(email: String)
    suspend fun buscarPorEmail(email: String): String?
    suspend fun deletarPorEmail(email: String)
    suspend fun listarTodos(): List<String>
}