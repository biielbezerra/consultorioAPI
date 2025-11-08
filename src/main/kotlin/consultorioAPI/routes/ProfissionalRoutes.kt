package consultorioAPI.routes

import consultorioAPI.controllers.ConsultaController
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
            get("/horarios-disponiveis") {//documentado
                profissionalController.listarHorariosDisponiveis(call)
            }

            //Horários por consultório
            get("/consultorio/{consultorioId}/horarios-disponiveis") {//documentado
                profissionalController.listarHorariosPorLocal(call)
            }

            // GET /profissionais/{id}/consultas
            // Lista as consultas do profissional (para ele mesmo ou staff)
            get("/consultas") {//documentado
                consultaController.listarConsultasProfissional(call)
            }

            // GET /profissionais/{id}/agenda/status?dataInicio=...&dataFim=...
            // Visualiza a agenda (Ocupado, Disponível...)
            get("/agenda/status") {//documentado
                profissionalController.obterStatusAgenda(call)
            }

            // PUT /profissionais/{id}/valor-consulta
            // Atualiza o valor base da consulta
            put("/valor-consulta") {//documentado
                profissionalController.atualizarValorConsulta(call)
            }

            put("/duracao-consulta") {
                profissionalController.atualizarDuracaoConsulta(call)
            }

            // PUT /profissionais/{id}/agenda-config
            // Define os dias de trabalho (HorarioTrabalho)
            put("/agenda-config") {//documentado
                profissionalController.configurarAgenda(call)
            }

            // POST /profissionais/{id}/agenda/folga
            // Define um dia de folga
            post("/agenda/folga") {//documentado
                profissionalController.definirFolga(call)
            }

            //Abre slot em horários que não estão definidos como horário de trabalho
            post("/agenda/horario-extra") {
                profissionalController.definirHorarioExtra(call)
            }

            // Rota para BLOQUEAR um horário
            post("/agenda/bloquear-intervalo") {
                profissionalController.bloquearIntervalo(call)
            }

            // GET /profissionais/{id}/promocoes-disponiveis
            // Lista promoções que ele pode ativar
            get("/promocoes-disponiveis") {//documentado
                profissionalController.listarPromocoesDisponiveis(call)
            }

            // POST /profissionais/{id}/promocoes/{promocaoId}/ativar
            // Ativa uma promoção
            post("/promocoes/{promocaoId}/ativar") {//documentado
                profissionalController.ativarPromocao(call)
            }

            // DELETE /profissionais/{id}/promocoes/{promocaoId}/desativar
            // Desativa uma promoção
            delete("/promocoes/{promocaoId}/desativar") {//documentado
                profissionalController.desativarPromocao(call)
            }

            post("/promocoes") {
                profissionalController.criarPromocaoPropria(call)
            }

            delete("/promocoes/{promocaoId}") {
                profissionalController.deletarPromocaoPropria(call)
            }
        }
    }
}