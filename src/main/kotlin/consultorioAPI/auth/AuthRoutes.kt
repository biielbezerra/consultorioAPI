package com.consultorioAPI.auth

import consultorioAPI.controllers.UsuarioController
import io.ktor.server.routing.*
import io.ktor.server.auth.*
import org.koin.ktor.ext.inject

fun Routing.authRoutes() {

    val usuarioController by inject<UsuarioController>()

    route("/auth") {

        post("/register/paciente") {
            usuarioController.registrarPaciente(call)
        }

        // POST /auth/completar-cadastro
        // Usuário (Profissional/Recepcionista) clica no link do e-mail
        post("/completar-cadastro") {
            usuarioController.completarCadastro(call)
        }

        // POST /auth/recusar-convite
        post("/recusar-convite") {
            usuarioController.recusarConvite(call)
        }
    }

}