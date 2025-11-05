package consultorioAPI.config

import com.consultorioAPI.repositories.*
import com.consultorioAPI.repositories.impl.*
import com.consultorioAPI.services.*
import com.consultorioAPI.controllers.ConsultaController
import consultorioAPI.controllers.AdminController
import consultorioAPI.controllers.ProfissionalController
import consultorioAPI.controllers.UsuarioController
import org.koin.dsl.module

val repositoryModule = module {
    single<AreaAtuacaoRepository> { SupabaseAreaAtuacaoRepository() }
    single<ConsultaRepository> { SupabaseConsultaRepository() }
    single<ConsultorioRepository> { SupabaseConsultorioRepository() }
    single<EmailBlocklistRepository> { SupabaseEmailBlocklistRepository() }
    single<PacienteRepository> { SupabasePacienteRepository() }
    single<ProfissionalRepository> { SupabaseProfissionalRepository() }
    single<PromocaoRepository> { SupabasePromocaoRepository() }
    single<RecepcionistaRepository> { SupabaseRecepcionistaRepository() }
    single<UserRepository> { SupabaseUserRepository() }
}

val serviceModule = module {
    single { PacienteService(get<ConsultaRepository>()) }
    single { AgendaService(get<ProfissionalRepository>(), get<ConsultaRepository>()) }

    single { PromocaoService(get<PromocaoRepository>(), get<ConsultaRepository>(), get<PacienteService>(), get<ProfissionalRepository>()) }
    single { ProfissionalService(get<ProfissionalRepository>(), get<PromocaoRepository>(), get<AgendaService>()) }
    single { ConsultaService(get<PacienteService>(), get<ConsultaRepository>(), get<PacienteRepository>(), get<ProfissionalRepository>(), get<PromocaoService>()) }

    single<UsuarioService> {
        UsuarioService(
            get<UserRepository>(),
            get<PacienteRepository>(),
            get<ProfissionalRepository>(),
            get<RecepcionistaRepository>(),
            get<AgendaService>(),
            get<AreaAtuacaoRepository>(),
            get<EmailBlocklistRepository>(),
            get<ConsultaRepository>()
        )
    }

    single { ManutencaoService(get<PacienteRepository>(), get<ProfissionalRepository>(), get<PacienteService>(), get<AgendaService>()) }

    single {
        ConsultorioService(
            get<ConsultorioRepository>(),
            get<ConsultaRepository>(),
            get<PacienteService>(),
            get<AgendaService>(),
            get<PacienteRepository>(),
            get<ProfissionalRepository>(),
            get<PromocaoService>(),
            get<EmailBlocklistRepository>(),
            get<UserRepository>(),
            get<UsuarioService>()
        )
    }
}

val controllerModule = module {

    single { UsuarioController(get<UsuarioService>()) }

    single {
        ConsultaController(
            get<ConsultorioService>(),
            get<ConsultaService>(),
            get<ProfissionalRepository>(),
            get<ConsultaRepository>(),
            get<PacienteRepository>()
        )
    }

    single {
        ProfissionalController(
            get<ProfissionalService>(),
            get<AgendaService>(),
            get<ProfissionalRepository>(),
            get<PromocaoService>()
        )
    }

    single {
        AdminController(
            get<PromocaoService>(),
            get<ConsultorioService>(),
            get<ManutencaoService>(),
            get<UsuarioService>(),
            get<ProfissionalService>()
        )
    }
}

val appModule = module {
    includes(repositoryModule, serviceModule, controllerModule)
}