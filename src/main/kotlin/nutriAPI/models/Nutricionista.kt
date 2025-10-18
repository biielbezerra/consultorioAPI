package com.nutriAPI.models

import java.time.LocalDateTime
import java.util.UUID

class Nutricionista(
    nomeNutri: String,
    email: String,
    senha: String,
    agenda: Agenda,
    diasDeTrabalho: List<HorarioTrabalho>,
    val CRN: String? = null
) : Profissional(
    nomeProfissional = nomeNutri,
    email = email,
    senha = senha,
    areaAtuacao = "Nutrição",
    agenda = agenda,
    diasDeTrabalho = diasDeTrabalho
) {

}