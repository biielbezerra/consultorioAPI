package consultorioAPI.routes

import com.consultorioAPI.controllers.ConsultaController
import io.ktor.server.routing.*
import io.ktor.server.auth.*
import org.koin.ktor.ext.inject

fun Routing.consultaRoutes() {

    val consultaController by inject<ConsultaController>()

    authenticate("auth-firebase-user") {

        route("/consultas") {

            post {//documentado
                consultaController.agendarConsulta(call)
            }

            // POST /consultas/dupla
            post("/dupla") {//documentado
                consultaController.agendarConsultaDupla(call)
            }

            post("/pacote") {//documentado
                consultaController.agendarPacote(call)
            }

            // PUT /consultas/{id}/reagendar
            put("/{id}/reagendar") {//documentado
                consultaController.reagendarConsulta(call)
            }

            // POST /consultas/{id}/cancelar
            post("/{id}/cancelar") {//documentado
                consultaController.cancelarConsulta(call)
            }

            // POST /consultas/{id}/finalizar
            post("/{id}/finalizar") {//documentado
                consultaController.finalizarConsulta(call)
            }
        }
    }
}