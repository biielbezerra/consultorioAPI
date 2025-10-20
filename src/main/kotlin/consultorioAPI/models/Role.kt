package com.consultorioAPI.models

import kotlinx.serialization.Serializable

@Serializable
enum class Role {
    SUPER_ADMIN,
    RECEPCIONISTA,
    PROFISSIONAL,
    PACIENTE
}