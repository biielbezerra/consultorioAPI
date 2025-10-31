package com.consultorioAPI.exceptions

class EmailBloqueadoException(message: String) : RuntimeException(message)

class PacienteInativoException(message: String, val pacienteId: String) : RuntimeException(message)

class RecursoNaoEncontradoException(message: String) : RuntimeException(message)

class NaoAutorizadoException(message: String) : RuntimeException(message)

class ConflitoDeEstadoException(message: String) : RuntimeException(message)

class InputInvalidoException(message: String) : RuntimeException(message)

