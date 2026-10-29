# Configuração de produção (app: Android, iOS e desktop)

Tudo que precisa ser definido para gerar os apps apontando para a API de produção. O guia da API está em `dieta-backend/docs/operacao/configuracao-producao.md`.

**Regra de ouro:** nenhum segredo vai para o Git (`google-services.json`, `GoogleService-Info.plist`, keystores e `local.properties` estão proibidos no repositório). Valores sensíveis entram por propriedades do Gradle, variáveis de ambiente do CI ou secrets do GitHub.

## 1. Resumo: o que cada plataforma precisa

| Item | Android | iOS | Desktop |
| --- | --- | --- | --- |
| URL da API | `-PurlApi=https://api.exemplo.com.br` | `API_HOST=api.exemplo.com.br` em `iosApp/Configuration/Config.xcconfig` | `-PurlApi=...` ao empacotar, ou `DIETA_URL_API` / `-Ddieta.url.api` em execução |
| Domínio dos links de convite | `-PconviteHost=convites.exemplo.com.br` | `CONVITE_HOST=...` no xcconfig | não usa |
| Push (Firebase) | `-PfcmAppId -PfcmApiKey -PfcmProjectId -PfcmSenderId` | pendente (MOB-27b) | não usa |
| Versão do app | `versionName` e `versionCode` em `androidApp/build.gradle.kts` | `MARKETING_VERSION` e `CURRENT_PROJECT_VERSION` no xcconfig | `versaoDistribuicao` em `desktopApp/build.gradle.kts` |
| Assinatura de release | keystore + secrets (pendente, MOB-35a) | Apple Team ID + certificados (pendente, MOB-35a) | sem assinatura (MSI/DMG/DEB) |

**Atenção:** se a URL da API não for informada, cada plataforma usa o padrão de **desenvolvimento** (`10.0.2.2:8080` no Android, `localhost:8080` no iOS e desktop). Um app de produção sem a URL não conecta à API real. Confira o `https://` antes de publicar.

## 2. URL da API

- A URL é a **origem** do servidor, sem `/api/v1` (o app acrescenta) e **sem credenciais**.
- Release exige **HTTPS**. `http` só é aceito para `localhost`, `10.0.2.2` e IPs de rede privada, e no Android só no build debug.
- Mesmo domínio do `DOMINIO` do backend (ex.: `https://api.dietamais.com.br`).

### Android

```
./gradlew :androidApp:assembleRelease -PurlApi=https://api.exemplo.com.br -PconviteHost=convites.exemplo.com.br
```

As propriedades podem vir de `-P` na linha de comando, de `~/.gradle/gradle.properties` ou, no CI, de variáveis `ORG_GRADLE_PROJECT_urlApi`, `ORG_GRADLE_PROJECT_conviteHost` etc. (secrets do GitHub).

### iOS

Em `iosApp/Configuration/Config.xcconfig`:

```
TEAM_ID=ABCDE12345            # Team ID da conta Apple Developer
API_HOST=api.exemplo.com.br   # só o host, sem https:// (o xcconfig trata // como comentário)
CONVITE_HOST=convites.exemplo.com.br
MARKETING_VERSION=1.0.0
CURRENT_PROJECT_VERSION=1     # número de build, sobe a cada envio à App Store
```

O app monta `https://` + `API_HOST`. Com `API_HOST` vazio usa `localhost` (só serve no simulador). O bundle id é `org.br.dietamais.dieta-mobile` + `TEAM_ID`.

### Desktop

- Instalador com a URL embutida: `./gradlew :desktopApp:packageMsi -PurlApi=https://api.exemplo.com.br` (também `packageDmg`, `packageDeb`). O valor vira a opção da JVM `-Ddieta.url.api=...` do aplicativo.
- Em execução, a variável de ambiente `DIETA_URL_API` tem prioridade sobre `-Ddieta.url.api`, que tem prioridade sobre `http://localhost:8080`. Em desenvolvimento: `DIETA_URL_API=http://localhost:8080 ./gradlew :desktopApp:run`.

### Desenvolvimento em aparelho físico ou WSL

Celular físico: `./gradlew :androidApp:installDebug -PurlApi=http://IP_DO_PC:8080` (mesmo Wi-Fi, firewall do PC liberando a 8080). Emulador: o padrão `10.0.2.2:8080` já aponta para o `localhost` do PC.

## 3. Versão do app e `APP_VERSAO_MINIMA`

O app envia a própria versão em `X-App-Versao`: `versionName` (Android), `MARKETING_VERSION` (iOS) e `versaoDistribuicao` (desktop). O backend compara com `APP_VERSAO_MINIMA`; abaixo dela responde `426` e o app mostra "atualize o app". Regras práticas:

1. Suba a versão **antes** de enviar o build às lojas.
2. Só aumente `APP_VERSAO_MINIMA` depois que a versão nova estiver disponível nas lojas, senão os usuários ficam bloqueados sem poder atualizar.
3. Use sempre o formato `1.4.0` (número.número.número).

## 4. Push no Android (Firebase)

1. Crie um projeto no Console Firebase e registre o app Android com o `applicationId` **`org.br.dietamais`**.
2. Do console tire os quatro valores e passe ao build (sem `google-services.json`):

```
-PfcmAppId=1:123456789:android:abc123 -PfcmApiKey=AIza... -PfcmProjectId=meu-projeto -PfcmSenderId=123456789
```

Sem eles o push fica desligado e o app funciona normalmente. No **backend**, use a conta de serviço **do mesmo projeto** em `FCM_CREDENCIAIS_JSON` e `PUSH_ENVIO_HABILITADO=true`.

iOS: o push usa o mesmo projeto Firebase mais a chave APNs; a integração nativa ainda está pendente (MOB-27b).

## 5. Links de convite (App Links / Universal Links)

Hoje o convite funciona por código e pelo esquema `dietamais://convites`. Para links `https` que abrem o app:

1. Escolha um domínio (ex.: `convites.exemplo.com.br`) e passe em `-PconviteHost` (Android) e `CONVITE_HOST` (iOS).
2. Publique no domínio:
   - Android: `https://HOST/.well-known/assetlinks.json` com o package `org.br.dietamais` e o **SHA-256 do certificado de assinatura** usado nas builds de release (e o da Play App Signing, se usar).
   - iOS: `https://HOST/.well-known/apple-app-site-association` com `TEAMID.bundleId` e os caminhos `/convite/*` e `/convites/*`.
3. No backend, aponte `EMAIL_URL_CONVITE` para o mesmo host e prefixo (ex.: `https://convites.exemplo.com.br/convites`).

Sem domínio configurado, o marcador `nao-configurado.invalid` mantém o recurso desligado sem prejudicar o resto.

## 6. Assinatura de release e lojas (pendente: MOB-35a)

Ainda **não está implementado no repositório**; é preciso:

- **Android:** criar um keystore de upload (`keytool -genkeypair -v -keystore dietamais-upload.jks -alias dietamais -keyalg RSA -keysize 2048 -validity 10000`), guardá-lo **fora do Git** com a senha, e passar ao CI como secrets (keystore em Base64, `KEYSTORE_SENHA`, `KEY_ALIAS`, `KEY_SENHA`). Conta Play Console (taxa única) e teste interno.
- **iOS:** conta Apple Developer (anual), certificado de distribuição, perfil de provisionamento com Associated Domains, e um Mac (o do seu amigo) para o build e o TestFlight.
- **Desktop:** MSI/DMG/DEB saem do Gradle sem assinatura. Para o Windows e o macOS não reclamarem, depois será preciso certificado de assinatura de código (e notarização no macOS).
- **CI:** o workflow (`.github/workflows/ci.yml`) hoje roda testes e builds de debug. Empacotamento de release e secrets precisam de edição em `.github`, que o agente não consegue escrever.

## 7. Checklist de release

1. API em produção respondendo `UP` em `https://DOMINIO/actuator/health`.
2. Versões atualizadas (seção 3) e `APP_VERSAO_MINIMA` coerente.
3. Gerar cada app com `-PurlApi` / `API_HOST` de produção.
4. Testar: cadastro de nutri, login, sync, aceite de convite, PDF, assinatura (sandbox do Asaas) e exclusão de conta.
5. Conferir que **nenhum** segredo ou arquivo de credencial entrou no commit (`git status`).
