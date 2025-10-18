package com.nutriAPI.models

import com.nutriAPI.services.AgendaService
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

data class Consultorio(
    val idConsultorio: String = UUID.randomUUID().toString(),
    val nomeConsultorio: String,
    val endereco: String
)