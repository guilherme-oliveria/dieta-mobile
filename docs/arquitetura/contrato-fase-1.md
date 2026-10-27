# Contrato da fase 1 — BE-C1

Complementa `integracao.md`. Cópia oficial no backend; mobile e frontend devem acompanhar suas alterações.

## Entrega e limites

Os 17 endpoints de BE-02 a BE-08 estão anotados no OpenAPI (`/v3/api-docs`). BE-02 implementa `POST /auth/cadastro-nutri` e BE-03 implementa login/refresh/logout e BE-04 confirmação/recuperação/redefinição; BE-05 a BE-08 implementam as operações restantes, sem stubs na fase 1. O `manifesto.json` em `src/test/resources/contrato/` indexa cada método/caminho, status de sucesso e exemplos de requisição/resposta/erro, todos validados contra os records Java. Exemplos são dados fictícios, com endereços `example.invalid` e credenciais explicitamente sem validade. O mobile pode usá-los com `MockEngine` até integrar cada endpoint real.

Os JSONs são exemplos de formato; seus valores fictícios não são dados fixos do servidor. BE-01 fornece erros, rastreio, idempotência e persistência transversal. BE-02 aplica as regras do cadastro e cria conta/organização/assinatura numa transação. BE-03 substitui Basic por Bearer/JWT e libera cadastro/login/refresh/logout ao público; BE-04 acrescenta as três rotas públicas de e-mail/senha. Vínculos e auditoria clínica foram implementados nas respectivas tarefas; o cadastro não faz login automático. Testes usam somente dados fictícios e chaves geradas em memória.

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

### Contexto e isolamento (BE-05)

Toda requisição autenticada confere o usuário ativo, perfil e identidade do JWT no banco, antes de acessar o cache idempotente. `nid`/`pid` alheios ou identidade removida invalidam a autenticação (401 `NAO_AUTENTICADO`). Organização vem da identidade da nutri no banco; não há header ou campo de corpo que escolha dono. A informação da conta e dos perfis em `/eu` reflete o banco no momento da requisição, sem exigir novo token para refletir nome/fuso/confirmação.

`GET /eu` retorna os mesmos campos/JSONs: no perfil NUTRICIONISTA, assinatura da própria organização e vínculos vazios; no perfil PACIENTE, assinatura nula e vínculos próprios (inclusive encerrados), sem vínculos de outro paciente. ADMIN vê apenas a própria conta, sem prontuários. Login/seleção de perfil reutiliza BE-03: `NUTRICIONISTA` é o valor no wire e "NUTRI" apenas abreviação do plano.

Rotas de nutri exigem perfil NUTRICIONISTA (403 `PERFIL_INVALIDO`). Escritas verificam a assinatura antes de executar ou reproduzir a operação idempotente: `TESTE` vencido por Clock, `CANCELADA`, `EXPIRADA`, ausência de assinatura ou `EM_ATRASO` após carência geram 422 `ASSINATURA_INATIVA`, RN-ASS-04. A carência padrão de 7 dias é configurável e começa em `fimPeriodoEm` (vencimento); sem vencimento conhecido, EM_ATRASO não autoriza escrita. ATIVA segue seu status até o ciclo de vida da BE-27. Leitura e acesso do paciente não são punidos pela assinatura da nutri.

Acesso clínico registra IDs do recurso/donos/ator, ação, perfil, organização, instante, IP da conexão e traceId. Não grava querystring, corpo, token, senha ou valor clínico na trilha. A trilha registra o acesso realizado, não afirma que uma transação posterior foi confirmada; fica preservada mesmo em rollback posterior. Recursos fora da visibilidade são 404 e não são lidos/alterados. A listagem de vínculos do próprio paciente também gera auditoria por vínculo. A retenção de RN-LGPD-01 é de 5 anos; esta etapa não apaga registros da trilha.

### Confirmação e recuperação (BE-04)

O cadastro enfileira o e-mail de confirmação na mesma transação da conta. O link contém token aleatório de 32 bytes, uso único e validade de 24 horas (RN-CON-05). O app envia o token no POST `confirmar-email`, que retorna 204; token inválido, vencido, anulado ou repetido retorna 401 `TOKEN_INVALIDO`, RN-CON-05. O GET do link não altera a conta automaticamente.

`esqueci-senha` sempre retorna 202 sem corpo para um e-mail válido, existente, inativo ou desconhecido, inclusive em falha de enfileiramento; falhas internas desfazem a operação e geram somente log técnico seguro. Usuário ativo recebe link de uso único válido por uma hora (RN-CON-07). Nova solicitação anula os tokens de recuperação anteriores. `redefinir-senha` aplica RN-CON-02, consome o token e revoga todos os refresh tokens desse usuário na mesma transação; retorna 204 sem login automático. Token de outra finalidade também é inválido. Senha fraca é 400 `FORMATO_INVALIDO`, RN-CON-02, sem consumir o token. Access tokens já emitidos vencem em no máximo 15 minutos.

Os três endpoints são públicos, com `@Valid`, sem tokens na resposta. Recuperação limita 10 pedidos/minuto por IP da conexão, com 429 `TAXA_EXCEDIDA` e `Retry-After`. Clientes continuam usando os JSONs aprovados; devem omitir Authorization nas rotas `/auth/**`. Os links e o remetente são configuráveis no servidor e nunca usam o Host do pedido.

O envio é assíncrono por `notificacao_pendente`: payload cifrado enquanto pendente, removido após envio, expiração ou falha definitiva. O banco de tokens guarda somente hash. `NOTIFICACOES_CHAVE` deve conter 32 bytes aleatórios em Base64; falta da chave desfaz o cadastro e não cria link inseguro. O job SMTP é habilitado com `EMAIL_ENVIO_HABILITADO=true`; em dev usa Mailpit do perfil `extras`, e em testes o transporte é falso. Tentativas não registram token, senha, destinatário ou conteúdo da mensagem em logs. Pode haver entrega duplicada após crash entre SMTP e confirmação no banco; o link continua de uso único.

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

## Termos de uso (BE-36)

`GET /legal/terms-of-use` é público e retorna JSON com `versao`, `status` (`RASCUNHO`, `APROVADO`, `PUBLICADO`), `aprovado`, `publicado`, `cadastroHabilitado`, `aprovadoEm`, `publicadoEm`, `conteudo` Markdown e `url`. Usa no-store. A fonte única `docs/legal/terms-of-use.md` é embalada no artefato; o estado inicial é 1.0.0 RASCUNHO, sem datas de aprovação/publicação. Não há parâmetro de requisição nem flag de ambiente que aprove esse documento. Publicação só habilita cadastro quando o estado é PUBLICADO e ambas as datas existem, estão efetivas pelo Clock e aprovação não é posterior à publicação. A revisão de código não publica o conteúdo jurídico.

No cadastro da nutri, `aceitouTermos` e `versaoTermos` existentes passam a ser confrontados com a fonte publicada. Na criação de paciente via convite, `cadastro` ganha os campos opcionais `aceitouTermos` e `versaoTermos`; para efetivamente criar uma conta eles devem declarar true e a versão publicada. Esses campos são independentes de `aceitouTermo`/`versaoTermo` clínicos no corpo externo do convite. Conta existente com cadastro nulo não passa por uma nova criação nem é bloqueada pelo rascunho. Clientes devem consultar os metadados, exibir conteúdo/aceite e manter cadastro indisponível até aprovação **e** publicação; backend aplica a mesma regra, inclusive antes do replay idempotente.

Erros novos: 422 `TERMOS_INDISPONIVEIS` quando cadastro ainda não foi liberado; 422 `TERMOS_NAO_ACEITOS` quando não há aceite explícito da versão vigente (RN-CON-09). Versionamento e instante aceitos ficam em `usuario.versao_termos`/`termos_aceitos_em`, vinculados à PK do usuário, atomicamente; usuários anteriores não recebem aceite fabricado. Textos/estados publicados fictícios existem somente na configuração de testes. Os 49 JSONs/manifesto antigos foram preservados; os campos opcionais novos não são serializados quando nulos.

## Pacientes e consentimento

BE-06 implementa GET/POST `/pacientes` e GET/PATCH `/pacientes/{id}`. A criação grava paciente sem usuário, vínculo CONVIDADO e ficha privada da nutri; não autentica nem concede acesso a uma conta pelo e-mail informado. Associação/consentimento entram no aceite BE-07. ID opcional do cliente deve ser UUIDv7 novo; ID já existente não permite apropriar ou associar prontuário alheio. E-mail repetido em ficha CONVIDADO/ATIVO da mesma nutri retorna `VINCULO_JA_EXISTE`; ausência de e-mail permite fichas distintas sem inferir identidade por nome. Vínculos e fichas de outras nutris não são expostos nem alterados pelo cadastro.

O limite considera CONVIDADO e ATIVO de todas as nutris da organização. A conferência mantém bloqueio transacional da organização até o cadastro concluir, inclusive em concorrência. Assinatura inativa bloqueia escrita antes da idempotência e também no serviço. A mesma chave idempotente reproduz Location/ETag/corpo sem criar nova ficha. Ficha de vínculo ENCERRADO continua legível, mas edição retorna 403 `ACESSO_NEGADO` (RN-VIN-05).

Campos da ficha são independentes do cadastro global e de cada outro vínculo do paciente. `nome` tem até 150 caracteres, e-mail 255, telefone 30, objetivo 2000 e observações 10000; estatura admite até três casas decimais e nascimento não pode estar no futuro. Strings opcionais vazias são normalizadas para nulo; e-mail é normalizado para minúsculas. Busca é substring literal do nome sem distinção de maiúsculas, inclusive `%` e `_` literais. Ordenação aceita `nome,asc`, `nome,desc`, `criadoEm,asc` e `criadoEm,desc`, com desempate estável pelo ID. Página negativa, tamanho fora de 1..100, busca acima de 150 caracteres ou ordem/status inválido retornam 400.

GET/PATCH retornam ETag forte da ficha; POST retorna ETag e Location. `If-Match` aceita somente uma versão inteira não negativa entre aspas (sem `W/`, `*` ou lista). Ausência/formato inválido retorna 400; versão divergente retorna 409 `CONFLITO_VERSAO` com `atual`, sem sobrescrever. A verificação de visibilidade precede a comparação de versão; outra nutri recebe 404 sem o registro em `atual`. Todos os sucessos retornam `Cache-Control: no-store`; acessos clínicos autorizados são auditados por ficha, sem armazenar conteúdo clínico na trilha. `adesao7d` permanece nula.

- Listagem: `pagina=0`, `tamanho=20` (máximo 100), `ordem=nome,asc`, `busca` opcional por nome e `status` opcional (`CONVIDADO`, `ATIVO`, `ENCERRADO`). Resposta: `itens`, `pagina`, `tamanho`, `total`.
- `PacienteResposta.id` é o paciente global; `vinculoId` identifica o prontuário da nutri autenticada. Nome, contato, nascimento, sexo, estatura de referência em metros, objetivo e observações são a projeção da ficha do vínculo. Requisições nunca fornecem dono/nutricionistaId. `adesao7d` fica nula até BE-16.
- `sexo`: `FEMININO`, `MASCULINO`, `OUTRO`, `NAO_INFORMADO`. Campos opcionais podem ser nulos. `PATCH` envia o conjunto de campos editáveis da ficha, com nome obrigatório; opcional omitido ou nulo limpa o campo. Status, vínculo, versão e dono não são editáveis no corpo. `If-Match: "<versao>"` é obrigatório; GET/PATCH retornam `ETag: "<versao>"`. Conflito devolve o registro em `atual`.
- Convite público só expõe `nutricionista` (`id`, `nome`, `crn`), `termo` (`versao`, `texto`) e `expiraEm`, sem identidade do paciente. Código usado/invalidado retorna `CONVITE_INVALIDO`; vencido, `CONVITE_EXPIRADO`. A geração devolve código de 8 caracteres, `link` de origem configurável e `expiraEm` calculado pelo servidor; não recebe prazo no corpo. Reenvio invalida o convite anterior.
- Aceite envia `aceitouTermo: true`, `versaoTermo` e `dispositivo`. Conta nova envia `cadastro: {nome, email, senha, fuso?}`; conta existente exige `Authorization: Bearer ...` e `cadastro: null`. Conhecer um e-mail não autentica o paciente. Consentimento e IP vêm da operação no servidor. Resposta traz `sessao` ativa `PACIENTE` e `vinculo`.
- Nutri dona ou o próprio paciente podem encerrar em `/pacientes/{id}/encerrar-vinculo`, com identidade conferida no backend. Encerramento mantém prontuário só leitura e acesso do paciente às dietas recebidas.

### Operações reais da BE-07

Geração, consulta, aceite e encerramento estão implementados. A geração exige e-mail confirmado da nutri (RN-CON-05, EMAIL_NAO_CONFIRMADO). Corrigir o e-mail da ficha invalida convites destinados ao endereço anterior. O código aleatório tem oito caracteres `[A-HJ-NP-Z2-9]`; `convite` guarda somente SHA-256. Prazo configurável `dieta.convite.validade` (padrão P7D) expira no instante exato. Reenvio invalida os códigos anteriores na mesma transação. O link usa `EMAIL_URL_CONVITE` (padrão `dietamais://convites`), também usado pelo template de e-mail; payload da outbox fica cifrado. Regerar usa a vaga CONVIDADO já ocupada, sem duplicar a contagem, e confere assinatura/limite total. Aceitar não ocupa nova vaga nem pune o paciente pela expiração posterior da assinatura da nutri.

O consentimento clínico exige `dieta.consentimento.versao` e `dieta.consentimento.texto` configurados. Não há texto fictício de produção ou publicação jurídica presumida; sem configuração, gerar/consultar/aceitar retorna `CONSENTIMENTO_OBRIGATORIO`. O aceite exige true e a versão apresentada; registra versão/hash do texto, userId, vínculo, Clock e IP da conexão. Não equivale a termos de uso: BE-36 implementa separadamente `docs/legal/terms-of-use.md` v1.0.0 RASCUNHO, rota `/legal/terms-of-use`, aprovação/publicação e bloqueio de cadastros reais. BE-07 não registra aceite de termos de uso a partir do consentimento clínico.

Conta nova só nasce de convite válido. Quando a ficha informa destinatário, o e-mail deve corresponder ao cadastro ou à conta autenticada. Conta existente exige Bearer e cadastro nulo; conhecer e-mail não autentica. Conta autenticada pode adquirir perfil PACIENTE; a sessão retornada usa esse perfil. Novo e-mail recebe confirmação pela outbox. Conta/paciente/vínculo/ficha/consentimento/sessão são atômicos. Há um paciente global por usuário; somente vínculo/ficha do convite são reatribuídos, mantendo `paciente_origem_id` para rastrear visibilidade. Duplicidade de vínculo aberto é recusada; históricos ENCERRADO permanecem. GET da ficha escolhe o vínculo aberto ou o encerrado mais recente; PATCH altera somente a ficha escolhida. Escritas por nutri são serializadas para evitar corrida entre reenvio, aceite e edição.

Extensão compatível autorizada pelo Guilherme: query opcional `vinculoId` em `/pacientes/{id}/encerrar-vinculo` escolhe o vínculo de `/eu.vinculos`; `{id}` permanece paciente global. Nutri só seleciona vínculo próprio e paciente só seleciona seu próprio vínculo; outro dono/paciente recebe 404. Sem query, nutri usa seu vínculo aberto/mais recente; paciente com um único aberto (ou único total) mantém compatibilidade. Com vários abertos, omissão retorna 400 `OBRIGATORIO`, campo `vinculoId`, sem alterações. Encerrar invalida convites pendentes, libera vaga e atualiza versão/seq da ficha sem apagar histórico; repetição é segura.

Consulta/aceite públicos usam limite por IP da conexão, sem confiar em forwarded headers: `dieta.convite.limite-ip` (padrão 60) e `dieta.convite.janela` (padrão PT1M). Excesso retorna 429 com Retry-After. Respostas de sucesso usam no-store; nenhum dado do paciente aparece na consulta pública. Os 49 JSONs/manifesto e campos existentes foram preservados; o novo seletor é opcional.

## Sync de leitura

BE-08 torna real a última operação da fase 1: as 17 operações estão implementadas. `GET /sync` retorna 200 e `Cache-Control: no-store`. Os 49 exemplos preservam seu formato; não pressupõem igualdade entre versões do envelope e da entidade.

`limite` é global entre as três coleções, de 1 a 1000, padrão 500. Cursor decimal não negativo até bigint, formato inválido, overflow, futuro ou limite inválido retorna 400 `FORMATO_INVALIDO`. A página usa snapshot consistente e ordem global crescente das revisões mais recentes por registro. Com `temMais`, cursor é a última revisão entregue; sem mais, é o marco confirmado do snapshot, mesmo sem mudanças visíveis. O cliente guarda cursor por conta/perfil.

`item.versao` é revisão monotônica da projeção no diário; `item.dados.versao` é versão da entidade clínica para `If-Match`. Nunca envie revisão do envelope como `If-Match`. A projeção do paciente pode trocar para uma nova ficha/vínculo com versão de entidade menor. Tombstone e reaparecimento também avançam a revisão.

Nutri vê fichas/vínculos próprios e seu resumo público. Paciente vê seus vínculos ATIVO/ENCERRADO e os resumos públicos das nutris correspondentes, sem dados privados das fichas. ADMIN recebe 403. ENCERRADO preserva histórico autorizado (RN-VIN-05) e atualiza status; só perda real de visibilidade gera tombstone. Paciente provisório substituído pelo global perde a projeção anterior; o global recebe a projeção atualmente autorizada. Exclusão lógica e restauração são reavaliadas em cada leitura. As projeções são atuais, não eventos com payload histórico.

`cursor` ausente inicia a carga; `limite` padrão 500. Resposta usa `mudancas.pacientes`, `mudancas.vinculos` e `mudancas.nutris`, todas listas, mesmo vazias. Os dados são filtrados por vínculo/dono. Cada item possui `id`, `versao`, `excluido` e `dados` tipados. Perda de visibilidade é tombstone `excluido: true`, `dados: null`; o app apaga sua cópia. Cursor é string decimal opaca para evitar perda de precisão de bigint nos clientes. Persista o cursor de cada página e continue enquanto `temMais`. Coleções de fases futuras serão novas e opcionais para clientes anteriores, que ignoram campos desconhecidos.

## Erros

`ProblemaResposta` modela `type`, `title`, `status`, `detail`, `codigo`, `regra`, `campos`, `traceId`, `atual`. `campos` é sempre lista; `regra`, `traceId` e `atual` podem ser nulos. O cliente decide por `codigo`. `atual` só se aplica ao conflito de versão. Erros técnicos sem regra específica usam `regra: null`; token inválido recebe RN de acordo com confirmação/recuperação, e nenhuma RN inventada para refresh. A conversão de exceções e de validação para esse formato entra em BE-01.

`CodigoErro` preserva os códigos definidos em `integracao.md` e associa os códigos de negócio às RNs pertinentes. A lista das novas entradas da fase está na seção Erros de `integracao.md`.
