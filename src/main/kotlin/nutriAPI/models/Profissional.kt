package com.nutriAPI.models

import java.util.UUID

open class Profissional(
    val idProfissional: String = UUID.randomUUID().toString(),
    val nomeProfissional: String,
    var email: String,
    var senha: String,
    val areaAtuacao: String,
    var valorBaseConsulta: Double = 170.0,
    var agenda: Agenda
) {

    val consultasProfissional = mutableListOf<Consulta>()

    fun listarConsultas(): List<Consulta> {
        return consultasProfissional
    }

    fun setValorConsulta(profissional: Profissional, novoValor: Double) {
        this.valorBaseConsulta = novoValor
    }


}