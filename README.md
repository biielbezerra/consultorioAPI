# consultorioAPI

This project was created using the [Ktor Project Generator](https://start.ktor.io).

Here are some useful links to get you started:

- [Ktor Documentation](https://ktor.io/docs/home.html)
- [Ktor GitHub page](https://github.com/ktorio/ktor)
- The [Ktor Slack chat](https://app.slack.com/client/T09229ZC6/C0A974TJ9). You'll need to [request an invite](https://surveys.jetbrains.com/s3/kotlin-slack-sign-up) to join.

## Features

Here's a list of features included in this project:

| Name                                               | Description                                                 |
| ----------------------------------------------------|------------------------------------------------------------- |
| [Routing](https://start.ktor.io/p/routing-default) | Allows to define structured routes and associated handlers. |

## Building & Running

To build or run the project, use one of the following tasks:

| Task                                    | Description                                                          |
| -----------------------------------------|---------------------------------------------------------------------- |
| `./gradlew test`                        | Run the tests                                                        |
| `./gradlew build`                       | Build everything                                                     |
| `./gradlew buildFatJar`                 | Build an executable JAR of the server with all dependencies included |
| `./gradlew buildImage`                  | Build the docker image to use with the fat JAR                       |
| `./gradlew publishImageToLocalRegistry` | Publish the docker image locally                                     |
| `./gradlew run`                         | Run the server                                                       |
| `./gradlew runDocker`                   | Run using the local docker image                                     |

If the server starts successfully, you'll see the following output:

```
2024-12-04 14:32:45.584 [main] INFO  Application - Application started in 0.303 seconds.
2024-12-04 14:32:45.682 [main] INFO  Application - Responding at http://0.0.0.0:8080
```

### Rotas disponíveis

### /admin/usuarios/linkar-perfil

"Admin vira Profissional": O SuperAdmin quer ele mesmo atender. O que ele faz?

1. O Admin digita o e-mail no formulário "Cadastrar Equipe".
2. O frontend chama POST /admin/usuarios/equipe.
3. A API retorna o 409 Conflict ("Este email já está em uso.").
4. O frontend vê o 409 e mostra "Este e-mail já pertence a um usuário. Deseja linkar um perfil de Profissional a esta conta?"
5. Se o Admin clicar "Sim", o frontend chama a outra rota: POST /admin/usuarios/linkar-perfil

| Rotas                           | Tipo                                                | Descrição                                                                                                                                                                                                                                                                                                                                                                                               |
|---------------------------------|-----------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `/admin/usuarios/linkar-perfil` | POST                                                | 1 Admin digita o e-mail no formulário "Cadastrar Equipe". \n2. O frontend chama POST /admin/usuarios/equipe. \n3. A API retorna o 409 Conflict ("Este email já está em uso."). 4 frontend vê o 409 e mostra "Este e-mail já pertence a um usuário. Deseja linkar um perfil de Profissional a esta conta?" 5. Se o Admin clicar "Sim", o frontend chama a outra rota: POST /admin/usuarios/linkar-perfil |
| `.`                             | Build everything                                    |                                                                                                                                                                                                                                                                                                                                                                                                         |
| `.`                             | Build an executable JAR of the server with all depe | dencies included                                                                                                                                                                                                                                                                                                                                                                                        |
| `.`                             | Build the docker image to use with the fat JAR      |                                                                                                                                                                                                                                                                                                                                                                                                         |
| `.`                             | Publish the docker image locally                    |                                                                                                                                                                                                                                                                                                                                                                                                         |
| `.`                             | Run the server                                      |                                                                                                                                                                                                                                                                                                                                                                                                         |
| `.`                             | Run using the local docker image                    |                                                                                                                                                                                                                                                                                                                                                                                                         |