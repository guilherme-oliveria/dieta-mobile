# Contrato da fase 1 — BE-C1

Complementa `integracao.md`. Cópia oficial no backend; mobile e frontend devem acompanhar suas alterações.

## Entrega e limites

Os 17 endpoints de BE-02 a BE-08 estão publicados como stubs `501 NAO_IMPLEMENTADO`, anotados no OpenAPI (`/v3/api-docs`). O `manifesto.json` em `src/test/resources/contrato/` indexa cada método/caminho, status de sucesso futuro e exemplos de requisição/resposta/erro, todos validados contra os records Java. Exemplos são dados fictícios, com endereços `example.invalid` e credenciais explicitamente sem validade. O mobile pode copiá-los e usar `MockEngine` assim que o PR de BE-C1 estiver aberto.

Os JSONs de sucesso representam o contrato **futuro**; os controllers ainda não retornam esses dados. Não há persistência, JWT, e-mail, validação de dono/plano, idempotência, auditoria ou regras de negócio executadas. A configuração Basic existente continua protegendo todas as rotas, inclusive as futuramente públicas. Nos testes HTTP há apenas um usuário simulado, sem credenciais reais. BE-01 a BE-08 substituem os stubs. Não há alteração de banco nem migração nesta tarefa.

O OpenAPI segue protegido pela configuração atual. A Swagger UI só é habilitada em desenvolvimento com `SWAGGER_UI_ENABLED=true`. O stub não cria `X-Trace-Id` (BE-01); usa `traceId: null` e `regra: null`. Datas fixas dos exemplos são insumos de serialização; as implementações calcularão os prazos usando `Clock` injetado. A dependência springdoc 3.2.0 é compatível com Boot 4.1 segundo a [matriz oficial](https://springdoc.org/#what-is-the-compatibility-matrix-of-springdoc-openapi-with-spring-boot).

## Endpoints

Todos os caminhos recebem `/api/v1`. Os nomes de diretório dos exemplos correspondem aos `operationId` do OpenAPI.

| Operação | Requisição / resposta futura | Status futuro | Exemplos |
| --- | --- | --- | --- |
| `POST /auth/cadastro-nutri` | `CadastroNutriRequisicao` → `CadastroNutriResposta` | 201 | `cadastrarNutri/` |
| `POST /auth/login` | `LoginRequisicao` → `SessaoResposta` | 200 | `entrar/` |
| `POST /auth/refresh` | `RefreshRequisicao` → `SessaoResposta` | 200 | `renovarSessao/` |
| `POST /auth/logout` | `LogoutRequisicao` → sem corpo | 204 | `sair/` |
| `POST /auth/confirmar-email` | `ConfirmarEmailRequisicao` → sem corpo | 204 | `confirmarEmail/` |
| `POST /auth/esqueci-senha` | `EsqueciSenhaRequisicao` → sem corpo | 202 | `recuperarSenha/` |
| `POST /auth/redefinir-senha` | `RedefinirSenhaRequisicao` → sem corpo | 204 | `redefinirSenha/` |
| `GET /eu` | Sem corpo → `EuResposta` | 200 | `consultarEu/` |
| `GET /pacientes` | Query → `PaginaPacientesResposta` | 200 | `listarPacientes/` |
| `POST /pacientes` | `CriarPacienteRequisicao` → `PacienteResposta` | 201 | `criarPaciente/` |
| `GET /pacientes/{id}` | Sem corpo → `PacienteResposta` + `ETag` | 200 | `consultarPaciente/` |
| `PATCH /pacientes/{id}` | `AtualizarPacienteRequisicao` + `If-Match` → `PacienteResposta` + `ETag` | 200 | `atualizarPaciente/` |
| `POST /pacientes/{id}/convite` | Sem corpo → `ConviteCriadoResposta` | 201 | `convidarPaciente/` |
| `GET /convites/{codigo}` | Sem corpo → `ConviteResposta` | 200 | `consultarConvite/` |
| `POST /convites/{codigo}/aceitar` | `AceitarConviteRequisicao` → `AceiteConviteResposta` | 200 | `aceitarConvite/` |
| `POST /pacientes/{id}/encerrar-vinculo` | Sem corpo → `VinculoResposta` | 200 | `encerrarVinculo/` |
| `GET /sync` | Query → `SyncResposta` | 200 | `puxarMudancas/` |

Os POSTs de cadastro da nutri, criação de paciente, geração/aceite de convite e encerramento de vínculo aceitam `Idempotency-Key` UUID. Reenvio com a mesma chave em 24 h representa a mesma operação. Não há corpo JSON artificial em operações sem corpo. `esqueci-senha` retorna o mesmo 202 para e-mail existente ou inexistente.

## Conta, perfil e sessão

- Cadastro exige nome, e-mail, senha, CRN, `aceitouTermos: true` e `versaoTermos`; `fuso` é opcional, padrão `America/Sao_Paulo`. Retorna `usuario` + `assinatura`, sem login automático.
- `dispositivo` no login/aceite contém `id` UUID da instalação, `plataforma` (`ANDROID`, `IOS`, `DESKTOP`) e `nomeAparelho`. Não contém token FCM nesta fase.
- `usuario` contém `id`, `nome`, `email`, `emailConfirmado`, `perfis`, `perfil` ativo, `nutricionistaId`, `pacienteId` e `fuso`. O valor no JSON/JWT é `NUTRICIONISTA`; "NUTRI" é abreviação descritiva no plano. As identidades podem coexistir na conta, conforme BE-05.
- Com um único perfil, login pode escolhê-lo. Com vários e `perfil` omitido, retorna `usuario.perfis`; `usuario.perfil`, tokens e `expiraEm` ficam nulos. A tela de escolha repete o login com o perfil escolhido e credenciais mantidas apenas em memória durante esse fluxo. Perfil alheio retorna `PERFIL_INVALIDO`. Refresh preserva o perfil ativo e sempre devolve tokens. `expiraEm` é instante UTC absoluto, não duração.
- `GET /eu` agrupa `usuario`, `assinatura` (nula na sessão paciente) e `vinculos` (lista vazia quando não aplicável). A assinatura resume `organizacaoId`, `plano`, `status`, `limitePacientes`, `pacientesOcupados` e `fimPeriodoEm`; checkout fica na fase 6.

## Pacientes e consentimento

- Listagem: `pagina=0`, `tamanho=20` (máximo 100), `ordem=nome,asc`, `busca` opcional por nome e `status` opcional (`CONVIDADO`, `ATIVO`, `ENCERRADO`). Resposta: `itens`, `pagina`, `tamanho`, `total`.
- `PacienteResposta.id` é o paciente global; `vinculoId` identifica o prontuário da nutri autenticada. Nome, contato, nascimento, sexo, estatura de referência em metros, objetivo e observações são a projeção da ficha do vínculo. Requisições nunca fornecem dono/nutricionistaId. `adesao7d` fica nula até BE-16.
- `sexo`: `FEMININO`, `MASCULINO`, `OUTRO`, `NAO_INFORMADO`. Campos opcionais podem ser nulos. `PATCH` envia o conjunto de campos editáveis da ficha, com nome obrigatório; opcional omitido ou nulo limpa o campo. Status, vínculo, versão e dono não são editáveis no corpo. `If-Match: "<versao>"` é obrigatório; GET/PATCH retornam `ETag: "<versao>"`. Conflito devolve o registro em `atual`.
- Convite público só expõe `nutricionista` (`id`, `nome`, `crn`), `termo` (`versao`, `texto`) e `expiraEm`, sem identidade do paciente. Código usado/invalidado retorna `CONVITE_INVALIDO`; vencido, `CONVITE_EXPIRADO`. A geração devolve código de 8 caracteres, `link` de origem configurável e `expiraEm` calculado pelo servidor; não recebe prazo no corpo. Reenvio invalida o convite anterior.
- Aceite envia `aceitouTermo: true`, `versaoTermo` e `dispositivo`. Conta nova envia `cadastro: {nome, email, senha, fuso?}`; conta existente exige `Authorization: Bearer ...` e `cadastro: null`. Conhecer um e-mail não autentica o paciente. Consentimento e IP vêm da operação no servidor. Resposta traz `sessao` ativa `PACIENTE` e `vinculo`.
- Nutri dona ou o próprio paciente podem encerrar em `/pacientes/{id}/encerrar-vinculo`, com identidade conferida no backend. Encerramento mantém prontuário só leitura e acesso do paciente às dietas recebidas.

## Sync de leitura

`cursor` ausente inicia a carga; `limite` padrão 500. Resposta usa `mudancas.pacientes`, `mudancas.vinculos` e `mudancas.nutris`, todas listas, mesmo vazias. Os dados são filtrados por vínculo/dono. Cada item possui `id`, `versao`, `excluido` e `dados` tipados. Perda de visibilidade é tombstone `excluido: true`, `dados: null`; o app apaga sua cópia. Cursor é string decimal opaca para evitar perda de precisão de bigint nos clientes. Persista o cursor de cada página e continue enquanto `temMais`. Coleções de fases futuras serão novas e opcionais para clientes anteriores, que ignoram campos desconhecidos.

## Erros

`ProblemaResposta` modela `type`, `title`, `status`, `detail`, `codigo`, `regra`, `campos`, `traceId`, `atual`. `campos` é sempre lista; `regra`, `traceId` e `atual` podem ser nulos. O cliente decide por `codigo`. `atual` só se aplica ao conflito de versão. Erros técnicos sem regra específica usam `regra: null`; token inválido recebe RN de acordo com confirmação/recuperação, e nenhuma RN inventada para refresh. A conversão de exceções e de validação para esse formato entra em BE-01.

`CodigoErro` preserva os códigos definidos em `integracao.md` e associa os códigos de negócio às RNs pertinentes. A lista das novas entradas da fase está na seção Erros de `integracao.md`.
