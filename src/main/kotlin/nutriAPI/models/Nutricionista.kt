package com.nutriAPI.models

import java.time.LocalDateTime
import java.util.UUID

data class Nutricionista(val idNutri: String = UUID.randomUUID().toString(),
                    val nomeNutri: String,
                    var email: String,
                    var senha: String,
                    var agenda: Agenda) {
}