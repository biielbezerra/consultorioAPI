package com.consultorioAPI.repositories

import com.consultorioAPI.models.Recepcionista

interface RecepcionistaRepository {

    fun salvar(recepcionista: Recepcionista): Recepcionista

    fun bucarPorId(id: String): Recepcionista?

    fun buscarPorUserId(userId: String): Recepcionista?

    fun buscarPorNome(nome: String): List<Recepcionista>

    fun listarTodos(): List<Recepcionista>

}