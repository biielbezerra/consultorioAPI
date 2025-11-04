package consultorioAPI.routes


import consultorioAPI.controllers.AdminController
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Routing.publicCronRoutes() {

    val adminController by inject<AdminController>()

    route("/cron") {
        val cronSecret = System.getenv("CRON_SECRET_TOKEN") ?: "fallback-secret-para-teste"

        post("/manutencao") {//documentado
            val secretHeader = call.request.header("X-Cron-Secret")

            if (secretHeader == cronSecret) {
                adminController.executarManutencao(call)
            } else {
                call.respond(HttpStatusCode.Unauthorized, "Token secreto inválido")
            }
        }
    }
}