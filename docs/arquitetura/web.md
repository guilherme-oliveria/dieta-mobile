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
- Sem fila em disco: as respostas parciais da anamnese do paciente aparecem na hora e seguem para a API em ordem; se a aba fechar antes de chegarem, perdem-se. Concluir só vale com tudo já enviado (RN-ANA-03).
- Sem push: a central de avisos recarrega ao abrir e a cada minuto, enquanto a aba está aberta.
- Sem sync, a nutri não vê os pesos que o paciente registra em casa (não há endpoint para ela); o gráfico de evolução da web mostra só as avaliações. O painel mostra só os questionários que a nutri já abriu na sessão.
- Link do convite: `/convite/{codigo}` abre o convite (o servidor precisa devolver `index.html` para qualquer caminho, como o Caddy faz com `try_files`; o servidor de desenvolvimento usa `historyApiFallback`). O `index.html` tem `<base href="/">` para o JS e o CSS carregarem de qualquer caminho.
- Atalhos: Alt+1 a Alt+5 trocam de aba, na ordem da navegação.

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

## Achados do e2e (A4, 29/10/2026)

Rodando o fluxo completo no Chromium (`e2e/`, `ops/e2e-wsl.sh`) contra o backend real, saíram estes pontos. Os dois primeiros são positivos e definem como os testes (e o leitor de tela) enxergam a web.

**Acessibilidade e automação**

- **A árvore de `semantics` chega ao DOM.** O Compose cria, dentro de um shadow root aberto, elementos com `role` (`heading`, `button`, `textbox`, `list`), `aria-label` e texto. Os botões de ícone e os campos têm nome (`Mostrar senha`, `Buscar paciente`, `Fechar planilha`). O Playwright atravessa o shadow DOM: os testes usam `getByRole`/`getByText`, sem coordenadas.
- O canvas fica por cima dos elementos e **intercepta o clique**; campos de texto são `<div contenteditable>`; elementos fora da janela chegam com caixa 0×0. O `e2e/apoio.ts` contorna os três (clique por coordenadas, digitação por teclado, rolagem por roda do mouse).
- **Árvore desatualizada depois que uma folha ou diálogo fecha** (Novo paciente, Gerar convite, Ler termos de uso, Publicar). A tela volta ao normal, mas o DOM continua com o conteúdo da folha (ou fica vazio) e nada o atualiza: nem espera de 12 s, clique, Tab, Escape, rolagem, redimensionar a janela nem atalho Alt+N. Um leitor de tela continuaria lendo a folha fechada. **Entra na A6** (investigar a versão do Compose e a ordem de desmontagem do `ModalBottomSheet`/`AlertDialog`). No e2e, cada fase abre a página do zero.
- Navegador headless do Linux sem `locale` quebra o Compose (`Incorrect locale information provided`); o Playwright usa `pt-BR`.

**Bugs encontrados**

- **Aceite de convite pela web falhava sempre** (`400 FORMATO_INVALIDO`): o app mandava `"fuso":"SYSTEM"` (kotlinx-datetime no navegador). Corrigido em `idFusoDoAparelho()` (PR "Corrige o fuso enviado no aceite do convite pela web").
- **Recarregar a página desloga.** O `web.md` promete refresh silencioso, mas o `AmbienteWeb` nunca chama `ApiDieta.restaurarSessao()` (só o `AmbienteDieta` do app chama): depois do F5 não sai nenhum `POST /auth/refresh` e a tela volta para Entrar, mesmo com o cookie `refreshToken` (`HttpOnly`, `Secure`, `Strict`) guardado. Pendente.
- **Cadastro de nutri está bloqueado na web:** o `main.kt` passa `termosCadastro = null`, então a tela mostra "Os termos de uso estão indisponíveis" (esperado até o Guilherme publicar os termos, BE-36, mas vale ligar o app aos termos publicados em `/legal/terms-of-use`, que o cadastro do paciente já usa). O e2e cria a segunda nutri pela API.
- **"Gerar convite" falha com o link padrão do backend.** O backend gera `dietamais://convites/CODIGO` (padrão de `EMAIL_URL_CONVITE`), mas `PacientesOnline.convidar` só aceita `https` e responde `RespostaInvalida`, que a tela mostra como "Não foi possível concluir". Em produção o link deve ser `https://…/convite` (já recomendado em `configuracao-producao.md`); o e2e sobe o backend com `EMAIL_URL_CONVITE=https://localhost:8081/convite`. Decidir se o app deve aceitar também o esquema `dietamais`.
- Texto do consentimento de desenvolvimento aparece com acentos quebrados ("FictÃ­cio"): o `application-dev.properties` é lido como Latin-1. Só afeta o perfil `dev`.
- O plano Teste (5 pacientes) e a carga de exemplo (3 pacientes) deixam espaço para 2 cadastros por banco: o e2e sobe um banco novo a cada execução.

## E2E (A4)

`ops/e2e-wsl.sh` (veja `e2e/README.md`) sobe Postgres 18, backend e web de produção e roda `e2e/fluxo-completo.spec.ts`: nutri cadastra e convida, paciente aceita, nutri monta e publica a dieta, paciente vê o Hoje e marca Comi, e uma segunda nutri recebe `404` na paciente da primeira. Workflow manual/por PR em `.github/workflows/e2e-web.yml`.

## Medições da A5 (MOB-43, 29/10/2026)

Medido no Chromium 141 do Playwright, no WSL, com `E2E_MEDIR=1 ops/e2e-wsl.sh medicoes` (`e2e/medicoes.spec.ts`; resultado bruto em `build/medicoes-web.json`). O servidor de teste imita o Caddy da produção: gzip nível 5, ETag e `Cache-Control: no-cache`.

**Bundle do primeiro carregamento** (`wasmJsBrowserDistribution`, sem `composeResources`):

| Arquivo | Bruto | gzip -9 | gzip -5 (Caddy) | brotli |
| --- | --- | --- | --- | --- |
| `…wasm` do app (login, pacientes, dieta, avaliação, agenda, anamnese, Ktor, Koin, Compose) | 6,86 MB | 1,95 MB | 1,97 MB | 1,40 MB |
| `skiko.wasm` (renderização) | 8,64 MB | 3,20 MB | 3,22 MB | 2,50 MB |
| `dietamais.js` | 0,54 MB | 0,10 MB | 0,10 MB | 0,08 MB |
| **Total** | 16,0 MB | **5,5 MB** (5,25 MiB) | 5,5 MB | 4,2 MB |

O app passou de 5,95 MB (MOB-43, primeira entrega) para 6,86 MB brutos com as telas novas (+15%). Medido na rede, o primeiro carregamento transfere 5,9 MB (inclui fontes e `strings`). O Caddy já oferece `zstd` e `gzip`; **brotli economiza cerca de 24%** sobre o gzip, mas o `encode` do Caddy não faz brotli (só se vier pré-comprimido ou por módulo).

**Abertura até o formulário de Entrar** (celular 390×844, cache vazio e depois recarga na mesma sessão):

| Rede | Primeira vez | Segunda vez (revalida, 304) | Bytes na segunda vez |
| --- | --- | --- | --- |
| Sem limite (loopback) | 1,4 s | 0,7 s | 2,4 KB |
| 4G rápido (9 Mbps, 165 ms) | **7,1 s** | 2,1 s | 2,4 KB |
| 4G lento (1,6 Mbps, 150 ms) | **31,1 s** | 2,0 s | 2,4 KB |

Leitura: no 4G bom a primeira abertura leva uns 7 s; em 4G fraco passa de 30 s, quase tudo é download do `skiko.wasm` e do `.wasm` do app. Nas aberturas seguintes só há revalidação (o Caddy manda `no-cache` com ETag e os `.wasm` têm hash no nome, então dá para trocar para `Cache-Control: public, max-age=31536000, immutable` nos `.wasm` e ganhar mais uns 1,5 s sem nenhum byte). A nutri no desktop em rede fixa não sente; a paciente em 4G fraco sente na primeira vez, e para ela o app Android/iOS segue sendo o caminho principal (a web da paciente é conveniência).

**Autofill de senha: não funciona.** Na tela de Entrar não existe nenhum `<input>`, `<form>` nem `type="password"`: os campos são `<div role="textbox" contenteditable>` sem `autocomplete` nem `name` (e `aria-label` "E-mail" e "Senha"). O gerenciador de senhas do navegador não oferece salvar nem preencher. Opções para o ponto de decisão do Guilherme: (1) aceitar, já que o app é a forma principal para a paciente; (2) fazer a tela de Entrar em HTML puro no `index.html` (form com `autocomplete="username"`/`current-password`), que entrega a sessão ao Compose; (3) esperar suporte a autofill do Compose para web. Não implementei nenhuma: muda arquitetura de login.
