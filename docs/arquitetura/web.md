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
- MOB-41 enxuto: `ApiDieta` usa `ArmazemSessao` (`ArmazemSessaoSql` no app, `ArmazemSessaoMemoria` na web); o construtor antigo com `BancoDieta` continua. O `offlineMain` físico foi adiado (a web nunca instancia banco, fila ou sync; o DCE do Kotlin/Wasm remove o que não é usado) e só volta se o bundle ficar grande demais.
- Atenção MOB-42: `SessaoLocal.validar` e o refresh hoje exigem `refreshToken` no corpo; na web ele vem só no cookie (BE-37), então a validação e `POST /auth/refresh` (sem corpo, `credentials: include`) precisam de um modo web.
- `FabricaDriver` tem `actual` provisório em `wasmJsMain` (lança erro); MOB-41 o remove ao mover o banco para o `offlineMain`.
- MOB-42: o `fetch` do navegador só manda o cookie do refresh na mesma origem, e o Ktor Js 3.6 não expõe `credentials: include` no código comum. Por isso site e API ficam na mesma origem, também em desenvolvimento (o servidor de dev precisa encaminhar `/api` para o backend; `window.DIETA_URL_API` só serve se a API estiver na mesma origem por outro caminho).

## Medições do ponto de decisão (MOB-43, primeira entrega)

Build de produção (`wasmJsProductionExecutable`), medido na nuvem com `gzip -9`:

| Arquivo | Bruto | gzip |
| --- | --- | --- |
| `dietamais.wasm` (app: login, pacientes, montador, Ktor, Koin, Compose) | 5,95 MB | 1,78 MB |
| `skiko.wasm` (renderização do Compose) | 8,64 MB | 3,33 MB |
| **Total do primeiro carregamento** | 14,6 MB | **cerca de 5,1 MB** |

Não medidos aqui (precisam de um navegador real): tempo de abertura em 4G simulado, leitor de tela e gerenciador de senhas. O `skiko.wasm` é fixo e domina o total; o app cresce com cada tela nova.

## Como a web é construída e publicada (sem Node, sem nginx)

- **Build:** `./gradlew :webApp:wasmJsBrowserDistribution` gera `webApp/build/dist/wasmJs/productionExecutable/`: `index.html`, `styles.css`, `dietamais.js` e os `.wasm`. São só arquivos estáticos; o Node do Gradle só existe na hora de compilar (no CI), nunca no servidor.
- **Servir:** o Caddy que já termina o HTTPS da API na VPS serve esses arquivos (`/srv/web`) e encaminha `/api/*` e `/legal/*` para o backend. Uma origem só, sem CORS na prática e com o cookie do refresh funcionando. Não há contêiner novo (ver `dieta-backend/ops/Caddyfile` e `docs/operacao/deploy-beta.md`).
- **Desenvolvimento:** `./gradlew :webApp:wasmJsBrowserDevelopmentRun` abre `http://localhost:8081` e encaminha `/api` para `http://localhost:8080`. No backend: `WEB_ORIGEM=http://localhost:8081` e `CORS_ORIGENS=http://localhost:8081`.
