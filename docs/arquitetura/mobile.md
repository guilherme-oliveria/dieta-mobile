# Dieta+ — arquitetura do app (KMP)

Um app Kotlin Multiplatform + Compose Multiplatform para **Android, iOS e Desktop** (Windows, Mac, Linux), com perfil nutri e perfil paciente.
Telas e componentes: `docs/design/` (PO de design). Contrato com o backend: `integracao.md`. Regras: `regras-de-negocio.md`.

## Quem usa o quê

| Plataforma | Perfil principal | Observação |
| --- | --- | --- |
| Android / iOS celular | Paciente (e nutri em movimento) | Layout compacto, bottom navigation |
| Android / iPad tablet | Nutri na consulta | Layout médio/expandido, navigation rail + lista-detalhe |
| Desktop | Nutri no consultório, inclusive **sem internet** `[SUPOSIÇÃO S7]` | Layout expandido; sem FCM, sync periódico |

## Camadas

```
ui (Compose)  ──▶  ViewModel (estado da tela)  ──▶  dominio (modelos, regras, casos de uso)  ──▶  data (repositórios)
                                                                                                 ├── local  (SQLDelight / SQLite)  ◀── as telas leem daqui
                                                                                                 ├── remoto (Ktor + kotlinx.serialization)
                                                                                                 └── sync   (Sincronizador + fila de mutações)
plataforma (expect/actual): armazenamento seguro, notificações locais, push, conectividade, compartilhar, abrir link
```

- **Offline-first:** a tela observa um `Flow` do SQLDelight. Ações gravam no banco local + enfileiram uma mutação; o `Sincronizador` envia e puxa em segundo plano (ver `integracao.md`, Sincronização). Exceções: ações que exigem online (login, convite, publicar dieta, enviar questionário, PDF, assinatura) chamam a API direto e mostram erro se não houver rede.
- **Fluxo unidirecional:** `ViewModel` expõe `StateFlow<Estado<Tela>>` (data class imutável) e recebe eventos por funções (`aoMarcarRefeicao(id)`). Efeitos únicos (navegar, snackbar) por `Channel`/`SharedFlow`.
- Regras do lado do cliente (prévia de kcal, quantidade do substituto, lista de compras, agendamento de lembretes) ficam em `dominio/` como funções puras com teste em `commonTest`. O backend continua sendo quem valida.

## Pacotes

Raiz `org.br.dietamais` em `shared/src/commonMain/kotlin/`:

```
org/br/dietamais/
├── App.kt                    # entrada comum: tema + DI + navegação
├── app/                      # navegação (rotas), shell adaptativo, módulos Koin
├── ui/
│   ├── theme/                # DietaTheme, cores, tipografia (já existe)
│   ├── comum/                # componentes reutilizáveis (ver docs/design/componentes.md)
│   ├── auth/                 # entrar, cadastro da nutri, esqueci a senha, aceitar convite
│   ├── notificacoes/         # central de avisos
│   ├── paciente/
│   │   ├── hoje/             # dieta do dia + check-in + água
│   │   ├── dieta/            # refeição detalhada
│   │   ├── substituicao/     # trocar alimento
│   │   ├── compras/          # lista de compras
│   │   ├── anamnese/         # responder questionário (wizard)
│   │   ├── evolucao/         # gráficos
│   │   └── perfil/           # lembretes, agenda .ics, privacidade, nutris
│   └── nutri/
│       ├── painel/           # tela inicial da nutri
│       ├── pacientes/        # lista + convite
│       ├── paciente/         # ficha do paciente (abas)
│       ├── avaliacao/
│       ├── dieta/            # montador
│       ├── alimentos/        # lista de alimentos da nutri
│       ├── anamnese/         # modelos e respostas
│       ├── agenda/           # consultas
│       └── assinatura/
├── dominio/<area>/           # modelos e regras: dieta, alimentos, rotina, avaliacao...
├── data/
│   ├── local/                # drivers e DAOs do SQLDelight
│   ├── remoto/               # ApiDieta (Ktor), dto/, tratamento de ProblemDetail
│   ├── sync/                 # Sincronizador, FilaMutacoes, cursores
│   └── repositorio/          # repositórios que juntam local + remoto
└── plataforma/               # expect: ArmazenamentoSeguro, NotificadorLocal, Conectividade, Compartilhador, AbridorLink
```

Schema do SQLDelight em `shared/src/commonMain/sqldelight/org/br/dietamais/data/local/*.sq`; migrações `*.sqm` numeradas (o banco local também é versionado; nunca altere uma migração já publicada nas lojas).

A fundação de MOB-04 (drivers, schema inicial, isolamento da fila e versionamento) está documentada em [banco-local.md](banco-local.md).

`androidMain`, `iosMain` e `jvmMain` só têm os `actual` de `plataforma/` e os drivers. Os apps (`androidApp`, `iosApp`, `desktopApp`) só inicializam: FCM, permissões, deep links, janela do desktop.

Observação: o pacote do app é `org.br.dietamais` e o do backend é `com.br.dietamais`. Está certo assim; não renomeie.

## Bibliotecas

Adicione pelo `gradle/libs.versions.toml`, sempre na versão estável mais recente compatível com o Kotlin 2.4 e o Compose Multiplatform 1.12 já configurados. Confira compatibilidade antes de subir versões.

| Necessidade | Biblioteca |
| --- | --- |
| HTTP | Ktor Client 3 (`core`, `content-negotiation`, `auth`, `logging`; engines OkHttp no Android/Desktop e Darwin no iOS) |
| JSON | kotlinx.serialization |
| Banco local | SQLDelight 2 (drivers android, native, sqlite JVM) + extensão de coroutines |
| Datas | kotlinx-datetime |
| DI | Koin (`koin-core`, `koin-compose`, `koin-compose-viewmodel`) |
| Navegação | Navigation Compose multiplataforma da JetBrains (`org.jetbrains.androidx.navigation`), rotas `@Serializable` |
| Layout adaptativo | `material3-adaptive` / `WindowSizeClass` multiplataforma |
| Preferências simples | multiplatform-settings |
| Log | Kermit |
| Gráficos | desenhar com `Canvas` do Compose (linha simples); só traga biblioteca se a tela pedir |
| Testes | kotlin-test, kotlinx-coroutines-test, Turbine, Ktor `MockEngine`, SQLDelight driver em memória |

Não use: Firebase além do Messaging, Realm, bibliotecas de UI só-Android em `commonMain`.

## Layout adaptativo

- `WindowSizeClass` decide o shell: **compacto** = bottom navigation (celular); **médio/expandido** = navigation rail + painel lista-detalhe (tablet, desktop).
- Uma tela não testa a plataforma, testa o tamanho. Nada de `if (iOS)` em tela; diferenças de plataforma vão para `plataforma/`.
- Desktop: janela mínima 1024×700, atalhos de teclado nas telas da nutri (salvar, nova refeição), menu da bandeja.

## Autenticação e sessão

- `ArmazenamentoSeguro` (expect/actual): Android Keystore + EncryptedSharedPreferences (ou DataStore cifrado), iOS Keychain, Desktop arquivo cifrado com chave no keystore do Java ou no keyring do sistema.
- Plugin `Auth` do Ktor com `bearer { loadTokens; refreshTokens }`: um refresh por vez, em 401; se o refresh falhar, limpa a sessão e volta para o login (dados locais do usuário são apagados no logout).
- Perfil vem de `GET /eu`; o shell mostra o grafo de navegação do perfil.

## Sincronização

Implementa exatamente o que está em `integracao.md`:

- Tabela local `mutacao_pendente` (id da mutação, entidade, operação, id, versão base, JSON, tentativas, status). Gravar no banco + enfileirar é uma transação só.
- `Sincronizador`: envia a fila em lotes de até 200, depois puxa `GET /sync?cursor=` até `temMais=false`. Gatilhos: abrir o app, voltar do segundo plano, conexão voltou, push `sync`, a cada 15 min em primeiro plano, e no desktop a cada 5 min.
- Android em segundo plano: WorkManager com restrição de rede. iOS: `BGAppRefreshTask` (melhor esforço). Desktop: coroutine enquanto o app estiver aberto.
- Conflito: guarda a versão do servidor em `conflito` e mostra a tela de resolução (MOB-30). Rejeitada: desfaz local e mostra a mensagem do `codigo`.
- Indicador global: "offline" e "N alterações pendentes" no topo do shell.

## Notificações

- **Locais (lembretes):** `NotificadorLocal` reagenda tudo sempre que a dieta em uso ou as preferências mudam (função pura `planejarLembretes(dieta, preferencias, hoje) -> List<Lembrete>` em `dominio`, com teste).
  - Android: `AlarmManager.setAndAllowWhileIdle` (sem alarme exato), reagendar em `BOOT_COMPLETED`; canal por tipo; ações "Comi/Troquei/Pulei" gravam check-in direto no banco local. Pedir `POST_NOTIFICATIONS` no Android 13+.
  - iOS: `UNUserNotificationCenter` com `UNCalendarNotificationTrigger` repetindo; **limite de 64 pendentes** (priorize refeições, depois água); categorias com ações.
  - Desktop: notificação do Compose Desktop (`TrayState.sendNotification`) enquanto o app está aberto ou na bandeja.
- **Push (FCM):** Android pelo `firebase-messaging` no `androidApp`; iOS pelo Firebase via Swift Package Manager no `iosApp` (APNs), repassando o token ao `shared`. Ao receber: dispara sync e navega pela `rota`. Desktop não tem push.
- `google-services.json` e `GoogleService-Info.plist` **não vão para o Git**; ficam em secrets do CI.

## Deep links

- Convite: `https://dietamais.com.br/convite/{codigo}` (App Links/Universal Links, domínio a definir) e `dietamais://convite/{codigo}` como alternativa. Desktop: campo para colar o código.
- Agenda: o app abre `webcal://…` ou o link do Google Agenda gerado a partir do token (`integracao.md`, Agenda).

## Testes

| Tipo | Onde | Roda com |
| --- | --- | --- |
| Regras puras (`dominio`) | `commonTest` | `./gradlew :shared:jvmTest` (rápido) |
| Repositórios e sync | `commonTest` com SQLDelight em memória + Ktor `MockEngine` e JSONs de `commonTest/resources/contrato/` | `jvmTest` |
| ViewModels | `commonTest` com `kotlinx-coroutines-test` + Turbine | `jvmTest` |
| Plataforma Android | `androidHostTest` | `./gradlew :shared:testAndroidHostTest` |
| iOS | `iosTest` | `./gradlew :shared:iosSimulatorArm64Test` (Mac / CI) |
| UI | testes de Compose (`runComposeUiTest`) para telas críticas: Hoje, montador de dieta, conflito | `jvmTest` |

## Build e distribuição

- Android: `./gradlew :androidApp:assembleDebug`; release assinado no CI com keystore em secret; Play Console (US$ 25 uma vez).
- iOS: Xcode no Mac (do amigo do Guilherme ou runner macOS do GitHub); Apple Developer (US$ 99/ano).
- Desktop: `compose.desktop.application.nativeDistributions` (MSI, DMG, DEB). Sem loja no MVP; download direto.
- Configuração por ambiente (`URL_API`) via BuildKonfig ou objeto `Config` por flavor; padrão local `http://10.0.2.2:8080` no emulador Android e `http://localhost:8080` no desktop/iOS simulador.
