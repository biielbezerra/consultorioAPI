package com.nutriAPI.models

import java.time.LocalDateTime
import java.util.UUID

class Paciente(val idPaciente: String = UUID.randomUUID().toString(),
               val nomePaciente: String,
               var email: String,
               var senha: String,
               val consultasPaciente: MutableList<Consulta>,
               val dataCadastro: LocalDateTime) {

    fun preAgendarConsulta(quantidade: Int){
        TODO()
    }

    fun agendarConsulta(consulta: Consulta) {
        TODO()
    }

    fun listarConsultas(): List<Consulta> {
        return consultasPaciente
    }

    fun isClienteFiel(){
        TODO()
    }

    fun temDesconto(): Boolean{
        if(TODO()){
            TODO()
        }
        return TODO()
    }

}