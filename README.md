# Migus backend

## Executar

Requisito: JDK 21. No primeiro uso, é necessário acesso à internet para baixar
Maven e dependências. Na raiz do projeto, execute no PowerShell:

```powershell
.\mvnw.cmd clean test
```

No Linux/macOS, use `./mvnw clean test`.

Os testes unitários da C05 usam JUnit e Mockito fornecidos pelos starters existentes. Não iniciam
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

## C11 — RF04: cadastrar programa

`POST /api/programas`, com o JWT existente no cabeçalho `Authorization: Bearer <token>`.
Somente uma conta existente, ativa e com `tipoUsuario` igual a `RESPONSAVEL` pode cadastrar.
O cadastro público continua criando `interessado`.

Exemplo de corpo (categorias e dias abaixo são apenas exemplos, não listas fechadas):

```json
{
  "nome": "Apoio à comunidade",
  "descricao": "Atividades de apoio",
  "categoria": ["Educação"],
  "diasDisponiveis": ["segunda"],
  "horarioInicio": "09:00",
  "horarioFim": "12:00",
  "ativo": true
}
```

Todos esses campos são obrigatórios. Textos e itens das listas não podem estar
em branco; listas não podem estar vazias. `ativo` precisa ser informado, inclusive
quando for `false`. Não há categorias fechadas, limite de tamanho, formato de
horário ou regra de duração definidos pelo grupo; essas restrições não foram criadas.

O servidor gera o ID e a data de cadastro e obtém o responsável pelo JWT e pelo
usuário consultado no banco. Campos de ID, responsável e auditoria enviados pelo
cliente não são usados. `dataAtualizacao` começa nula. A coleção é `programas`,
com ID e responsável mapeados para ObjectId e datas para Date no MongoDB.

Respostas: 201 com o programa criado; 400 para dados inválidos; 403 quando o JWT
estiver ausente/inválido ou a conta não tiver permissão. O bloqueio sem JWT mantém
o comportamento da configuração de segurança existente.

### Ajuste necessário na atualização de usuários

`PUT /api/usuarios/{id}` permite atualizar somente a própria conta. Enviar
`tipoUsuario` ou `ativo` com valor não nulo retorna 403. Nome, e-mail, telefone e
data de nascimento continuam seguindo as regras existentes. Perfil e status
atuais são preservados. Não foi criado cadastro ou promoção pública de responsável.

### Validar sem banco

Com JDK 21 selecionado, execute:

```powershell
.\mvnw.cmd -B -ntp clean verify
```

Além dos testes existentes, há testes das regras e testes HTTP com JWT, controller
e service reais e repositories simulados. O contexto web de teste não carrega a
aplicação nem a rotina MongoDB de inicialização. Eles não comprovam persistência real.

### Validar persistência separadamente

Configure `MONGODB_TEST_URI` no ambiente, com credencial exclusiva de testes e
banco `migus_testes` na URI. Não grave a conexão no Git. O teste não usa
`MONGODB_URI`, `.env` ou outra conexão como alternativa.

```powershell
.\mvnw.cmd -B -ntp "-Dtest=ProgramaMongoIT" test
```

O Surefire seleciona por padrão nomes como `Test*`, `*Test`, `*Tests` e `*TestCase`.
`ProgramaMongoIT` não corresponde a esses padrões. Não há Failsafe configurado no
projeto; por isso `clean verify` não o executa. O comando acima o seleciona explicitamente. Se faltar a variável ou o banco indicado não for `migus_testes`, falha antes
de criar o contexto. Não há teste desativado ou condição que apresente sucesso
quando faltar conexão.

O contexto mínimo usa o profile `test` e cria somente a conexão e o MongoTemplate.
Não carrega `MigusApplication` nem executa a rotina do Leo. O teste cria um responsável
fictício e um programa, consulta o programa e confere também os tipos gravados.
No `finally`, remove somente os registros identificados como próprios daquela
execução, inclusive quando uma verificação falha, e consulta os mesmos identificadores
para confirmar a limpeza. Não remove bancos ou coleções.

Validação local em 28/10/2026: `ProgramaMongoIT` passou no Atlas, em `migus_testes`,
com 1 teste, sem falhas, erros ou testes ignorados. Foram confirmadas a gravação,
a leitura, os tipos ObjectId/Date e a remoção do programa e do usuário fictício.
O sucesso de `clean verify` não substitui essa validação separada.

Neste computador, a URI SRV falhou na consulta DNS do Java. A execução passou
com uma URI padrão `mongodb://` dos mesmos servidores do cluster, com TLS ligado
(equivalente à conexão SRV). Nenhuma configuração permanente foi alterada.
A senha foi lida de forma oculta no terminal; a URI existiu somente na variável
de ambiente do processo de teste e foi removida ao terminar. Não salve senhas
ou URIs completas em comandos, arquivos do projeto ou logs.

## C17 — RF10: Visualizar minhas inscrições

`GET /api/usuarios/me/inscricoes` consulta somente as inscrições da conta autenticada e ativa.
Envie o JWT no cabeçalho `Authorization: Bearer <token>`. O servidor usa a identidade
do token; parâmetros ou corpo enviados pelo cliente não escolhem outro usuário.

A resposta `200` é uma lista com `inscricaoId`, `programaId`, `programaNome`,
`dataInscricao` (data ISO) e `status` (`PENDENTE`, `APROVADA`, `RECUSADA` ou `TRANSFERIDA`).
Sem inscrições, retorna `[]`. Programas inativos também são consultados.
Se um programa não existir, a inscrição continua na lista, com seu `programaId`
e `programaNome: null`. A consulta não grava nem remove dados.

Os testes verificam o service com repositories simulados e a rota HTTP com JWT,
incluindo isolamento entre contas, tentativa de trocar a identidade, autenticação,
conta inativa, lista vazia e programas inativos ou indisponíveis.

Com Java 21, execute no Windows:

```powershell
.\mvnw.cmd -B -ntp clean verify
```

Esse comando executa os testes sem banco e gera `target/site/jacoco/jacoco.xml`.
Validação real da RF10 em 06/11/2026, no banco `migus`, autorizada para dados fictícios exclusivos: cadastro público e login, lista vazia, inscrições existentes, os três estados, isolamento entre contas, programa inativo e programa ausente passaram na comparação entre API e MongoDB. Os IDs foram registrados fora do repositório. As 4 inscrições, 3 programas e 3 contas da execução foram removidos pelos IDs, com ausência confirmada. `migus_testes` e os dados anteriores foram preservados.

Novos cadastros enquadrados na regra de domínio existente gravam `RESPONSAVEL`, como esperado pelo cadastro de programas e pela interface. Contas antigas não foram alteradas. Após essa correção, `clean verify` passou com 89 testes. A conferência manual feita pelo autor com frontend, backend e banco reais confirmou os três estados, isolamento entre contas, lista vazia, programa inativo e programa indisponível, em desktop e celular emulado de 393 px. A navegação por teclado foi conferida anteriormente na prévia simulada, separadamente. Ao encerrar a demonstração, foram removidas pelos IDs as 5 inscrições (incluindo a inscrição adicional), os 2 programas fictícios restantes e as 4 contas, com ausência confirmada. O Programa Amigos Legais e suas demais inscrições foram preservados; `migus_testes` não foi acessado na limpeza.
