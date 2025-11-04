package consultorioAPI.routes

import consultorioAPI.controllers.UsuarioController
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Routing.usuarioRoutes() {

    val usuarioController by inject<UsuarioController>()

    authenticate("auth-firebase-user") {


        route("/usuarios/me"){

            get{//documentado
                usuarioController.buscarMeuPerfil(call)
            }

            put {//documentado
                usuarioController.atualizarMeuPerfil(call)
            }

            delete {//documentado
                usuarioController.deletarMinhaConta(call)
            }

            post("/seguranca") {//documentado
                usuarioController.atualizarMinhaSenha(call)
            }

        }
    }
}