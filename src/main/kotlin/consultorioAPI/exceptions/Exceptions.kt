package com.consultorioAPI.exceptions

class EmailBloqueadoException(message: String) : RuntimeException(message)

class PacienteInativoException(message: String, val pacienteId: String) : RuntimeException(message)

