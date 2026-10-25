# Contrato da fase 1 — BE-C1

Complementa `integracao.md`. Cópia oficial no backend; mobile e frontend devem acompanhar suas alterações.

## Entrega e limites

Os 17 endpoints de BE-02 a BE-08 estão anotados no OpenAPI (`/v3/api-docs`). BE-02 implementa `POST /auth/cadastro-nutri` e BE-03 implementa login, refresh e logout; os outros 13 continuam stubs `501 NAO_IMPLEMENTADO`. O `manifesto.json` em `src/test/resources/contrato/` indexa cada método/caminho, status de sucesso e exemplos de requisição/resposta/erro, todos validados contra os records Java. Exemplos são dados fictícios, com endereços `example.invalid` e credenciais explicitamente sem validade. O mobile pode usá-los com `MockEngine` até integrar cada endpoint real.

Os JSONs são exemplos de formato; seus valores fictícios não são dados fixos do servidor. BE-01 fornece erros, rastreio, idempotência e persistência transversal. BE-02 aplica as regras do cadastro e cria conta/organização/assinatura numa transação. BE-03 substitui Basic por Bearer/JWT e libera somente cadastro/login/refresh/logout ao público. Envio de e-mail, vínculos e auditoria clínica ainda entram nas respectivas tarefas; o cadastro não faz login automático. Testes usam somente dados fictícios e chaves geradas em memória.

O OpenAPI segue protegido pela configuração atual. A Swagger UI só é habilitada em desenvolvimento com `SWAGGER_UI_ENABLED=true`. Desde BE-01, o filtro cria `X-Trace-Id` e preenche o rastreio dos erros/stubs; o `traceId: null` dos exemplos continua válido como campo opcional do contrato. Datas fixas dos exemplos são insumos de serialização; prazos reais usam `Clock` injetado. A dependência springdoc 3.2.0 é compatível com Boot 4.1 segundo a [matriz oficial](https://springdoc.org/#what-is-the-compatibility-matrix-of-springdoc-openapi-with-spring-boot).

## Endpoints

Todos os caminhos recebem `/api/v1`. Os nomes de diretório dos exemplos correspondem aos `operationId` do OpenAPI.

| Operação | Requisição / resposta | Status de sucesso | Exemplos |
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

### Implementação de sessões (BE-03)

Login, refresh e logout são públicos: a senha ou o próprio refresh autentica a operação. Access usa JWT RS256 por 15 minutos. Refresh contém 32 bytes aleatórios, guardados somente como SHA-256; a família tem prazo absoluto de 30 dias, sem prolongar na rotação. Reuso de token consumido revoga toda a família, inclusive o sucessor. Logout revoga a família do dispositivo informado pelo refresh, sem afetar outro dispositivo; é idempotente para token desconhecido. Revogar refresh não revoga access já emitido, que dura no máximo 15 minutos.

RN-CON-03 usa janela móvel de 15 minutos: a quinta falha bloqueia o e-mail por 15 minutos, inclusive para e-mail inexistente; retorna `LOGIN_BLOQUEADO`, 429 e `Retry-After`. As outras falhas usam `CREDENCIAIS_INVALIDAS`, 401, sem distinguir usuário inexistente/inativo. Tentativas e bloqueios são persistentes; senha correta não remove um bloqueio ainda vigente. Login também limita 10 requisições/minuto por IP da conexão, com `TAXA_EXCEDIDA` e `Retry-After`; a proteção por e-mail vale entre instâncias. O limitador por IP desta etapa é local a cada processo e não confia em `X-Forwarded-For`.

As respostas de sessão usam `Cache-Control: no-store`. O app envia refresh no JSON aprovado. O transporte web também aceita cookie `refreshToken` (`HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth`), sem corpo, mas somente com `Origin` igual à origem HTTPS configurada em `WEB_ORIGEM`; sem essa configuração o modo cookie é recusado. Login/refresh emitem o cookie e logout o expira. Nenhum token ou senha aparece na representação textual dos DTOs nem nos logs.

JWT, fuso e expiração usam `Clock` injetado. As chaves RSA vêm de `JWT_CHAVE_PRIVADA` (PEM PKCS#8) e `JWT_CHAVE_PUBLICA` (PEM X.509), com no mínimo 2048 bits. Só em perfis `dev`/`test` a ausência de ambas gera um par efêmero; em outros perfis a aplicação recusa iniciar sem chaves. Reiniciar dev invalida os access tokens emitidos antes do reinício. A seleção de perfil respeita o formato publicado; `/eu`, validação de dono no banco a cada chamada e auditoria permanecem em BE-05.

- Cadastro exige nome, e-mail, senha, CRN, `aceitouTermos: true` e `versaoTermos`; `fuso` é opcional, padrão `America/Sao_Paulo`. Retorna `usuario` + `assinatura`, sem login automático. BE-02 normaliza nome/CRN/versão removendo espaços nas bordas e e-mail também para minúsculas; a senha permanece exatamente como informada. Limites de armazenamento: nome 150, e-mail 255, CRN 20, versão do termo 40 e fuso 80 caracteres. Campos inválidos geram 400 com `campos`; senha comum usa `FORMATO_INVALIDO`, RN-CON-02. E-mail já cadastrado retorna 409 `EMAIL_JA_CADASTRADO`, RN-CON-01, inclusive sob concorrência. A versão declarada do termo e o instante do aceite são guardados pelo servidor. CRN e e-mail nascem não verificados; prazo e limite do teste vêm do plano ativo no banco, sem constantes no caso de uso.
- `dispositivo` no login/aceite contém `id` UUID da instalação, `plataforma` (`ANDROID`, `IOS`, `DESKTOP`) e `nomeAparelho` (até 150 caracteres). Não contém token FCM nesta fase.
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
