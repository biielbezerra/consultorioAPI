package consultorioAPI.routes

import com.consultorioAPI.controllers.AreaAtuacaoController
import consultorioAPI.controllers.AdminController
import consultorioAPI.controllers.UsuarioController
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Routing.adminRoutes() {

    val adminController by inject<AdminController>()
    val usuarioController by inject<UsuarioController>()
    val areaAtuacaoController by inject<AreaAtuacaoController>()

    authenticate("auth-firebase-user") {

        route("/admin") {

            // Rotas do AdminController
            post("/promocoes") {//documentado
                adminController.criarPromocao(call)
            }
            delete("/promocoes/{id}") {
                adminController.deletarPromocaoAdmin(call)
            }
            post("/consultorios") {//documentado
                adminController.criarConsultorio(call)
            }

            post("/areas-atuacao") {
                areaAtuacaoController.criarAreaAtuacao(call)
            }
            get("/areas-atuacao") {
                areaAtuacaoController.listarAreasAtuacao(call)
            }

            post("/manutencao") {//documentado
                adminController.executarManutencao(call)
            }

            // Rotas do UsuarioController (seção de admin)
            post("/usuarios/equipe") {//documentado
                usuarioController.preCadastrarEquipe(call)
            }
            put("/usuarios/{id}/status") {//documentado
                usuarioController.atualizarStatusEquipe(call)
            }
            post("/usuarios/{id}/reenviar-convite") {
                usuarioController.reenviarConvite(call)
            }
            delete("/usuarios/{id}") {//documentado
                usuarioController.deletarUsuario(call)
            }
            post("/emails/desbloquear") {//documentado
                usuarioController.desbloquearEmail(call)
            }
            get("/emails/bloqueados") {//documentado
                usuarioController.listarEmailsBloqueados(call)
            }
            post("/usuarios/linkar-perfil") {//documentado
                adminController.linkarPerfil(call)
            }
            post("/transferir-propriedade") {//documentado
                adminController.transferirSuperAdmin(call)
            }
            get("/profissionais") {//documentado
                adminController.listarProfissionais(call)
            }
            get("/pacientes") {
                adminController.listarPacientes(call)
            }
            get("/promocoes") {
                adminController.listarTodasPromocoes(call)
            }
        }
    }
}