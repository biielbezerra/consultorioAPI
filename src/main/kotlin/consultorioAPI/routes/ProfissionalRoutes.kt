package consultorioAPI.routes

import com.consultorioAPI.controllers.ConsultaController
import consultorioAPI.controllers.ProfissionalController
import io.ktor.server.routing.*
import io.ktor.server.auth.*
import org.koin.ktor.ext.inject

fun Routing.profissionalRoutes() {

    val profissionalController by inject<ProfissionalController>()
    val consultaController by inject<ConsultaController>()

    authenticate("auth-firebase-user") {
        route("/profissionais/{id}") {

            //Horários gerais sem filtro
            get("/horarios-disponiveis") {
                profissionalController.listarHorariosDisponiveis(call)
            }

            //Horários por consultório
            get("/consultorio/{consultorioId}/horarios-disponiveis") {
                profissionalController.listarHorariosPorLocal(call)
            }

            // GET /profissionais/{id}/consultas
            // Lista as consultas do profissional (para ele mesmo ou staff)
            get("/consultas") {
                consultaController.listarConsultasProfissional(call)
            }

            // GET /profissionais/{id}/agenda/status?dataInicio=...&dataFim=...
            // Visualiza a agenda (Ocupado, Disponível...)
            get("/agenda/status") {
                profissionalController.obterStatusAgenda(call)
            }

            // PUT /profissionais/{id}/valor-consulta
            // Atualiza o valor base da consulta
            put("/valor-consulta") {
                profissionalController.atualizarValorConsulta(call)
            }

            // PUT /profissionais/{id}/agenda-config
            // Define os dias de trabalho (HorarioTrabalho)
            put("/agenda-config") {
                profissionalController.configurarAgenda(call)
            }

            // POST /profissionais/{id}/agenda/folga
            // Define um dia de folga
            post("/agenda/folga") {
                profissionalController.definirFolga(call)
            }

            // GET /profissionais/{id}/promocoes-disponiveis
            // Lista promoções que ele pode ativar
            get("/promocoes-disponiveis") {
                profissionalController.listarPromocoesDisponiveis(call)
            }

            // POST /profissionais/{id}/promocoes/{promocaoId}/ativar
            // Ativa uma promoção
            post("/promocoes/{promocaoId}/ativar") {
                profissionalController.ativarPromocao(call)
            }

            // DELETE /profissionais/{id}/promocoes/{promocaoId}/desativar
            // Desativa uma promoção
            delete("/promocoes/{promocaoId}/desativar") {
                profissionalController.desativarPromocao(call)
            }
        }
    }
}