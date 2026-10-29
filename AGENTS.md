# AGENTS.md — dieta-mobile

Instruções para agentes de código (Codex, Claude Code e outros). O `CLAUDE.md` importa este arquivo; edite só aqui.

## O projeto

Dieta+: app para nutricionistas e pacientes (multi-nutri, com assinatura). Este repositório é o **app**: Kotlin Multiplatform + Compose Multiplatform, um código só para **Android, iOS e Desktop**, com perfil nutri e perfil paciente, **offline-first**.
Repositórios irmãos: `guilherme-oliveria/dieta-backend` (API Java/Spring, fonte de verdade das regras) e `guilherme-oliveria/dieta-frontend` (Angular, pós-MVP).
Idioma: **português** em docs, commits, PRs, textos da UI e nomes de domínio. Termos técnicos (ViewModel, repository) ficam em inglês.

## Leia antes de começar

1. `docs/arquitetura/visao-geral.md`: produto, roadmap, decisões e suposições.
2. `docs/arquitetura/plano-de-acao.md`: **a lista de tarefas técnicas**. Pegue a primeira `[ ]` cujas dependências estão `[x]`, a menos que o pedido diga outra.
3. `docs/arquitetura/mobile.md`: camadas, pacotes, bibliotecas, sync, notificações.
4. `docs/arquitetura/integracao.md`: contrato com o backend (auth, erros, sync, push, .ics).
5. `docs/arquitetura/regras-de-negocio.md` e `funcionalidades-mvp.md`: regras `RN-xxx` e critérios de aceite `F-xx`.
6. `docs/design/`: como cada tela deve ficar.

Os arquivos `visao-geral`, `regras-de-negocio`, `integracao` e `funcionalidades-mvp` são **cópias**; a oficial fica em `dieta-backend/docs/arquitetura/`. Não divirja: se precisar mudar, mude lá primeiro.

## Design e telas

Telas e componentes seguem `docs/design/`: tokens e componentes em `docs/design/componentes.md`, mapa tela→pacote em `docs/design/telas.md`, ordem de implementação em `docs/design/plano-de-acao-ui.md`. Antes de criar uma tela, confira o canvas https://claude.ai/artifact/6thej5zGsTo81ZosiigGxC; sem acesso ao claude.ai (Codex e outros), leia a cópia dos artboards em `docs/design/artboards/`.

Cores do tema já estão em `ui/theme` (verde `#2E7D4F`, fundo `#F5F6F1`, laranja `#C2551F` só para alertas; fonte Plus Jakarta Sans).

## Stack

Kotlin 2.4 · Compose Multiplatform 1.12 · Material 3 · AGP 9 · alvos `android`, `iosArm64`, `iosSimulatorArm64`, `jvm` (desktop) e `wasmJs` (web, online-only; ver `docs/arquitetura/web.md`). Pacote `org.br.dietamais` (o backend usa `com.br.dietamais`; não renomeie).
Bibliotecas planejadas (MOB-01): Ktor, kotlinx.serialization, SQLDelight, Koin, Navigation Compose, kotlinx-datetime, multiplatform-settings, Kermit. Versões só pelo `gradle/libs.versions.toml`.

## Comandos

| Ação | Comando |
| --- | --- |
| Testes comuns (rápido, use sempre) | `./gradlew :shared:jvmTest` |
| Testes Android (host) | `./gradlew :shared:testAndroidHostTest` |
| Testes iOS (só no Mac) | `./gradlew :shared:iosSimulatorArm64Test` |
| App Android | `./gradlew :androidApp:assembleDebug` |
| Web (navegador) | `./gradlew :webApp:wasmJsBrowserDevelopmentRun` |
| Build da web | `./gradlew :webApp:wasmJsBrowserDistribution` |
| App desktop | `./gradlew :desktopApp:run` (hot reload: `:desktopApp:hotRun --auto`) |
| iOS | abrir `iosApp/` no Xcode |

## Estrutura

```
shared/src/commonMain/kotlin/org/br/dietamais/
  app/          navegação, shell adaptativo, DI
  ui/theme      tema           ui/comum   componentes
  ui/auth  ui/notificacoes
  ui/paciente/{hoje,dieta,substituicao,compras,anamnese,evolucao,perfil}
  ui/nutri/{painel,pacientes,paciente,avaliacao,dieta,alimentos,anamnese,agenda,assinatura}
  dominio/<area>   regras puras e modelos
  data/{local,remoto,sync,repositorio}
  plataforma/      expect (actual em androidMain, iosMain, jvmMain)
androidApp/  iosApp/  desktopApp/  webApp/   só pontos de entrada e config de plataforma
```

## Regras inegociáveis

- **O backend decide.** Validação no app é só feedback; nunca confie nela como garantia nem replique regra de permissão/plano.
- **Offline-first:** telas leem do SQLDelight. Escrita = banco local + `mutacao_pendente` na mesma transação. IDs novos são UUIDv7 gerados no app. Só chamam a API direto as ações que exigem online (listadas em `integracao.md`).
- **Erros pelo `codigo`** do ProblemDetail, nunca pelo texto.
- **Código comum primeiro.** Nada de `if (plataforma)` em tela. Diferença de plataforma vai para `plataforma/` com `expect/actual`. Layout decide por `WindowSizeClass`, não por plataforma.
- **Segredos e dados:** tokens só no `ArmazenamentoSeguro`. Nunca logue token nem dado de saúde. `google-services.json`, `GoogleService-Info.plist`, keystores e `local.properties` não vão para o Git.
- **Push sem dado clínico** no texto da notificação.
- Ignore campos JSON desconhecidos (`ignoreUnknownKeys = true`) e trate enum desconhecido; o backend pode evoluir antes do app.
- Migração do SQLDelight já publicada não muda; crie outra.

## Convenções

- Tela = `XxxTela.kt` (Composable sem estado, recebe `EstadoXxx` e callbacks) + `XxxViewModel.kt` (expõe `StateFlow<EstadoXxx>`). Previews com dados falsos.
- Strings da UI em `composeResources/values/strings.xml` (português), não no código.
- Regras de cliente (kcal, substituto, lista de compras, lembretes) são funções puras em `dominio/` com teste.
- Siga o estilo do código existente (4 espaços, nomes em português no domínio).

## Fluxo de trabalho

1. Branch a partir da `develop` (Gitflow; ver `docs/arquitetura/plano-de-execucao.md`), com PR de volta para `develop`: `feature/MOB-NN-descricao-curta`. Se o ambiente já definiu uma branch, use a dele.
2. Commits em português no imperativo (`Adiciona tela Hoje do paciente`).
3. Rode `./gradlew :shared:jvmTest` (e `:shared:testAndroidHostTest` se mexeu em Android).
4. PR com `.github/pull_request_template.md`; marque `[x]` em `docs/arquitetura/plano-de-acao.md` no mesmo PR.
5. Se o endpoint do backend ainda não existe, use `MockEngine` com o JSON do contrato e diga no PR.

## Revisão por QA (vale para Codex, Claude e qualquer agente)

Quem escreve o código nunca aprova o próprio trabalho. O fluxo de todo PR de tarefa é:

1. **Agente de código** (Codex ou outro) abre o PR para a `develop` com o ID da tarefa no título (`BE-NN …`, `MOB-NN …`). Ele **nunca** aprova, faz merge, fecha PR alheio ou resolve comentário do QA.
2. **Agente QA** (Claude, um por repositório) revisa o PR contra a tarefa do `plano-de-acao.md`, a definição de pronto deste arquivo, as regras `RN-xxx` citadas, o contrato (`integracao.md` e os JSONs de `contrato/`) e, no mobile, `docs/design/`. Confere também o CI verde. E publica o veredito:
   - **Aprovado:** review "Approve" ou, se o GitHub não deixar (mesma conta), comentário que começa com `QA: APROVADO`. Depois o QA faz o merge na `develop`. Merge na `main` é só do Guilherme.
   - **Corrigir:** review "Request changes" ou comentário que começa com `QA: CORRIGIR`, com a lista do que falta.
3. **Agente de código**, ao receber `QA: CORRIGIR`: corrige na **mesma branch**, faz push, responde cada apontamento dizendo o que mudou e comenta `QA: PRONTO PARA REVISAR`. Discordou de um apontamento? Responde explicando, sem ignorar.
4. **Próxima tarefa só depois do veredito.** Antes de começar qualquer tarefa nova, o agente de código confere os PRs que abriu: com `QA: CORRIGIR` pendente, corrige primeiro; sem veredito ainda, para e avisa "aguardando QA" (pode adiantar só tarefas que não dependem daquele PR, saindo da branch dele).

## Definição de pronto

Testes de `dominio` e ViewModel · tela nos 3 tamanhos e conferida com o canvas · funciona offline (ou desabilita o que exige rede) · CI verde (Android, desktop e iOS).

## Limitações do ambiente na nuvem

No ambiente cloud do Claude, `dl.google.com` (Google Maven) pode estar bloqueado: aí o Gradle nem configura o projeto (o plugin Android vem de lá). Nesse caso, escreva o código e os testes, diga no PR que não rodou localmente e confie no CI do GitHub. Para liberar, o Guilherme adiciona `dl.google.com` e `api.adoptium.net` em Network access do ambiente.
