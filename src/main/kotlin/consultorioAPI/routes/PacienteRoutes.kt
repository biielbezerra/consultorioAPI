package com.consultorioAPI.routes

import consultorioAPI.controllers.ConsultaController
import io.ktor.server.routing.*
import io.ktor.server.auth.*
import org.koin.ktor.ext.inject

fun Routing.pacienteRoutes() {

    val consultaController by inject<ConsultaController>()

    authenticate("auth-firebase-user") {

        route("/pacientes/{id}") {

            // GET /pacientes/{id}/consultas
            get("/consultas") {//documentado
                consultaController.listarConsultasPaciente(call)
            }
        }
    }
}