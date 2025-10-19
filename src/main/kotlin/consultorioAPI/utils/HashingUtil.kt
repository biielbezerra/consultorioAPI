package com.consultorioAPI.utils

object HashingUtil {
    fun hashSenha(senha: String): String{
        return "hash_simulado_de_${senha.reversed()}"
    }
}