package com.nutriAPI.models

import java.time.LocalDateTime
import java.util.UUID

class Nutricionista(val idNutri: String = UUID.randomUUID().toString(),
                    val nomeNutri: String,
                    var email: String,
                    var senha: String,
                    var agenda: Agenda) {

    fun listarHorariosDisponiveis(){
        TODO()
    }

    fun bloquearHorario(){
        TODO()
    }

    fun liberarHorario(){
        TODO()
    }

    fun agendarConsulta(nomePaciente:String,
                        emailPaciente:String,
                        dataHora: LocalDateTime,
                        valor:Double,
                        isAvulso:Boolean = true){
        TODO()
    }

}