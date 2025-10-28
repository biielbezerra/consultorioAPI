package consultorioAPI.routes

import consultorioAPI.controllers.AdminController
import consultorioAPI.controllers.UsuarioController
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Routing.adminRoutes() {

    val adminController by inject<AdminController>()
    val usuarioController by inject<UsuarioController>()

    authenticate("auth-firebase-user") {

        route("/admin") {

            // Rotas do AdminController
            post("/promocoes") {
                adminController.criarPromocao(call)
            }
            post("/consultorios") {
                adminController.criarConsultorio(call)
            }
            post("/manutencao") {
                adminController.executarManutencao(call)
            }

            // Rotas do UsuarioController (seção de admin)
            post("/usuarios/equipe") {
                usuarioController.preCadastrarEquipe(call)
            }
            put("/usuarios/{id}/status") {
                usuarioController.atualizarStatusEquipe(call)
            }
            post("/usuarios/{id}/reenviar-convite") {
                usuarioController.reenviarConvite(call)
            }
            delete("/usuarios/{id}") {
                usuarioController.deletarUsuario(call)
            }
            post("/emails/desbloquear") {
                usuarioController.desbloquearEmail(call)
            }
            get("/emails/bloqueados") {
                usuarioController.listarEmailsBloqueados(call)
            }
        }
    }
}