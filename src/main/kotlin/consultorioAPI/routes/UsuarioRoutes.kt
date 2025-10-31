package consultorioAPI.routes

import consultorioAPI.controllers.UsuarioController
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Routing.usuarioRoutes() {

    val usuarioController by inject<UsuarioController>()

    authenticate("auth-firebase-user") {

        get("/usuarios/me") {

            usuarioController.buscarMeuPerfil(call)
        }

        put {
            usuarioController.atualizarMeuPerfil(call)
        }

        post("/seguranca") {
            usuarioController.atualizarMinhaSenha(call)
        }

        delete {
            usuarioController.deletarMinhaConta(call)
        }
    }
}