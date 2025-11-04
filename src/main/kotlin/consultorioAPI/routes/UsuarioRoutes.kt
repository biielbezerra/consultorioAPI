package consultorioAPI.routes

import consultorioAPI.controllers.UsuarioController
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Routing.usuarioRoutes() {

    val usuarioController by inject<UsuarioController>()

    authenticate("auth-firebase-user") {

        get("/usuarios/me") {//documentado

            usuarioController.buscarMeuPerfil(call)
        }

        put {//documentado
            usuarioController.atualizarMeuPerfil(call)
        }

        post("/seguranca") {//documentado
            usuarioController.atualizarMinhaSenha(call)
        }

        delete {//documentado
            usuarioController.deletarMinhaConta(call)
        }
    }
}