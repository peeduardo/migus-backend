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

Novos cadastros enquadrados na regra de domínio existente gravam `RESPONSAVEL`, como esperado pelo cadastro de programas e pela interface. Contas antigas não foram alteradas. Após essa correção, `clean verify` passou com 89 testes. A conferência manual feita pelo autor com frontend, backend e banco reais confirmou os três estados, isolamento entre contas, lista vazia, programa inativo e programa indisponível, em desktop e celular emulado de 393 px. A navegação por teclado foi conferida anteriormente na prévia simulada, separadamente. Ao encerrar a demonstração, foram removidas pelos IDs as 5 inscrições (incluindo a inscrição adicional), os 2 programas fictícios restantes e as 4 contas, com ausência confirmada. O Programa Amigos Legais e suas demais inscrições foram preservados; `migus_testes` não foi acessado na limpeza.

## C12 — RF05: Editar programa

`PUT /api/programas/{id}`, com o JWT no cabeçalho `Authorization: Bearer <token>`.
O corpo é o mesmo do cadastro (`nome`, `descricao`, `categoria`, `diasDisponiveis`,
`horarioInicio`, `horarioFim`, `ativo`), com as mesmas validações. Os dados atuais
são consultados pelo `GET /api/programas/{id}` já existente.

Somente o responsável ativo que cadastrou o programa pode editá-lo. O servidor
preserva ID, responsável e data de cadastro e define `dataAtualizacao`; campos
de ID, responsável ou auditoria enviados pelo cliente não são usados.

Respostas: 200 com o programa atualizado; 400 para dados inválidos; 403 para JWT
ausente/inválido, perfil sem permissão ou programa de outro responsável; 404
quando o programa não existe.

Os testes de `ProgramaServiceTest` e `ProgramaControllerTest` usam repositories
simulados e conferem o objeto enviado ao `save`. Eles não comprovam persistência
real no MongoDB; para isso, valide com a aplicação conectada ao banco de testes.

## Disponibilidade e inscrições

Inativar ou reativar um programa preserva as inscrições e suas situações.
As consultas pessoais informam a disponibilidade separadamente. A reativação
registrada pelo servidor permite mostrar o aviso de retorno do programa.
Inscrições antigas em `SUSPENSA` não são restauradas automaticamente.

A API impede nova inscrição quando o par usuário/programa já existe. A garantia
contra solicitações simultâneas ainda depende do alinhamento e validação de um
índice único, que não é criado automaticamente.
