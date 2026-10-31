# Dieta+ — comunicação entre as aplicações

> Documento comum aos 3 repositórios. Cópia oficial em `dieta-backend/docs/arquitetura/`.
> **Fonte de verdade do contrato:** o OpenAPI gerado pelo backend (`GET /v3/api-docs`, UI em `/swagger-ui.html` só em dev).
> Este documento define as convenções; o OpenAPI define cada endpoint.
> Mudou o contrato? Backend primeiro, depois mobile e web. Nunca quebre uma versão publicada (ver Versionamento).

## Transporte

- HTTPS em tudo (em dev, `http://localhost:8080`; no emulador Android, `http://10.0.2.2:8080`).
- REST com JSON UTF-8. Prefixo **`/api/v1`**.
- Nomes de campo em **camelCase** português (`dataNascimento`, `limitePacientes`). Enums em MAIÚSCULAS (`PUBLICADA`).
- Datas: instantes em ISO-8601 UTC (`2026-10-24T19:46:53Z`); datas puras `2026-10-24`; horários `08:00`. Fuso do usuário em `usuario.fuso` (padrão `America/Sao_Paulo`) para lembretes e .ics.
- Números decimais como número JSON (`102.5`), dinheiro em string decimal (`"29.90"`).
- IDs são UUID (string). Registros criados offline usam **UUIDv7 gerado no cliente**; o backend aceita o `id` enviado no `POST`/sync e usa `uuidv7()` do Postgres 18 como padrão quando não vem.

## Autenticação

Login próprio, Spring Security + OAuth2 Resource Server (JWT).

| Endpoint | Uso |
| --- | --- |
| `POST /api/v1/auth/cadastro-nutri` | Público desde BE-02: cria nutri + organização + assinatura Teste; 201 sem login automático |
| `POST /api/v1/auth/login` | `{email, senha, dispositivo, perfil?}` → `{accessToken, refreshToken, expiraEm, usuario}` |
| `POST /api/v1/auth/refresh` | `{refreshToken}` → novo par (rotação: o refresh antigo morre) |
| `POST /api/v1/auth/logout` | Revoga o refresh token do dispositivo |
| `POST /api/v1/auth/confirmar-email`, `/esqueci-senha`, `/redefinir-senha` | Fluxos por token de uso único |
| `GET /api/v1/convites/{codigo}` | Público: mostra nutri e termo de consentimento |
| `POST /api/v1/convites/{codigo}/aceitar` | Cria conta de paciente (se preciso) + consentimento + login |
| `GET /api/v1/eu` | Usuário logado, perfil, assinatura (nutri), vínculos (paciente) |

- **Access token**: JWT assinado (RS256 ou EdDSA), validade **15 min**, claims `sub` (usuario_id), `perfil`, `nid` (nutricionista_id, se nutri), `pid` (paciente_id, se paciente). Header `Authorization: Bearer <token>`.
- **Refresh token**: opaco, aleatório (32 bytes), validade **30 dias**, guardado com hash no banco por dispositivo, **rotativo** (reuso de um refresh antigo revoga a família inteira).
- Os claims ajudam o roteamento, mas **o backend sempre confere no banco** vínculo, dono e status da assinatura.
- Mobile guarda tokens em armazenamento seguro (Keystore/Keychain/arquivo cifrado no desktop). Web guarda o access em memória e o refresh em cookie `HttpOnly; Secure; SameSite=Strict` no path `/api/v1/auth` (o backend aceita os dois modos).
- Rate limit: login e convites 10/min por IP; API geral 300/min por usuário. Excedeu: `429` com `Retry-After`.

BE-03 implementa login, refresh e logout sem alterar os campos dos exemplos. O app envia refresh no JSON e omite o header Authorization nessas rotas públicas (um Bearer expirado é rejeitado pelo resource server). Access usa RS256; refresh tem prazo absoluto de família de 30 dias, com rotação, revogação da família por reuso e logout por dispositivo. Respostas usam `Cache-Control: no-store`. Detalhes de bloqueio RN-CON-03, cookie seguro com validação de Origin e limite por IP estão em `contrato-fase-1.md`. A proteção por e-mail é persistente; o limite por IP desta etapa é local ao processo. Rate limit de convites e geral entram nas tarefas correspondentes.

BE-04 implementa confirmação de e-mail (token de uso único, 24 h) e recuperação/redefinição (uma hora). O cadastro enfileira a confirmação; os links abrem o app e o app envia o POST já contratado. `esqueci-senha` retorna sempre 202 sem corpo para e-mail válido, inclusive desconhecido/inativo ou em falha interna, sem enumerar contas. Redefinição consome o token, aplica RN-CON-02 e revoga todos os refresh do usuário, sem login automático. O payload da outbox é cifrado enquanto pendente e apagado após envio/expiração/falha definitiva. Envio SMTP depende de configuração do servidor; detalhes e limites estão em `contrato-fase-1.md`. JSONs/códigos da BE-C1 são preservados.

## Erros

BE-36 acrescenta `TERMOS_INDISPONIVEIS` e `TERMOS_NAO_ACEITOS` (422, RN-CON-09). A fonte interna dos termos de uso é consultada publicamente por `GET /legal/terms-of-use`: versão, estado, aprovação/publicação, `cadastroHabilitado`, datas e conteúdo Markdown. Cadastros de nutri e de paciente exigem aceite explícito da versão aprovada e publicada; consentimento clínico é separado. O documento inicial 1.0.0 está em RASCUNHO e mantém cadastro indisponível; login e fluxos existentes continuam normais. Detalhes/campos opcionais do cadastro paciente em [contrato-fase-1.md](contrato-fase-1.md).

Formato **RFC 9457 Problem Details** (`application/problem+json`), já nativo no Spring:

```json
{
  "type": "https://dietamais.com.br/erros/limite-pacientes-atingido",
  "title": "Limite de pacientes atingido",
  "status": 422,
  "detail": "Seu plano permite 20 pacientes ativos.",
  "codigo": "LIMITE_PACIENTES_ATINGIDO",
  "regra": "RN-ASS-03",
  "campos": [{ "campo": "email", "codigo": "OBRIGATORIO", "mensagem": "Informe o e-mail" }],
  "traceId": "4bf92f3577b34da6"
}
```

- Os clientes decidem o que mostrar **pelo `codigo`**, nunca pelo texto.
- `400` JSON inválido · `401` sem token/expirado (cliente tenta refresh uma vez) · `403` sem permissão · `404` não existe **ou não é seu** (não revelar existência) · `409` conflito de versão/duplicado · `422` regra de negócio · `429` limite de taxa · `5xx` erro do servidor (cliente pode repetir com a mesma `Idempotency-Key`).

Códigos já definidos: `LOGIN_BLOQUEADO`, `CREDENCIAIS_INVALIDAS`, `EMAIL_NAO_CONFIRMADO`, `EMAIL_JA_CADASTRADO`, `TOKEN_INVALIDO`, `LIMITE_PACIENTES_ATINGIDO`, `ASSINATURA_INATIVA`, `CONVITE_INVALIDO`, `CONVITE_EXPIRADO`, `VINCULO_JA_EXISTE`, `DIETA_VAZIA`, `DIETA_NAO_EDITAVEL`, `VALOR_FORA_DO_INTERVALO`, `DADO_INSUFICIENTE_CALCULO`, `PROTOCOLO_INVALIDO`, `JUSTIFICATIVA_OBRIGATORIA`, `QUESTIONARIO_FECHADO`, `CONSULTA_CANCELADA`, `ASSINATURA_JA_ATIVA`, `PAGAMENTO_INDISPONIVEL`, `EXPORTACAO_EM_ANDAMENTO`, `VINCULO_ATIVO`, `VERSAO_APP_OBSOLETA`, `CONFLITO_VERSAO`, `OBRIGATORIO`, `FORMATO_INVALIDO`, `CONSENTIMENTO_OBRIGATORIO`, `PERFIL_INVALIDO`, `RECURSO_NAO_ENCONTRADO`, `NAO_AUTENTICADO`, `ACESSO_NEGADO`, `TAXA_EXCEDIDA`, `NAO_IMPLEMENTADO`, `ERRO_INTERNO`, `METODO_NAO_PERMITIDO`, `TIPO_CONTEUDO_NAO_SUPORTADO`, `CHAVE_IDEMPOTENCIA_REUTILIZADA`. Novo código: adicione aqui e na classe `CodigoErro` do backend.

Códigos acrescentados em BE-C1: `CONSENTIMENTO_OBRIGATORIO` (422, RN-VIN-04), `PERFIL_INVALIDO` (403), `RECURSO_NAO_ENCONTRADO` (404, inclusive recurso de outro dono), `NAO_AUTENTICADO` (401), `ACESSO_NEGADO` (403), `TAXA_EXCEDIDA` (429) e `NAO_IMPLEMENTADO` (501). `campos` é sempre lista; `regra`, `traceId` e `atual` podem ser nulos. Conflito de versão traz o registro vigente em `atual`. O ID de regra de `TOKEN_INVALIDO` depende da operação (confirmação ou recuperação); erros técnicos sem RN usam `regra: null`.

BE-01 acrescenta `ERRO_INTERNO` (500), `METODO_NAO_PERMITIDO` (405), `TIPO_CONTEUDO_NAO_SUPORTADO` (415) e `CHAVE_IDEMPOTENCIA_REUTILIZADA` (409, mesma chave com outra requisição). São códigos técnicos, com `regra: null`. Erros inesperados nunca expõem mensagem interna, corpo da requisição ou valor rejeitado.

### Contrato da fase 1

Contrato completo da fase 1: [contrato-fase-1.md](contrato-fase-1.md). BE-C1 publica 17 operações, records e exemplos de sucesso/erro em `src/test/resources/contrato/manifesto.json`; após BE-08, as dezessete operações são reais, incluindo GET /sync. `GET /eu` lê a conta, assinatura da própria organização no perfil NUTRICIONISTA ou vínculos próprios no perfil PACIENTE. GET/POST `/pacientes` e GET/PATCH `/pacientes/{id}` usam ficha própria da nutri, limite de organização e `If-Match` para edição; adesão permanece nula. Toda chamada autenticada valida usuário ativo, perfil e `nid`/`pid` no banco antes da idempotência; identidade revogada ou divergente retorna 401 `NAO_AUTENTICADO`. Perfis múltiplos usam `perfis` disponíveis e `perfil` ativo em `usuario`, com `NUTRICIONISTA` no JSON/JWT; login com `perfil` opcional foi implementado em BE-03. Os campos e os 49 exemplos JSON foram preservados.

BE-02 substitui apenas o stub de cadastro da nutri. Requisição e resposta mantêm os mesmos campos e JSONs de exemplo; os detalhes de normalização, limites de armazenamento e erros de validação estão em `contrato-fase-1.md`. A conta nasce com e-mail não confirmado, CRN não verificado e assinatura de teste cuja duração/limite vêm do banco. A confirmação e o envio de e-mail continuam em BE-04; não há sessão/token na resposta do cadastro.

## Convenções REST

- Recursos no plural, em português: `/api/v1/pacientes`, `/api/v1/pacientes/{id}/avaliacoes`, `/api/v1/dietas/{id}/publicar` (ação = `POST` em subrecurso verbal).
- Nutri acessa por `/api/v1/pacientes/...` (só os vinculados a ela). Paciente acessa o que é dele por `/api/v1/eu/...` (`/eu/dietas`, `/eu/checkins`).
- Paginação: `?pagina=0&tamanho=20&ordem=nome,asc` → `{ "itens": [], "pagina": 0, "tamanho": 20, "total": 57 }`. Máximo 100 por página.
- Escrita concorrente: `PUT`/`PATCH` exigem `If-Match: "<versao>"` (ou campo `versao` no corpo). Versão diferente → `409 CONFLITO_VERSAO` com o registro atual no corpo (`atual`). Resposta de leitura traz `ETag: "<versao>"`.
- **Idempotência**: todo `POST` que cria algo aceita `Idempotency-Key: <uuid>` opcional. Mobile sempre envia (é o que torna seguro reenviar a fila offline). A chave é isolada pelo usuário/perfil autenticado (ou pelo escopo público), e o hash inclui método, rota, query, tipo de conteúdo e corpo. Repetição idêntica em 24 h devolve status/corpo/headers funcionais originais, com novo rastreio da requisição. Chave reutilizada com conteúdo diferente retorna 409. Só respostas 2xx são armazenadas; erros e stubs 501 não são cacheados. A reserva da chave, a operação e a resposta são gravadas na mesma transação; um erro desfaz a operação e libera a chave. O prazo é configurável. O banco guarda apenas hash da requisição, nunca seu corpo; a resposta persistida é dado restrito e acompanha o isolamento da chave.

  O corpo armazenado para replay é cifrado, inclusive quando a resposta contém uma sessão. `IDEMPOTENCIA_CHAVE` precisa estar configurada no servidor antes de operações idempotentes reais; sua ausência desfaz a operação e responde `ERRO_INTERNO`, sem fallback para armazenamento em texto puro. A chave nunca é enviada pelo cliente.

- **Rastreio**: o backend devolve `X-Trace-Id` em respostas, inclusive erros; clientes logam junto com o erro. Valores recebidos só são reaproveitados se tiverem 16 a 64 caracteres hexadecimais. Caso contrário, o servidor gera outro identificador.

## Mapa de endpoints do MVP

Contrato da fase 2: [contrato-fase-2.md](contrato-fase-2.md), com 17 operações e exemplos tipados em `contrato/fase-2/`. Após BE-10/BE-33, grupos/alimentos/lista do paciente e perfil são reais; oito operações de dietas continuam 501 até BE-11..13. Grupos são cópias próprias com campos opcionais macrosEstimados/versao/origemMacros, editáveis via PATCH /grupos-alimentos/{id} e If-Match. POST de alimento é idempotente, PATCH edita/desativa; lista do paciente usa If-Match próprio e auditoria clínica. Fonte/método inicial em [catálogo](catalogo-base.md). Exemplos legados continuam válidos; a fixture de desativação foi alinhada a ativo=false. Não há escrita global ou propagação para outras nutris. Perfil usa nome de exibição próprio, sem telefone/orientação nos resumos públicos. Fase 1 e GET /sync permanecem implementados; novas fontes de alimentos/listas só entram em BE-14.

Visão geral para planejar; detalhe e schema no OpenAPI.

| Módulo | Endpoints principais | Quem |
| --- | --- | --- |
| contas | `/auth/*`, `/eu`, `/eu/dispositivos` (registra token FCM), `/eu/preferencias-notificacao` | todos |
| assinaturas | `GET /assinatura`, `POST /assinatura/checkout` (link do gateway), `POST /webhooks/pagamento` (público, validado) | nutri / gateway |
| pacientes | `GET/POST /pacientes`, `GET/PATCH /pacientes/{id}`, `POST /pacientes/{id}/convite`, `POST /pacientes/{id}/encerrar-vinculo` | nutri |
| alimentos | `GET /grupos-alimentos`, `GET/POST/PATCH /alimentos` (lista da nutri), `GET/PUT /pacientes/{id}/lista-substituicao` | nutri |
| dietas | `GET/POST /pacientes/{id}/dietas`, `GET/PUT /dietas/{id}`, `POST /dietas/{id}/publicar`, `POST /dietas/{id}/duplicar`, `GET /eu/dietas`, `PUT /eu/dieta-em-uso` | nutri / paciente |
| rotina | `POST /eu/checkins`, `POST /eu/agua`, `POST /eu/pesos`, `GET /eu/rotina?de=&ate=` (BE-38: registros do período, até 366 dias, no formato do `/sync`; usado pela web), `GET /pacientes/{id}/adesao` | paciente / nutri |
| avaliacoes | `GET/POST /pacientes/{id}/avaliacoes`, `PUT /avaliacoes/{id}`, `GET /eu/avaliacoes`, `GET /eu/evolucao` | nutri / paciente |
| questionarios | `GET/POST /modelos-questionario`, `POST /pacientes/{id}/questionarios` (envia), `GET /eu/questionarios`, `PUT /eu/questionarios/{id}/respostas`, `POST /eu/questionarios/{id}/enviar` | nutri / paciente |
| notificacoes | `GET /eu/avisos`, `POST /eu/avisos/{id}/lido`, `POST /eu/avisos/lidos` (central de avisos) | nutri / paciente |
| agenda | `GET/POST /consultas`, `PATCH /consultas/{id}`, `GET /eu/consultas`, `POST /eu/calendario/token` (gera/revoga link .ics) | nutri / paciente |
| relatorios | `GET /dietas/{id}/pdf`, `GET /avaliacoes/{id}/pdf`, `POST /eu/exportacao` | nutri / paciente |
| sync | `GET /sync`, `POST /sync` | app |

## Sincronização offline (app ↔ backend)

O app é offline-first: as telas leem **só do SQLite local**; um `Sincronizador` conversa com o backend em segundo plano.

### Puxar mudanças — `GET /api/v1/sync?cursor=<cursor>&limite=500`

- BE-08 implementa `pacientes`, `vinculos` e `nutris`; BE-14 acrescenta `grupos`, `alimentos`, `listas`, `dietas` (nutri), `snapshots` e `dietaEmUso` (paciente) — ver [contrato-fase-2.md](contrato-fase-2.md). O diário de revisões por destinatário guarda somente metadados e é atualizado na mesma transação da alteração. A revisão só fica disponível junto com o commit.
- Resposta:

```json
{
  "cursor": "184467",
  "temMais": false,
  "mudancas": {
    "pacientes":   [{ "id": "…", "versao": 4, "excluido": false, "dados": { … } }],
    "vinculos":    [],
    "nutris":      []
  }
}
```

- `cursor` vazio = carga inicial completa (paginada por `temMais`). O app guarda o último `cursor` e repete até `temMais=false`.
- NUTRICIONISTA recebe fichas e vínculos próprios e seu resumo público; PACIENTE recebe seus vínculos ATIVO/ENCERRADO e resumos públicos das respectivas nutris, sem ficha clínica privada. ADMIN recebe 403. ENCERRADO conserva histórico autorizado e atualiza status (RN-VIN-05), sem tombstone.
- Perda real de visibilidade, como exclusão lógica ou substituição do paciente provisório pelo global no aceite, gera `excluido: true`, `dados: null`. Restauração gera nova revisão com dados. As projeções são atuais, não histórico de payloads.
- `versao` do envelope é revisão monotônica da projeção no diário; `dados.versao` é versão da entidade para `If-Match`. Não há igualdade entre elas. Uma nova ficha do mesmo paciente pode ter versão de entidade menor; a revisão do envelope continua maior. Nunca use revisão do diário como `If-Match`.
- `limite` vale para a soma das três coleções, entre 1 e 1000, padrão 500. A página é escolhida pela ordem global crescente das revisões mais recentes por registro no snapshot, antes de separar as listas. Combine as listas pela revisão quando precisar da ordem global.
- Cursor é decimal não negativo até bigint. Formato inválido, overflow, futuro ou limite inválido retorna 400 `FORMATO_INVALIDO`. Com `temMais`, aponta à última revisão entregue; sem mais, ao marco confirmado do snapshot, inclusive em página vazia ou com mudanças somente de outro usuário. Não use o maior `seq` de uma tabela como cursor. Guarde cursor por conta/perfil e reinicie a carga ao trocar identidade ou limpar o banco local. O servidor reaplica autorização em toda leitura.
- Quando puxar: ao abrir o app, ao voltar do segundo plano, ao receber push `sync`, a cada 15 min em primeiro plano e após cada envio.

### Enviar mudanças — `POST /api/v1/sync`

```json
{
  "mutacoes": [
    { "idMutacao": "uuid", "entidade": "avaliacoes", "operacao": "CRIAR", "id": "uuidv7", "versaoBase": null, "dados": { … } },
    { "idMutacao": "uuid", "entidade": "dietas", "operacao": "ATUALIZAR", "id": "…", "versaoBase": 3, "dados": { … } }
  ]
}
```

Resposta, um resultado por mutação, na mesma ordem:

```json
{ "resultados": [
  { "idMutacao": "…", "status": "APLICADA", "versao": 1 },
  { "idMutacao": "…", "status": "CONFLITO", "codigo": "CONFLITO_VERSAO", "atual": { "versao": 5, "dados": { … } } },
  { "idMutacao": "…", "status": "REJEITADA", "codigo": "ASSINATURA_INATIVA", "regra": "RN-ASS-04" }
]}
```

- Implementado na BE-15: `checkins`, `agua` e `pesos` com `CRIAR` (ver [contrato-fase-3.md](contrato-fase-3.md)); `GET /sync` ganha as coleções `checkins`, `agua`, `pesos`. Escrita da nutri (`pacientes`, `alimentos`, `listas`, `dietas`, `avaliacoes`, `modelos`, `consultas`): ver [contrato-fase-5.md](contrato-fase-5.md). Assinatura, checkout, PDF, LGPD e admin (fase 6): [contrato-fase-6.md](contrato-fase-6.md).
- `operacao`: `CRIAR`, `ATUALIZAR`, `EXCLUIR` (lógica). Até 200 mutações por lote. Cada mutação roda na sua própria transação.
- `idMutacao` torna o envio idempotente: reenviar a mesma mutação no mesmo usuário/perfil e com o mesmo conteúdo devolve o mesmo resultado, sem reaplicar. A identidade inclui `entidade`, `operacao`, `id`, `versaoBase` e `dados`; ordem das propriedades JSON, espaços e escala numérica equivalente não alteram o conteúdo, mas ordem de arrays altera. O servidor usa HMAC com a chave de idempotência já configurada; não grava o corpo da requisição como fingerprint. Reuso em outro usuário/perfil ou com conteúdo diferente retorna `REJEITADA`/`CHAVE_IDEMPOTENCIA_REUTILIZADA`, sem `atual`, versão ou conteúdo do resultado guardado. O lote continua retornando resultados por item.
- O bloqueio transacional por `idMutacao` serializa lookup/aplicação/gravação; o resultado confirmado primeiro determina o replay. Falha de regra desfaz a operação e reconsulta a reserva antes de guardar a rejeição, sem sobrescrever um resultado confirmado por concorrente. Falha em reservar a chave desfaz a operação em vez de devolver uma aplicação não registrada.
- Compatibilidade da migração 029: resultados antigos sem perfil/fingerprint permanecem armazenados, mas não podem ser autenticados para replay e retornam a mesma rejeição de reuso, sem reexecutar. O cliente deve reconciliar o estado da entidade antes de criar nova intenção com outro ID; não reenviar cegamente uma inclusão com novo ID. A ausência de `IDEMPOTENCIA_CHAVE` impede a escrita antes de executar a mutação (`ERRO_INTERNO`); não há fingerprint sem autenticação nem associação oportunista do legado ao perfil atual.
- `APLICADA` → o app marca como enviada e atualiza `versao`. `CONFLITO` → guarda o `atual` e mostra a escolha à nutri ("manter o meu" reenvia com `versaoBase` = versão atual; "usar o do servidor" descarta a local). `REJEITADA` → desfaz localmente e mostra a mensagem do `codigo`.
- Os endpoints REST normais continuam valendo para telas online e para o site; o sync é o caminho do app.

### O que cada perfil sincroniza

| Perfil | Puxa | Envia |
| --- | --- | --- |
| Paciente | Seus vínculos, nutris, dietas publicadas (com snapshot da lista de substituição e orientação), avaliações compartilhadas, questionários, consultas, avisos, preferências | Check-ins, água, peso, respostas de questionário, dieta em uso, preferências |
| Nutri | Seus pacientes, fichas, lista de alimentos, listas por paciente, dietas (rascunhos e publicadas), avaliações, modelos e respostas de questionário, consultas, avisos, adesão (check-ins dos últimos 30 dias) | Tudo que ela edita, exceto publicar dieta, convidar paciente e ações de assinatura, que exigem estar online |

Ações que **exigem online** (o app desabilita o botão sem conexão): login/cadastro, convidar paciente, aceitar convite, publicar dieta, enviar questionário, gerar PDF, tudo de assinatura.

## Push (FCM)

- O app registra o token em `POST /api/v1/eu/dispositivos` `{token, plataforma: ANDROID|IOS|DESKTOP|WEB, nomeAparelho}`. Desktop não tem FCM: usa só o sync periódico.
- Payload sempre genérico (RN-NOT-02): `{ "tipo": "NOVA_DIETA", "sync": true, "rota": "paciente/dieta" }`. O app ao receber faz um sync e navega pela `rota`.
- Backend envia via fila na tabela `aviso`: o aviso é gravado logo após o commit do evento (e por jobs de lembrete); um job `@Scheduled` (a cada 30 s) envia, respeita preferências e silêncio e tenta até 5 vezes. Detalhes em `contrato-fase-4.md` (BE-24).

## Agenda Google/Apple (.ics)

- O paciente gera um link secreto em `POST /api/v1/eu/calendario/token` → `https://api.dietamais.com.br/cal/<token>.ics`. Revogar gera outro.
- `GET /cal/{token}.ics` é público (o token é a credencial, 32 bytes aleatórios), `text/calendar`, com um `VEVENT` recorrente diário (`RRULE:FREQ=DAILY`) por refeição da dieta em uso, mais as consultas. `UID` estável por refeição para o calendário atualizar sem duplicar.
- O app oferece botões "Adicionar ao Google Agenda" (abre `https://calendar.google.com/calendar/r?cid=webcal://…`) e "Adicionar ao Calendário da Apple" (abre `webcal://…`).
- Título genérico ("Café da manhã — Dieta+"), sem quantidades, porque o feed é acessível a quem tiver o link.

## Pagamento (fase 6)

- Gateway atrás da interface `GatewayPagamento` no backend (implementação inicial: Asaas `[SUPOSIÇÃO S3]`).
- `POST /api/v1/assinatura/checkout` devolve a URL do checkout hospedado; o app abre no navegador externo (nunca compra dentro do app).
- `POST /api/v1/webhooks/pagamento`: valida o token/assinatura do gateway, grava o evento em `evento_pagamento` com `evento_id` único (ignora repetidos) e atualiza a assinatura.

## Arquivos

- PDFs e fotos ficam num storage compatível com S3 (MinIO em dev). O backend devolve **URL pré-assinada** de 5 min; nada de arquivo público.
- Upload (foto do prato, P2): `POST /api/v1/eu/fotos` devolve URL pré-assinada de upload.

## Versionamento do contrato

- Mudança **compatível** (campo novo opcional, endpoint novo, valor de enum novo) entra em `/api/v1`. Clientes **ignoram campos desconhecidos** e tratam enum desconhecido como "outro".
- Mudança **incompatível** (remover/renomear campo, mudar tipo, mudar significado) exige `/api/v2` daquele recurso, mantendo o v1 por pelo menos 6 meses (apps antigos nas lojas).
- O app envia `X-App-Versao: 1.4.0` e `X-Plataforma: ANDROID` (`ANDROID|IOS|DESKTOP|WEB`; a web, em KMP, manda `WEB`). Abaixo da versão mínima configurada (`APP_VERSAO_MINIMA`; para `X-Plataforma: WEB` vale `APP_VERSAO_MINIMA_WEB`, vazia = sem bloqueio), o backend responde `426 VERSAO_APP_OBSOLETA` em qualquer rota de `/api/v1` (menos webhooks e links públicos de arquivo); o app mostra a tela de atualização e não repete a chamada. Sem o cabeçalho não há bloqueio. Limite geral: `429 TAXA_EXCEDIDA` com `Retry-After` (segundos) acima de 300 requisições por minuto por usuário; o app espera o `Retry-After` e repete (leitura) ou mantém a mutação na fila (escrita).

## Como os clientes consomem o contrato

| Cliente | Como |
| --- | --- |
| Mobile (KMP) | DTOs escritos à mão em `shared/.../data/remoto/dto` com `kotlinx.serialization`, espelhando o OpenAPI; cliente Ktor. Teste de contrato: JSONs de exemplo em `commonTest/resources/contrato/` copiados do backend. |
| Web (KMP, alvo `wasmJs` do `dieta-mobile`) | Mesmo `ApiDieta` e DTOs do app, online-only. Sessão no navegador (BE-37): pedido com `Origin` igual a `WEB_ORIGEM` (login, refresh) recebe o `refreshToken` **só no cookie** `HttpOnly` (o corpo vem sem o campo) e o access fica em memória; `refresh` e `logout` vão sem corpo, com `credentials: include`. CORS libera `X-Plataforma`. O cliente Angular gerado (`dieta-frontend`) não será usado. |
| Backend | `springdoc-openapi` gera o documento. Exemplos JSON de cada DTO ficam em `src/test/resources/contrato/` e um teste garante que eles continuam válidos. |
