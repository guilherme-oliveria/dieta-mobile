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

## Rodando

- Android: `./gradlew :androidApp:assembleDebug`, ou pelo Android Studio.
- Desktop: `./gradlew :desktopApp:run`. Com hot reload: `./gradlew :desktopApp:hotRun --auto`.
- iOS: abra a pasta `iosApp` no Xcode (precisa de um Mac).

## Testes

- Android: `./gradlew :shared:testAndroidHostTest`
- Desktop: `./gradlew :shared:jvmTest`
- iOS: `./gradlew :shared:iosSimulatorArm64Test` (no Mac)

O GitHub Actions roda os testes e o build de Android e desktop no Linux, e os testes de iOS num Mac.
