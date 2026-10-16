# Migus backend — testes C05

## Requisitos e configuração

- JDK 21 e acesso à internet para baixar Maven e dependências no primeiro uso.
- Conexão MongoDB de testes na nuvem (Atlas previsto pelo grupo).
- Docker e Testcontainers não são utilizados nesta versão.

Quando a conexão estiver disponível, configure `MONGODB_TEST_URI` no ambiente do
terminal ou da IDE que executará os testes. Receba a URI por um meio privado;
não a coloque no código, neste README ou no Git. O usuário do banco deve ter
permissão de leitura e escrita somente em `migus_testes`. No Atlas, será necessário
liberar o acesso de rede da máquina que executa os testes.

Os dois testes ativam o profile `test`, com configuração em
`src/test/resources/application-test.properties`. A propriedade do Spring Boot 4.1.1
é `spring.mongodb.uri`. A classe verifica a variável e dá um erro claro se estiver
ausente ou vazia, sem ignorar testes e sem usar localhost ou a conexão de desenvolvimento.
A configuração dinâmica garante que a URI venha da variável exclusiva de testes.

## Execução

Na raiz do projeto, com a variável configurada, execute no PowerShell:

```powershell
.\mvnw.cmd test
```

Para apenas compilar, sem executar testes nem conectar ao banco:

```powershell
.\mvnw.cmd test-compile
```

A execução desta versão está pendente da conexão que será disponibilizada pelo Leo.
O sucesso anterior com Testcontainers não valida esta adaptação.

## Como funciona e padrão

JUnit Jupiter, Spring Boot Test e Mockito continuam fornecidos pelos starters.
O `MongoClient` é fornecido pelo Spring, sem simular o banco com Mockito.
`contextLoads()` foi preservado e usa o mesmo profile de testes.

O teste de integração segue três etapas:

1. Preparação: seleciona `migus_testes`, coleção `c05_documentos`, e cria dados
   fictícios com um novo identificador.
2. Execução: insere o documento e busca pelo identificador.
3. Verificação: confirma que encontrou o documento e confere identificador, nome e idade.

Um bloco `finally` tenta remover somente o documento criado pela execução, mesmo
se uma verificação falhar. Não apaga bancos ou coleções. Se a conexão cair ou faltar
permissão, a limpeza também poderá falhar e o erro será apresentado.

Mantenha os testes em `src/test/java`, nos pacotes correspondentes à aplicação.
Use classes terminadas em `Test` ou `Tests`, métodos com nomes claros, dados
fictícios e testes independentes da ordem de execução.

`test` executa os testes; `verify` também aplica as regras existentes de cobertura
JaCoCo. Workflow, JaCoCo e Sonar não foram alterados nesta adaptação.
