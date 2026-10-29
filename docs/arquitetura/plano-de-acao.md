# Dieta+ mobile — plano de ação

Tarefas em ordem de execução. Cada tarefa = **uma branch + um PR** (`feature/MOB-NN-descricao-curta`, saindo de `develop` e voltando para `develop`). Marque `[x]` aqui no mesmo PR.
A ordem entre backend, mobile e design (ondas, marcos e releases) está em `plano-de-execucao.md`. Os JSONs de exemplo de cada fase saem antes, na tarefa `BE-Cn` (contrato da fase) do backend.
Dependências do backend usam o prefixo `BE-` (`dieta-backend/docs/arquitetura/plano-de-acao.md`). Se o endpoint ainda não existe, desenvolva contra o Ktor `MockEngine` com o JSON de exemplo do contrato e deixe o PR dizendo isso.
A parte visual de cada tela (layout, componentes, estados) está em `docs/design/telas.md` e a ordem das telas em `docs/design/plano-de-acao-ui.md`. Este arquivo é a ordem técnica; cada tarefa de tela aqui corresponde às telas listadas lá.

Definição de pronto de toda tarefa:
- Regras do cliente em `dominio/` com teste em `commonTest`; ViewModel com teste.
- Tela funciona nos 3 tamanhos (compacto, médio, expandido) e confere com o canvas.
- Leitura vem do banco local; nada trava sem internet (ou o botão fica desabilitado se a ação exige online).
- `./gradlew :shared:jvmTest :shared:testAndroidHostTest` verde e CI verde (inclui iOS).

## Fase 1 — Fundação do app, contas e vínculo

- [x] **MOB-01 Dependências e estrutura.** Ktor, kotlinx.serialization, coroutines, kotlinx-datetime, Koin, Navigation Compose, SQLDelight, multiplatform-settings, Kermit no `libs.versions.toml`; pacotes de `mobile.md` criados; Koin iniciado no `App.kt`; `Config` com `URL_API` por plataforma.
- [x] **MOB-02 Tipografia e componentes base.** Plus Jakarta Sans em `composeResources`; tipografia no `DietaTheme`; primeiros componentes de `ui/comum` conforme `docs/design/componentes.md` (botões, campos, cartão de refeição, estado vazio, erro, carregando, banner offline). Inclui a Fase 0 de `docs/design/plano-de-acao-ui.md`: `Color.kt` corrigido com todos os tokens de `componentes.md` (o atual diverge do canvas), `Dimens`, `Shapes` e ícones de `ui/comum/icones`. *Depende de:* MOB-01.
- [x] **MOB-03 Navegação e shell adaptativo.** Rotas `@Serializable`; shell por `WindowSizeClass` (bottom nav no compacto, rail + lista-detalhe no médio/expandido); grafos separados para nutri e paciente; janela do desktop (tamanho mínimo, título). *Depende de:* MOB-02.
- [x] **MOB-04 Banco local e fila.** SQLDelight com drivers Android/iOS/JVM; tabelas `sessao`, `cursor_sync`, `mutacao_pendente`, `conflito`; `FilaMutacoes` com teste (gravar + enfileirar na mesma transação). *Depende de:* MOB-01.
- [x] **MOB-05 Cliente da API e sessão.** `ApiDieta` (Ktor) com headers `X-App-Versao`, `X-Plataforma`, `Idempotency-Key`; leitura de ProblemDetail → `ErroApi(codigo, regra, campos)`; plugin `Auth` com refresh; `ArmazenamentoSeguro` expect/actual nas 3 plataformas; logout limpa dados locais; teste de contrato que desserializa os JSONs de `commonTest/resources/contrato/` (copiados do BE-Cn). *Depende de:* MOB-04, BE-03 (até lá, `MockEngine` com BE-C1).
- [x] **MOB-06 Entrar e cadastro da nutri.** Telas de `ui/auth`: entrar, cadastro da nutri (com CRN e termos), esqueci a senha; aviso de e-mail não confirmado; bloqueio de login com mensagem do `codigo`; `RecuperarSenhaScreen` e `EscolherPerfilScreen` (conta que é nutri e paciente, pelos `perfis` do `/eu`). *Depende de:* MOB-03, MOB-05, BE-02 a BE-04.
- [x] **MOB-07 Pacientes da nutri.** `ui/nutri/pacientes` (lista, busca, filtro por status) e `ui/nutri/paciente` (ficha, abas vazias); criar paciente; gerar e compartilhar convite (link + código pelo share nativo); tratar `LIMITE_PACIENTES_ATINGIDO` com aviso de plano. *Depende de:* MOB-06, BE-06, BE-07.
- [x] **MOB-08 Aceitar convite.** Deep link `convite/{codigo}` (Android App Links, iOS Universal Links, esquema `dietamais://`) e campo de código; tela com nutri + termo de consentimento; criar senha ou entrar; erros `CONVITE_EXPIRADO`/`CONVITE_INVALIDO`. *Depende de:* MOB-06, BE-07.

## Fase 2 — Alimentos e dieta

- [x] **MOB-09 Sync de leitura.** `Sincronizador.puxar()` com cursor e `temMais`; aplica mudanças e exclusões no SQLite; `Conectividade` expect/actual; gatilhos (abrir, voltar do fundo, conexão voltou, 15 min); WorkManager no Android; indicador offline no shell. *Depende de:* MOB-04, MOB-05, BE-08.
  - [x] Correção incremental: projetar grupos/alimentos em `alimento_cache` e conservar a base remota por conta/perfil, reaplicando pendências abertas do mesmo alimento após pull, refresh e resultado de envio. Rejeição parcial não desfaz edição já confirmada nem outra pendência. A ordem de múltiplas edições após `manterMinha` permanece backlog separado; este recorte não conclui MOB-10 nem a auditoria integral dos commits externos.
- [ ] **MOB-10 Alimentos da nutri.** `ui/nutri/alimentos`: lista por grupo, busca, criar, editar, desativar (online nesta fase). *Depende de:* MOB-09, BE-10, BE-14.
  - [x] Recorte independente autorizado na onda5: DTOs/HTTP/cache/telas/testes contra BE-C2 aprovado `c35541d`; MockEngine somente em testes, 501 tratado como indisponibilidade. Ver `alimentos-da-nutri.md`.
  - [ ] Integração final: BE-09/catálogo e BE-10/BE-14 implementadas, aprovadas/mergeadas; sincronizar contrato final e fontes reais de sync. Este gate não é liberado pelo recorte independente.
- [x] **MOB-11 Lista de substituição do paciente.** Aba na ficha: remover/adicionar alimentos para aquele paciente. *Depende de:* MOB-10.
- [x] **MOB-12 Montador de dieta.** `ui/nutri/dieta`: refeições (nome, horário, reordenar), itens (buscar alimento, porções), totais em tempo real com `CalculadoraTotais` em `dominio` (mesma fórmula do backend, teste com a dieta de 2509 kcal); salvar rascunho. Layout de duas colunas no expandido. *Depende de:* MOB-10, BE-11.
- [x] **MOB-13 Publicar e duplicar dieta.** Publicar (exige online, confirmação), duplicar, ver histórico de versões; tratar `DIETA_VAZIA`. *Depende de:* MOB-12, BE-12.
- [x] **MOB-14 Hoje e Dieta (paciente).** `ui/paciente/hoje` (refeições do dia, próxima em destaque) e `ui/paciente/dieta` (refeição detalhada), lendo o snapshot local; seletor de dieta em uso quando houver mais de uma nutri; `OrientacoesScreen` e o estado "sua nutri está montando sua dieta" quando não há dieta publicada. *Depende de:* MOB-09, BE-13.
- [x] **MOB-15 Trocar alimento.** `ui/paciente/substituicao`: substitutos do mesmo grupo da lista congelada, com quantidade equivalente (`calcularSubstituto` em `dominio`, com teste); escolher grava check-in TROCADO (local, enviado na fase 3). *Depende de:* MOB-14.

## Fase 2b — Conta da nutri

- [x] **MOB-36 Perfil da nutri.** Parte "meus dados" da `ContaScreen` em `ui/nutri/assinatura`: nome de exibição, CRN, telefone, orientação padrão (usada ao publicar dieta). A parte de assinatura entra em MOB-32. *Depende de:* MOB-07, BE-33.

## Fase 3 — Rotina do paciente

- [x] **MOB-16 Envio da fila.** `Sincronizador.enviar()` com lotes, `idMutacao`, tratamento de APLICADA/REJEITADA/CONFLITO; reenvio com backoff. *Depende de:* MOB-09, BE-15.
- [x] **MOB-17 Check-in e água.** Marcar refeição FEITO/TROCADO/PULADO (até 3 dias para trás), contador de água com meta 35 ml/kg (`AguaSheet`); tudo offline via fila. *Depende de:* MOB-14, MOB-16.
- [x] **MOB-18 Lembretes locais.** `planejarLembretes` em `dominio` (com teste: horários, silêncio, limite de 64 no iOS); `NotificadorLocal` expect/actual (AlarmManager + boot no Android, UNUserNotificationCenter no iOS, bandeja no desktop); ações "Comi/Troquei/Pulei"; pedido de permissão. *Depende de:* MOB-17.
- [ ] **MOB-18b Ações na notificação.** Botões "Comi/Troquei/Pulei" na notificação local (Android `BroadcastReceiver`, iOS `UNNotificationCategory`) gravando o check-in pela fila. A MOB-18 entregou agendamento, silêncio, permissão e toque para abrir; o código de Android e iOS não pôde ser compilado no ambiente da nuvem (sem SDK), confie no CI. *Depende de:* MOB-18.
- [x] **MOB-19 Perfil e preferências.** `ui/paciente/perfil`: `PerfilScreen`, tipos de lembrete, horário de silêncio, `MinhasNutrisScreen` só leitura (vínculos e nutri ativa; troca com cópia de histórico é P2), sair; sincroniza `preferencias-notificacao`. *Depende de:* MOB-18, BE-18.
- [x] **MOB-20 Lista de compras.** `ui/paciente/compras`: `gerarListaCompras(dieta, dias)` em `dominio` (com teste), agrupada por grupo, marcar itens (local), compartilhar como texto. *Depende de:* MOB-14.
- [x] **MOB-21 Agenda do celular.** No perfil: gerar link .ics, botões Google Agenda e Calendário da Apple, revogar link. *Depende de:* MOB-19, BE-17.
- [ ] **MOB-35a Beta interno.** Assinatura de release com secrets no CI, Play Console (teste interno), TestFlight pelo Mac, MSI/DMG como artefato do CI, `URL_API` do beta (BE-32a). Libera o marco M3 com a nutri real. *Depende de:* MOB-17 a MOB-21, BE-32a.

## Fase 4 — Acompanhamento

- [x] **MOB-22 Avaliação.** `ui/nutri/avaliacao`: formulário em seções (medidas básicas, perímetros, dobras, composição, parecer), validação de intervalos (RN-AVA-03), IMC mostrado ao salvar; `HistoricoAvaliacoesScreen` com comparação de duas datas; paciente vê avaliações compartilhadas. *Depende de:* BE-19.
- [x] **MOB-23 Evolução.** `ui/paciente/evolucao` e aba na ficha da nutri: gráficos de linha (peso, % gordura, perímetros, peso em casa) com `Canvas`; `RegistrarPesoSheet` (peso em casa offline pela fila, BE-15). *Depende de:* MOB-22, BE-20.
- [x] **MOB-24 Anamnese da nutri.** `ui/nutri/anamnese`: editar modelo (blocos, perguntas, tipos, condição), enviar ao paciente, ler respostas na ficha. *Depende de:* BE-21, BE-22.
- [x] **MOB-25 Anamnese do paciente.** `ui/paciente/anamnese`: wizard por bloco, salvar parcial (offline), enviar (online); condição de exibição. *Depende de:* MOB-24, BE-22.
- [x] **MOB-26 Agenda de consultas.** `ui/nutri/agenda` (dia/semana, criar, remarcar, cancelar) e próxima consulta no Hoje do paciente. *Depende de:* BE-23.
- [x] **MOB-27 Push e central de avisos (Android e comum).** Registro do token em `POST /eu/dispositivos` (`RegistradorPush`), sync ao receber o push, navegação pela `rota` (`DestinoAviso`) e `ui/notificacoes` com a central de avisos (histórico do sync, marcar como lido). FCM no `androidApp` sem `google-services.json` (propriedades `fcmAppId`, `fcmApiKey`, `fcmProjectId`, `fcmSenderId` do Gradle). O código de Android não pôde ser compilado na nuvem: confie no CI. *Depende de:* BE-24, BE-34.
- [ ] **MOB-27b Push no iOS.** Firebase por Swift Package Manager no `iosApp`, capability Push Notifications, chave APNs no Firebase, `AppDelegate` repassando o token e o toque para `ReceptorPush` (ver mobile.md). Precisa de conta Apple e de um Mac. *Depende de:* MOB-27.
- [x] **MOB-28 Painel da nutri.** `ui/nutri/painel`: consultas do dia, pacientes sem check-in há 3 dias, questionários respondidos, uso do plano. *Depende de:* MOB-26, BE-16.

## Fase 5 — Offline da nutri

- [x] **MOB-29 Escrita offline da nutri** (consultas, avaliações, cadastro de paciente e alimentos; o restante está em MOB-29b). Ficha, alimentos, listas, rascunho de dieta, avaliação, modelos e consultas passam a gravar local + fila (ATUALIZAR/EXCLUIR com `versaoBase`). Ações que exigem online continuam desabilitadas sem rede. *Depende de:* MOB-16, BE-25.
- [ ] **MOB-29b Escrita offline: dietas, listas e modelos.** Rascunho de dieta, lista de substituição e modelo de questionário gravando local + fila (hoje continuam online). Publicar, convidar e enviar questionário seguem exigindo rede. *Depende de:* MOB-29.
- [x] **MOB-30 Conflitos e pendências.** Tela de conflito (lado a lado: minha versão × servidor; "manter a minha" / "usar a do servidor"); contador de pendências e lista do que falta enviar; teste de ponta a ponta com `MockEngine` devolvendo CONFLITO. *Depende de:* MOB-29.
  - [x] Bugfix de idempotência: “manter a minha” cria nova idMutacao com a versão atual do servidor, em transação; regressão com cache por identidade e preservação de pendências/rollback. Demais apontamentos da auditoria são tratados separadamente.
- [x] **MOB-31 Desktop pronto para consultório** (sync a cada 5 min mesmo com a janela na bandeja, menu e balões na bandeja, atalhos Ctrl+R/Ctrl+W/Ctrl+Q). *Depende de:* MOB-29.
- [ ] **MOB-31b Pacotes do desktop no CI.** `nativeDistributions` (MSI, DMG, DEB) como artefato do workflow; exige editar `.github/workflows` (protegido para escrita remota: o Guilherme aplica). *Depende de:* MOB-31.

## Fase 6 — Assinatura, PDF, privacidade e lojas

- [x] **MOB-32 Assinatura.** `ui/nutri/assinatura`: plano, status, uso x limite, aviso de atraso/carência; "Assinar" abre o checkout no navegador externo (nunca compra dentro do app); modo só leitura quando `ASSINATURA_INATIVA`. *Depende de:* BE-26, BE-27.
- [x] **MOB-33 PDF.** Gerar e compartilhar PDF da dieta e da avaliação (download da URL pré-assinada + share nativo). *Depende de:* BE-28. Entregue: o app pede o link assinado e abre no navegador do sistema (baixar e compartilhar ficam com ele); envio da logo fica para MOB-33b.
- [x] **MOB-34 Privacidade.** Termos e política, exportar meus dados, excluir conta (com confirmação). *Depende de:* BE-29.
- [x] **MOB-35 Publicação.** Ícones e splash do design, nome "Dieta+", assinatura de release no CI (secrets), Play Console (teste interno), App Store Connect/TestFlight pelo Mac, `426 VERSAO_APP_OBSOLETA` → tela "atualize o app". *Depende de:* todas as anteriores. Entregue só o `426 VERSAO_APP_OBSOLETA` → tela "atualize o app"; ícones, assinatura de release e lojas seguem em MOB-35a (dependem do Guilherme).

## Fase 7 — Web no navegador (P1)

Substitui o site em Angular (`dieta-frontend`, que fica arquivado). Nutri e paciente, só online, mesmas telas do app. Arquitetura em `web.md`; ordem e paralelismo na onda 16 do `plano-de-execucao.md`. A definição de pronto da web troca "leitura do banco local" por "funciona sem banco local e mostra a faixa offline sem rede" e acrescenta `./gradlew :shared:wasmJsBrowserTest` verde.

- [x] **MOB-40 Alvo web e CI.** `wasmJs { browser() }` no `shared`; módulo `webApp` (só `index.html`, tela de carregamento, "navegador não suportado" e `ComposeViewport`); conferir no `libs.versions.toml` que Ktor (engine JS), Koin, Navigation, `material3-adaptive`, multiplatform-settings, Kermit e Compose Resources têm artefato `wasmJs`; `actual` provisórios em `wasmJsMain`; CI compila `:webApp:wasmJsBrowserDistribution`. Atualizar `mobile.md` e `AGENTS.md` (alvos e comandos) e copiar `visao-geral.md`/`integracao.md` do BE-37. *Depende de:* nada.
- [x] **MOB-41 Separar o que é offline.** *(Feito de forma enxuta: `ApiDieta` recebe `ArmazemSessao` (SQL no app, memória na web) e não depende mais do banco; o source set físico `offlineMain` foi adiado, porque o SQLDelight compila para `wasmJs` e o build de produção descarta o código não usado. Reavaliar no ponto de decisão do MOB-43 com o tamanho real do bundle.)* Source set intermediário `offlineMain` (android, ios, jvm) com SQLDelight, `data/sync`, repositórios com banco, `AmbienteDieta` e `ModuloBancoLocal`; `ApiDieta` deixa de receber `BancoDieta` (interface `ArmazemSessao`, implementação SQLDelight no `offlineMain`). Se o plugin do SQLDelight só gerar código no `commonMain`, o código gerado e o runtime ficam comuns e só os repositórios e o sync mudam de lugar. Sem mudança de comportamento: `jvmTest`, `testAndroidHostTest` e iOS verdes. *Depende de:* MOB-40.
- [ ] **MOB-42 Sessão e plataforma no navegador.** *(Feito: `actual` web, `Plataforma.WEB`, sessão sem refresh no corpo, refresh e logout sem corpo, refresh silencioso ao recarregar, `MockEngine` em `SessaoWebTest`. Falta: `AmbienteWeb` e `moduloWeb`, que dependem das Portas só-API do MOB-43.)* `actual` web de `plataforma/` (tabela em `web.md`); access em memória, refresh pelo cookie (`credentials: include` nas rotas `auth/`, refresh e logout sem corpo), refresh silencioso ao recarregar a página; `X-Plataforma: WEB`; `AmbienteWeb` e `moduloWeb` no Koin. Teste com `MockEngine` (login, refresh, 401, logout). *Depende de:* MOB-41, BE-37.
- [ ] **MOB-43 Base online e telas da nutri (com ponto de decisão).** `RecursoOnline<T>` em `commonMain` com teste; Portas só-API: pacientes, alimentos, lista do paciente, dietas, avaliações, anamnese, consultas, avisos; `PortaPendencias` vazia; `409 CONFLITO_VERSAO` vira mensagem "alterado em outro aparelho" e recarrega. Testes das Portas com `MockEngine` e os JSONs de `commonTest/resources/contrato/`. **Ponto de decisão:** primeiro entregar entrar + pacientes + montador de dieta no navegador e anotar no PR tamanho do bundle (gzip), tempo de abertura em 4G simulado, leitor de tela e autofill de senha. Se o Guilherme reprovar, a fase para aqui e o `dieta-frontend` volta. *Depende de:* MOB-42.
- [ ] **MOB-44 Telas do paciente.** Portas só-API: dieta do paciente, rotina (check-in, água, peso), compras (itens marcados só em memória), evolução, vínculos/perfil, preferências, calendário (.ics). Aviso "instale o app para receber lembretes" no perfil e no Hoje; `NotificadorLocal` sem efeito. *Depende de:* MOB-43.
- [ ] **MOB-45 Navegador e acessibilidade.** URL e botão voltar do navegador ligados à navegação (link direto para `convite/{codigo}`); atalhos de teclado da nutri; ordem de foco e `semantics` nas telas críticas (Entrar, Hoje, montador, ficha); campos de e-mail e senha aceitando o gerenciador de senhas; checklist em Chrome, Firefox, Safari no Mac e no iPhone, nos 3 tamanhos. *Depende de:* MOB-43 (e MOB-44 para as telas do paciente).
- [ ] **MOB-46 Publicação da web.** CI gera `wasmJsBrowserDistribution` como artefato; servir estático pelo Caddy da VPS na mesma origem da API (`/` = app, `/api/v1` = backend), `Content-Type: application/wasm`, cache longo dos arquivos versionados e `no-cache` no `index.html`, CSP; `WEB_ORIGEM` e `CORS_ORIGENS` no `.env.prod`. Landing, planos e checkout ficam em HTML estático fora do app. *Depende de:* MOB-45, BE-32b.

## Pós-MVP (P2)

Troca de nutri com cópia do histórico, vitrine de nutris, consultoria online paga, foto do prato, chat, integração Google Fit/Apple Saúde, anúncios.
