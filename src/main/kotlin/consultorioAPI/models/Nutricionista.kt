package com.consultorioAPI.models

class Nutricionista(
    nomeNutri: String,
    userId: String,
    agenda: Agenda,
    val CRN: String? = null
) : Profissional(
    nomeProfissional = nomeNutri,
    userId = userId,
    areaAtuacao = "Nutrição",
    agenda = agenda,
    diasDeTrabalho = emptyList(),
    status = StatusUsuario.CONVIDADO
) {

}