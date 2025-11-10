# consultorioAPI

API RESTful completa para um sistema de gerenciamento de consultório, construída em Kotlin com Ktor.

---

## 🚀 Sobre o Projeto

Esta é uma API RESTful completa para um sistema de gerenciamento de consultório, construída em Kotlin com Ktor. O projeto serve como um estudo de caso para arquitetura de backend moderna, incluindo:

* Autenticação via **Firebase Admin SDK**.
* Banco de dados **Supabase (PostgreSQL)**.
* Injeção de Dependência com **Koin**.
* Arquitetura limpa (Controller-Service-Repository).
* Mapeamento de **DTOs de Requisição e Resposta** (ViewModels).
* Gerenciamento de erros com **StatusPages**.
* Logging estruturado com **Logback/SLF4J**.

---

## 💻 Tech Stack

* **Linguagem:** Kotlin
* **Framework:** Ktor 3.3.0
* **Autenticação:** Firebase Admin SDK 9.3.0
* **Banco de Dados:** Supabase (PostgreSQL) com `supabase-kt` 3.2.5
* **Injeção de Dependência:** Koin
* **Serialização:** kotlinx.serialization
* **Logs:** SLF4J & Logback
* **Configuração:** Dotenv-kotlin

---

## 🛠️ Configuração e Instalação

Para rodar este projeto localmente, siga estes passos:

1.  **Clone o repositório:**
    ```bash
    git clone [URL_DO_SEU_REPOSITORIO]
    cd consultorioAPI
    ```

2.  **Crie o arquivo `.env`:**
    Na raiz do projeto, crie um arquivo chamado `.env`. Este arquivo é **obrigatório** para carregar as chaves da API.

3.  **Adicione as variáveis de ambiente:**
    Copie e cole o seguinte conteúdo no seu `.env` e substitua pelos seus valores reais:

    ```ini
    # Configuração do Supabase (Banco de Dados)
    SUPABASE_URL=https://[SEU_PROJETO].supabase.co
    SUPABASE_SERVICE_KEY=[SUA_CHAVE_SERVICE_ROLE]

    # Configuração do Firebase (Autenticação)
    FIREBASE_PROJECT_ID=[SEU_PROJECT_ID_DO_FIREBASE]
    FIREBASE_SERVICE_ACCOUNT_PATH=src/main/resources/firebase-service-account.json
    ```
    *Nota: O `FIREBASE_SERVICE_ACCOUNT_PATH` aponta para o arquivo JSON (`firebase-service-account.json`) que você baixou do Google Cloud/Firebase.*

4.  **Rode a aplicação:**
    Execute a função `main` no arquivo `Application.kt` pela sua IDE (IntelliJ). A API estará disponível em `http://localhost:8080`.

5.  **Acesse a Documentação (Swagger):**
    Com a API rodando, acesse `http://localhost:8080/swagger`. Esta é a documentação interativa e a **melhor forma de testar as rotas**.

---

## 🌊 Principais Fluxos da API

Esta API utiliza um sistema de autenticação robusto e controle de acesso baseado em papéis (RBAC).

### 1. Autenticação (Firebase JWT)

A API **não** lida com senhas. Ela é um "serviço de recursos" que espera um **Token JWT (Bearer Token)** válido gerado pelo Firebase Auth.

1.  Um aplicativo cliente (mobile ou web) faz login no Firebase (ex: com email/senha ou Google Sign-In).
2.  O Firebase retorna um `idToken` (JWT).
3.  O cliente envia esse `idToken` no header `Authorization: Bearer [idToken]` para **todas** as rotas protegidas da API.
4.  A API (em `SecurityConfig.kt`) valida esse token com o Firebase.
5.  Se o token for válido, a API busca o `User` correspondente no banco de dados Supabase. Se o `User` existir e estiver `ATIVO`, a requisição é autorizada.

### 2. Papéis de Usuário (RBAC)

O sistema possui 4 papéis (`Role`) que definem as permissões:
* **`SUPER_ADMIN`:** Controle total. Pode criar outros admins, gerenciar consultórios e ver tudo.
* **`RECEPCIONISTA`:** Papel de staff. Pode agendar para pacientes, gerenciar pacientes, criar áreas de atuação.
* **`PROFISSIONAL`:** Papel de staff. Pode gerenciar a *própria* agenda, promoções e consultas.
* **`PACIENTE`:** Papel de cliente. Só pode gerenciar as *próprias* consultas e perfil.

### 3. Onboarding de Usuários

Existem dois fluxos principais para um usuário entrar no sistema:

* **Fluxo de Paciente (Público):**
    1.  O paciente usa a rota pública `POST /auth/register/paciente`.
    2.  O `UsuarioService` cria o usuário no Firebase Auth (com senha) e, em seguida, cria as entradas `User` e `Paciente` no Supabase.

* **Fluxo de Staff (Convite):**
    1.  Um `SUPER_ADMIN` chama `POST /admin/usuarios/equipe` com o email e o `role` (ex: `PROFISSIONAL`).
    2.  O `UsuarioService` cria o usuário no Firebase (sem senha) e cria as entradas `User` e `Profissional` (com status `CONVIDADO`) no Supabase.
    3.  O usuário recebe um email (TODO) com um link de convite.
    4.  O usuário clica no link e chama `POST /auth/completar-cadastro` para definir sua senha e mudar seu status para `ATIVO`.

---

## 🏛️ Arquitetura: Models vs. DTOs de Resposta

Este projeto separa estritamente os *Models* (como `Consulta.kt`) dos *DTOs de Resposta* (como `ConsultaResponse`).

* **Models (`models/`):** Representam a *estrutura exata* do banco de dados (PostgreSQL). Eles são "crus" e contêm IDs (ex: `profissionalID`, `consultorioId`).
* **Response DTOs (`dtos/ResponseDtos.kt`):** Representam o JSON "limpo" que é enviado ao frontend.

**Por quê?** Para eficiência. Em vez de o frontend receber uma `Consulta` com 5 IDs e ter que fazer 5 novas chamadas para buscar os nomes, a API faz esse trabalho. Os `Mappers` (`mappers/Mappers.kt`) buscam os dados aninhados (como o nome do consultório e a área de atuação) e os "montam" em um `ConsultaResponse` completo.

**Exemplo de Resposta de `POST /consultas`:**
```json
{
  "idConsulta": "5eef...",
  "dataHoraConsulta": "2025-11-10T09:00:00Z",
  "statusConsulta": "AGENDADA",
  "nomePaciente": "Gabriel Bezerra",
  "nomeProfissional": "Dra. Ana",
  
  "consultorio": { 
    "idConsultorio": "2599...",
    "nome": "Consultório Principal",
    "endereco": "Rua Fictícia, 123"
  },
  "areaAtuacao": {
    "idArea": "2d24...",
    "nome": "Psicologia"
  }
}
```
## Referência da API (Rotas)

Abaixo está um resumo das rotas disponíveis:

| Rotas                                                                  | Tipo    | Descrição                                                             |
|------------------------------------------------------------------------|---------|-----------------------------------------------------------------------|
| `/auth/register/paciente`                                              | POST    | Registra um novo paciente (público)                                   |
| `/auth/completar-cadastro`                                             | POST    | Completa o cadastro de um membro da equipe (link do e-mail)           |
| `/auth/recusar-convite`                                                | POST    | Recusa um convite de equipe (link do e-mail)                          |
| `/auth/social/onboarding`                                              | POST    | (Login Social) Obtém ou cria um perfil de paciente                    |
| `/admin/promocoes`                                                     | POST    | (Admin) Cria uma promoção global ou específica                        |
| `/admin/promocoes`                                                     | GET     | (Admin) Lista todas as promoções                                      |
| `/admin/promocoes/{id}`                                                | DELETE  | (Admin) Deleta uma promoção                                           |
| `/admin/consultorios`                                                  | POST    | (Admin) Cria um novo consultório                                      |
| `/admin/areas-atuacao`                                                 | GET     | (Admin/Recep) Lista todas as Áreas de Atuação                         |
| `/admin/areas-atuacao`                                                 | POST    | (Admin/Recep) Cria uma nova Área de Atuação                           |
| `/admin/manutencao`                                                    | POST    | (Admin) Executa a rotina de manutenção diária manualmente             |
| `/admin/usuarios/equipe`                                               | POST    | (Admin) Pré-cadastra um membro da equipe (Profissional/Recepcionista) |
| `/admin/usuarios/{id}/status`                                          | PUT     | (Admin) Atualiza o status de um membro da equipe (ATIVO/INATIVO)      |
| `/admin/usuarios/{id}/reenviar-convite`                                | POST    | (Admin) Reenvia o convite para um membro da equipe                    |
| `/admin/usuarios/{id}`                                                 | DELETE  | (Admin) Deleta um usuário (Paciente ou Equipe)                        |
| `/admin/emails/desbloquear`                                            | POST    | (Admin) Desbloqueia um email da blocklist                             |
| `/admin/emails/bloqueados`                                             | GET     | (Admin) Lista todos os emails bloqueados                              |
| `/admin/usuarios/linkar-perfil`                                        | POST    | (Admin) Linka um perfil (Prof/Recep) a um usuário SuperAdmin          |
| `/admin/transferir-propriedade`                                        | POST    | (Admin) Transfere a propriedade de SuperAdmin para outro email        |
| `/admin/profissionais`                                                 | GET     | (Admin) Lista todos os profissionais ativos                           |
| `/admin/pacientes`                                                     | GET     | (Admin) Lista todos os pacientes                                      |
| `/consultas`                                                           | POST    | Agenda uma nova consulta (avulsa)                                     |
| `/consultas/dupla`                                                     | POST    | Agenda uma consulta dupla (primeira vez)                              |
| `/consultas/pacote`                                                    | POST    | Agenda um pacote de consultas (1 agendada + N créditos)               |
| `/consultas/{id}/reagendar`                                            | PUT     | Reagenda uma consulta existente                                       |
| `/consultas/{id}/cancelar`                                             | POST    | Cancela uma consulta                                                  |
| `/consultas/{id}/finalizar`                                            | POST    | Finaliza uma consulta (REALIZADA ou NAO_COMPARECEU)                   |
| `/profissionais/{id}/horarios-disponiveis`                             | GET     | Lista todos os horários disponíveis (geral)                           |
| `/profissionais/{id}/consultorio/{consultorioId}/horarios-disponiveis` | GET     | Lista horários disponíveis filtrando por consultório                  |
| `/profissionais/{id}/consultas`                                        | GET     | Lista todas as consultas de um profissional                           |
| `/profissionais/{id}/agenda/status`                                    | GET     | Obtém o status da agenda (Ocupado, Disponível, etc)                   |
| `/profissionais/{id}/valor-consulta`                                   | PUT     | Atualiza o valor base da consulta do profissional                     |
| `/profissionais/{id}/duracao-consulta`                                 | PUT     | (Admin/Recep/Prof) Atualiza a duração padrão da consulta              |
| `/profissionais/{id}/agenda-config`                                    | PUT     | Define a agenda de trabalho padrão (e gera horários futuros)          |
| `/profissionais/{id}/agenda/folga`                                     | POST    | Define um dia de folga (remove horários de trabalho daquele dia)      |
| `/profissionais/{id}/agenda/horario-extra`                             | POST    | Adiciona um intervalo de disponibilidade (horário extra)              |
| `/profissionais/{id}/agenda/bloquear-intervalo`                        | POST    | Bloqueia um intervalo de tempo (remove disponibilidade)               |
| `/profissionais/{id}/promocoes-disponiveis`                            | GET     | (Profissional) Lista promoções globais/próprias que ele pode ativar   |
| `/profissionais/{id}/promocoes/{promocaoId}/ativar`                    | POST    | (Profissional) Ativa uma promoção para sua agenda                     |
| `/profissionais/{id}/promocoes/{promocaoId}/desativar`                 | DELETE  | (Profissional) Desativa uma promoção de sua agenda                    |
| `/profissionais/{id}/promocoes`                                        | POST    | (Profissional) Cria uma promoção para si mesmo                        |
| `/profissionais/{id}/promocoes/{promocaoId}`                           | DELETE  | (Profissional) Deleta uma promoção que ele mesmo criou                |
| `/pacientes/{id}/consultas`                                            | GET     | Lista todas as consultas de um paciente                               |
| `/usuarios/me`                                                         | GET     | Busca o perfil do usuário logado (Paciente, Prof, etc)                |
| `/usuarios/me`                                                         | PUT     | Atualiza o perfil do usuário logado (nome, telefone)                  |
| `/usuarios/me`                                                         | DELETE  | (Paciente) Deleta a própria conta                                     |
| `/usuarios/me/seguranca`                                               | POST    | Atualiza a senha do usuário logado                                    |
| `/cron/manutencao`                                                     | POST    | (Cron Job) Rota pública para executar a manutenção diária             |

