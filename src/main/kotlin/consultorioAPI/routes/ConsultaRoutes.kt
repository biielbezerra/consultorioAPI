package consultorioAPI.routes

import com.consultorioAPI.controllers.ConsultaController
import io.ktor.server.routing.*
import io.ktor.server.auth.*
import org.koin.ktor.ext.inject

fun Routing.consultaRoutes() {

    val consultaController by inject<ConsultaController>()

    authenticate("auth-firebase-user") {

        route("/consultas") {

            post {
                consultaController.agendarConsulta(call)
            }

            // POST /consultas/dupla
            post("/dupla") {
                consultaController.agendarConsultaDupla(call)
            }

            post("/pacote") {
                consultaController.agendarPacote(call)
            }

            // PUT /consultas/{id}/reagendar
            put("/{id}/reagendar") {
                consultaController.reagendarConsulta(call)
            }

            // POST /consultas/{id}/cancelar
            post("/{id}/cancelar") {
                consultaController.cancelarConsulta(call)
            }

            // POST /consultas/{id}/finalizar
            post("/{id}/finalizar") {
                consultaController.finalizarConsulta(call)
            }
        }
    }
}