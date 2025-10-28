package consultorioAPI.routes

import consultorioAPI.controllers.UsuarioController
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Routing.usuarioRoutes() {

    authenticate("auth-firebase-user") {

        get("/usuarios/me") {

        }

        // TODO: Adicionar rotas que TODO usuário logado pode fazer
        // Ex: GET /usuarios/meu-perfil
    }
}