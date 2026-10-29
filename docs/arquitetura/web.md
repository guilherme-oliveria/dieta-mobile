# Dieta+ — web no navegador (Compose para web)

> Decisão de 29/10/2026 (Guilherme): a versão web sai deste repositório, como alvo `wasmJs` do Kotlin Multiplatform, e não mais do `dieta-frontend` (Angular). Tarefas na Fase 7 do `plano-de-acao.md` (MOB-40 a MOB-46) e BE-37 no backend.
> Este arquivo diz como a web se encaixa no app. O resto da arquitetura continua em `mobile.md`.

## O que a web é

| | Web | App (Android, iOS, Desktop) |
| --- | --- | --- |
| Perfis | Nutri e paciente | Nutri e paciente |
| Dados | **Só online**: lê e escreve direto na API | Offline-first: SQLDelight + fila + `/sync` |
| Sessão | Access token em memória; refresh em cookie `HttpOnly` (já emitido pelo backend) | `ArmazenamentoSeguro` (Keystore, Keychain, arquivo cifrado) |
| Lembretes de refeição e água | Não (aviso "instale o app para receber lembretes") | Sim (`NotificadorLocal`) |
| Push | Fora do escopo inicial | FCM |
| Conflito de versão | `409 CONFLITO_VERSAO` tratado na hora: mensagem e recarregar | Fila + tela de conflito (MOB-30) |

O Postgres do backend continua sendo a única fonte de verdade. O navegador nunca guarda dado de saúde em disco (nada de SQLite/OPFS, IndexedDB ou `localStorage` com dado clínico).

## Como o código se divide

As telas e os ViewModels já dependem só das interfaces `PortaXxx` de `data/repositorio/`. A web reaproveita telas, ViewModels, `dominio/`, `ApiDieta`, DTOs e recursos sem mudança; só troca as implementações das Portas e os `actual` de `plataforma/`.

```
commonMain     ui/, dominio/, Portas (interfaces), ApiDieta, DTOs, RecursoOnline, repositórios que já são só API
offlineMain    (android + ios + jvm) SQLDelight, data/sync, repositórios com banco, AmbienteDieta, ModuloBancoLocal
wasmJsMain     actual de plataforma/, Portas só-API (PacientesOnline, ...), AmbienteWeb, moduloWeb (Koin)
webApp/        index.html, carregamento, ComposeViewport (só ponto de entrada, como androidApp e desktopApp)
```

- `ApiDieta` deixa de receber `BancoDieta`. A sessão passa por uma interface `ArmazemSessao`: no `offlineMain` grava na tabela `sessao` e limpa os caches no logout; na web fica em memória.
- `RecursoOnline<T>` (commonMain) é a base das Portas web: `StateFlow` com a lista, `atualizar()` por `GET`, escrita com `If-Match`/`versao`, `409` vira erro de conflito para a tela. Cada Porta web é uma classe fina sobre ele.
- Os 6 repositórios que já não usam banco (autenticação, convites, assinatura, PDF, perfil da nutri, privacidade) são os mesmos nas duas montagens.
- `PortaPendencias` na web devolve sempre vazio (não há fila).

## Plataforma na web

| `expect` | `actual` em `wasmJsMain` |
| --- | --- |
| `motorHttp()` | Engine JS do Ktor (Fetch) com `credentials: "include"` só nas rotas `auth/` |
| `plataformaApi()` | `Plataforma.WEB` (`X-Plataforma: WEB`, BE-37) |
| `ArmazenamentoSeguro` | Em memória; o refresh fica no cookie e o JavaScript não o lê |
| `Conectividade` | `navigator.onLine` + eventos `online`/`offline` |
| `Compartilhador` | Web Share API quando existir; senão copiar para a área de transferência |
| `NotificadorLocal` | Sem efeito |
| `IdentidadeDispositivo` | UUID em `localStorage` (não é dado de saúde) |
| `urlApiPadrao()` | Mesma origem do site (`/api/v1`), configurável no build |
| `FabricaDriver` | Não existe na web (fica no `offlineMain`) |

Renovar a sessão na web: `POST /auth/refresh` sem corpo; o backend lê o cookie e confere `Origin` com `WEB_ORIGEM`. Recarregar a página = refresh silencioso; se falhar, volta para Entrar.

## Limites conhecidos

- Navegador com WasmGC: Chrome/Edge 119+, Firefox 120+, Safari 18.2+ (iOS 18.2+). Fora disso, `index.html` mostra a tela "navegador não suportado".
- Primeiro carregamento de alguns MB: tela de carregamento no `index.html`, cache forte dos `.wasm` com nome versionado.
- Sem SEO: o app é desenhado em canvas. Landing, planos e checkout ficam numa página HTML estática à parte.
- Acessibilidade e autofill de senha são mais fracos que num site em HTML: MOB-45 confere leitor de tela, teclado e gerenciador de senhas.

## Comandos (a partir de MOB-40)

| Ação | Comando |
| --- | --- |
| Rodar no navegador | `./gradlew :webApp:wasmJsBrowserDevelopmentRun` |
| Testes comuns no navegador | `./gradlew :shared:wasmJsBrowserTest` |
| Build de produção | `./gradlew :webApp:wasmJsBrowserDistribution` (sai em `webApp/build/dist/wasmJs/productionExecutable/`) |

## Achados na implementação (MOB-40)

- Todas as bibliotecas do `commonMain` (Ktor, Koin, Navigation, `material3-adaptive`, SQLDelight runtime, multiplatform-settings, Kermit, Compose Resources) compilam para `wasmJs` sem troca de versão; só entrou `ktor-client-js`.
- `Plataforma` ganhou `WEB` (o backend aceita desde o BE-37). O CORS do backend agora libera o cabeçalho `X-Plataforma`; sem isso o preflight do navegador falharia.
- Na nuvem do Claude, `:webApp:wasmJsBrowserDistribution` não roda (o yarn baixa o `karma` de `codeload.github.com`, bloqueado). Lá vale `:webApp:compileProductionExecutableKotlinWasmJs`; o CI do GitHub faz a distribuição completa.
- `FabricaDriver` tem `actual` provisório em `wasmJsMain` (lança erro); MOB-41 o remove ao mover o banco para o `offlineMain`.
