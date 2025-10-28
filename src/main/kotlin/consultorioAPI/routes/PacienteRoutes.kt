package com.consultorioAPI.routes

import com.consultorioAPI.controllers.ConsultaController
import io.ktor.server.routing.*
import io.ktor.server.auth.*
import org.koin.ktor.ext.inject

fun Routing.pacienteRoutes() {

    val consultaController by inject<ConsultaController>()

    authenticate("auth-firebase-user") {

        route("/pacientes/{id}") {

            // GET /pacientes/{id}/consultas
            get("/consultas") {
                consultaController.listarConsultasPaciente(call)
            }

            // TODO: Adicionar outras rotas específicas do paciente
            // Ex: GET /pacientes/{id}/perfil
            //     PUT /pacientes/{id}/perfil
        }
    }
}