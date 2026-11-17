# Migus Backend

API backend do projeto Migus, feita em Java com Spring Boot, Maven e MongoDB.

## O que tem aqui

- **Cadastro e atualização de usuários** (`PUT /api/usuarios/{id}`): o usuário só altera a própria conta e não pode mudar `tipoUsuario` nem `ativo`.
- **Cadastro de programas** (`POST /api/programas`): restrito a contas ativas do tipo `RESPONSAVEL`, autenticadas com JWT.
- **Autenticação via JWT** no cabeçalho `Authorization: Bearer <token>`.
- **Testes unitários** (JUnit + Mockito), sem banco de dados.
- **Teste de integração com MongoDB** (`ProgramaMongoIT`), executado separadamente.
- **CI** com GitHub Actions (`.github/workflows`).

## Requisitos

- JDK 21
- Git
- Internet no primeiro uso (para baixar o Maven e as dependências)
- Um banco MongoDB (ex.: MongoDB Atlas), necessário somente para rodar a aplicação

## Como baixar

```bash
git clone https://github.com/peeduardo/migus-backend.git
cd migus-backend
```

## Como rodar a aplicação

1. Configure a conexão com o MongoDB. ⚠️ Confirme em `src/main/resources` o nome exato da propriedade/variável (provavelmente `MONGODB_URI`):

   ```bash
   # Linux/macOS
   export MONGODB_URI="mongodb+srv://usuario:senha@cluster/migus"

   # Windows (PowerShell)
   $env:MONGODB_URI="mongodb+srv://usuario:senha@cluster/migus"
   ```

2. Inicie a aplicação:

   ```bash
   # Linux/macOS
   ./mvnw spring-boot:run

   # Windows (PowerShell)
   .\mvnw.cmd spring-boot:run
   ```

⚠️ A porta e outras variáveis (como o segredo do JWT) devem estar em `application.properties`/`application.yml`. Por padrão, o Spring Boot usa a porta 8080.

## Como rodar os testes

Testes unitários (não precisam de banco):

```bash
./mvnw clean test            # Linux/macOS
.\mvnw.cmd clean test        # Windows
```

Validação completa, com regras de cobertura:

```bash
./mvnw -B -ntp clean verify
```

Teste de integração com MongoDB (opcional): defina `MONGODB_TEST_URI` apontando para o banco `migus_testes` e execute:

```bash
./mvnw -B -ntp "-Dtest=ProgramaMongoIT" test
```

