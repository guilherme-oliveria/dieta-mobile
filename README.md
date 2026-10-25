# Dieta+ — app (Android, iOS e desktop)

App do Dieta+ em Kotlin Multiplatform com Compose Multiplatform: a mesma interface roda no Android, no iPhone e no desktop (Windows, Mac e Linux). Um único app atende a nutricionista e o paciente.

## Estrutura

| Pasta | O que tem |
| --- | --- |
| `shared/src/commonMain` | Código comum a todas as plataformas: telas, tema, regras e acesso à API |
| `shared/src/androidMain`, `iosMain`, `jvmMain` | O pouco que muda por plataforma |
| `androidApp` | Ponto de entrada do Android |
| `iosApp` | Projeto Xcode, ponto de entrada do iOS |
| `desktopApp` | Ponto de entrada do desktop |

Dentro de `commonMain/kotlin/org/br/dietamais`:

- `ui/theme`: cores e tema do app, tiradas do canvas de design (verde `#2E7D4F`, fundo `#F5F6F1`, laranja `#C2551F` só para alertas).
- `ui/<funcionalidade>`: as telas, uma pasta por funcionalidade (entram com o plano de ação).
- `data`: cliente da API e banco local para uso sem internet (entram com o plano de ação).
- `app`: módulos de injeção de dependências e, nas próximas tarefas, navegação e shell.
- `dominio`: modelos e regras puras, separados por área.
- `plataforma`: configuração e declarações `expect`, com implementações `actual` nos source sets de plataforma.

As pastas ainda sem código possuem `.gitkeep` para preservar a estrutura prevista em `docs/arquitetura/mobile.md`.

## Configuração da API

`Config` é imutável e registrado no módulo Koin iniciado pelo `App`. O padrão de desenvolvimento é `http://10.0.2.2:8080` no Android e `http://localhost:8080` no desktop e no simulador iOS. A URL representa a origem do servidor; o prefixo `/api/v1` será acrescentado pelo cliente de MOB-05.

Para outro ambiente ou um aparelho físico, o ponto de entrada passa a configuração explicitamente:

```kotlin
App(config = Config(URL_API = "https://api.exemplo.test"))
```

Cada flavor ou distribuição fornece sua URL pelo ponto de entrada da plataforma. Não coloque credenciais na URL. O módulo SQLDelight de MOB-04 é configurado pelos pontos de entrada Android/iOS/desktop e abre o banco quando solicitado; cliente HTTP e sessão autenticada entram nas próximas tarefas.

## Banco local

A base técnica contém sessão sem tokens, cursor de sync, fila de mutações e conflitos. `FilaMutacoes` confirma a escrita local e a pendência na mesma transação. Configuração, versionamento e testes estão em [banco-local.md](docs/arquitetura/banco-local.md).

## Rodando

- Android: `./gradlew :androidApp:assembleDebug`, ou pelo Android Studio.
- Desktop: `./gradlew :desktopApp:run`. Com hot reload: `./gradlew :desktopApp:hotRun --auto`.
- iOS: abra a pasta `iosApp` no Xcode (precisa de um Mac).

## Testes

- Android: `./gradlew :shared:testAndroidHostTest`
- Desktop: `./gradlew :shared:jvmTest`
- iOS: `./gradlew :shared:iosSimulatorArm64Test` (no Mac)

O GitHub Actions roda os testes e o build de Android e desktop no Linux, e os testes de iOS num Mac.

## Documentação e plano de ação

Arquitetura, regras de negócio, contrato entre as aplicações e o plano de ação ficam em [`docs/arquitetura/`](docs/arquitetura/). Agentes de código (Claude Code, Codex) leem o [`AGENTS.md`](AGENTS.md). O design das telas fica em [`docs/design/`](docs/design/).
