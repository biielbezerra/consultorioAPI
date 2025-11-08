package consultorioAPI.mappers

import com.consultorioAPI.models.*
import consultorioAPI.dtos.*
import com.consultorioAPI.repositories.*
import kotlin.time.ExperimentalTime

fun AreaAtuacao.toResponse(): AreaAtuacaoResponse {
    return AreaAtuacaoResponse(
        idArea = this.idArea,
        nome = this.nome,
        nomeRegistro = this.nomeRegistro
    )
}

fun Consultorio.toResponse(): ConsultorioResponse {
    return ConsultorioResponse(
        idConsultorio = this.idConsultorio,
        nome = this.nomeConsultorio,
        endereco = this.endereco
    )
}

suspend fun Paciente.toResponse(userRepository: UserRepository): PacienteResponse {
    val user = userRepository.buscarPorId(this.userId, incluirDeletados = true)
        ?: throw IllegalStateException("Dados de usuário (User) não encontrados para o Paciente ${this.idPaciente}")

    return PacienteResponse(
        idPaciente = this.idPaciente,
        userId = this.userId,
        email = user.email, // <-- Dado aninhado
        nomePaciente = this.nomePaciente,
        telefone = this.telefone,
        status = this.status
    )
}

suspend fun Profissional.toResponse(
    userRepository: UserRepository,
    areaAtuacaoRepository: AreaAtuacaoRepository
): ProfissionalResponse {
    val user = userRepository.buscarPorId(this.userId, incluirDeletados = true)
        ?: throw IllegalStateException("Dados de usuário (User) não encontrados para o Profissional ${this.idProfissional}")

    val area = areaAtuacaoRepository.buscarPorId(this.areaAtuacaoId)
        ?: throw IllegalStateException("Área de Atuação ${this.areaAtuacaoId} não encontrada para o Profissional ${this.idProfissional}")

    return ProfissionalResponse(
        idProfissional = this.idProfissional,
        userId = this.userId,
        email = user.email,
        nomeProfissional = this.nomeProfissional,
        telefone = this.telefone,
        valorBaseConsulta = this.valorBaseConsulta,
        duracaoPadraoMinutos = this.duracaoPadraoMinutos,
        areaAtuacao = area.toResponse(),
        atributosEspecificos = this.atributosEspecificos,
        status = this.status
    )
}

suspend fun Recepcionista.toResponse(userRepository: UserRepository): RecepcionistaResponse {
    val user = userRepository.buscarPorId(this.userId, incluirDeletados = true)
        ?: throw IllegalStateException("Dados de usuário (User) não encontrados para a Recepcionista ${this.idRecepcionista}")

    return RecepcionistaResponse(
        idRecepcionista = this.idRecepcionista,
        userId = this.userId,
        email = user.email,
        nomeRecepcionista = this.nomeRecepcionista,
        telefone = this.telefone,
        status = this.status
    )
}

@OptIn(ExperimentalTime::class)
suspend fun Consulta.toResponse(
    consultorioRepository: ConsultorioRepository,
    areaAtuacaoRepository: AreaAtuacaoRepository
): ConsultaResponse {

    val consultorio = this.consultorioId?.let { consultorioRepository.buscarPorId(it)?.toResponse() }

    val area = areaAtuacaoRepository.buscarPorId(this.area)?.toResponse()
        ?: throw IllegalStateException("Área de Atuação ${this.area} não encontrada para a Consulta ${this.idConsulta}")

    return ConsultaResponse(
        idConsulta = this.idConsulta,
        dataHoraConsulta = this.dataHoraConsulta,
        statusConsulta = this.statusConsulta,
        duracaoEmMinutos = this.duracaoEmMinutos,
        valorConsulta = this.valorConsulta,
        nomePaciente = this.nomePaciente,
        nomeProfissional = this.nomeProfissional,
        consultorio = consultorio,
        areaAtuacao = area
    )
}

@OptIn(ExperimentalTime::class)
fun Promocao.toResponse(): PromocaoResponse {
    return PromocaoResponse(
        idPromocao = this.idPromocao,
        descricao = this.descricao,
        percentualDesconto = this.percentualDesconto,
        dataInicio = this.dataInicio,
        dataFim = this.dataFim,
        tipoPromocao = this.tipoPromocao,
        profissionalIdAplicavel = this.profissionalIdAplicavel,
        escopo = this.escopo
    )
}