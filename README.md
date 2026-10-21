# Migus backend — testes unitários C05

## Executar

Requisito: JDK 21. No primeiro uso, é necessário acesso à internet para baixar
Maven e dependências. Na raiz do projeto, execute no PowerShell:

```powershell
.\mvnw.cmd clean test
```

No Linux/macOS, use `./mvnw clean test`.

Os testes usam JUnit e Mockito fornecidos pelos starters existentes. Não iniciam
Spring, não acessam MongoDB ou Atlas, não usam Docker ou Testcontainers e não
precisam de variáveis de conexão. A configuração MongoDB da aplicação continua
sendo necessária somente para executar a aplicação real.

## O que é testado

A classe `MigusApplicationTests` testa a rotina `testarMongoDB` já implementada:

- Quando o e-mail não existe, verifica que o repository recebe a chamada para
  salvar e que o usuário enviado tem o nome e o e-mail esperados.
- Quando o e-mail já existe, verifica que o repository não recebe a chamada para salvar.

Nos dois casos, também verifica a consulta de existência e a contagem de usuários.
O repository é um mock: suas respostas são combinadas no teste e nenhum banco é
consultado. `ArgumentCaptor` guarda o usuário passado para `save`, permitindo
conferir seus dados. Não existe regra de erro adicional no código para testar.

## Padrão

- Testes em `src/test/java`, seguindo o pacote da classe testada.
- Classes terminadas em `Test` ou `Tests` e métodos com nomes claros.
- Separar preparação, execução e verificação.
- Usar Mockito somente para dependências externas, como o repository.
- Não usar `@SpringBootTest` nos testes unitários.

O antigo teste de integração e seu profile de banco foram removidos.
`clean test` executa a suíte sem reutilizar classes ou recursos de testes antigos.
`verify` também aplica as regras de cobertura existentes. Workflow, JaCoCo e Sonar
não foram alterados pela adaptação para testes unitários.
