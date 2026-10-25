# Cliente da API e sessão — MOB-05

`ApiDieta` usa Ktor, os motores OkHttp (Android/JVM) e Darwin (iOS), e o contrato oficial da fase 1. `Config.URL_API` aceita a origem do servidor; o cliente acrescenta `/api/v1` uma única vez. HTTPS é obrigatório fora dos endereços locais de desenvolvimento. Caminhos de operação são relativos, sem URL externa, travessia de diretórios ou redirecionamento automático.

`obter<T>` e `enviar<T>` desserializam DTOs com `JsonContrato`. O cliente envia `X-App-Versao` e `X-Plataforma`; POST recebe uma `Idempotency-Key` UUIDv7, conservada na retentativa após refresh. Reenvios de uma operação devem fornecer a mesma chave: a fila futura usará o identificador durável da mutação. Não há repetição automática de POST após erro de rede/5xx. Operações sem corpo não recebem JSON artificial. Parâmetros de consulta são codificados pelo Ktor.

Rotas `auth/**` omitem Bearer, inclusive logout, conforme BE-03: senha/refresh no JSON autentica essas operações; um Bearer expirado seria rejeitado antes delas. Outras rotas públicas podem usar `publica = true`. Dados clínicos continuam sendo lidos do banco local; esse cliente não cria telas nem faz sync.

## Contrato, erros e evolução

As cópias `integracao.md`, `contrato-fase-1.md` e os 49 JSONs mais manifesto vieram da `develop` oficial aprovada do backend, commit `eb7925314aada2fda1bd745cf3bd5d21b99f3633` (BE-03). Os arquivos foram conferidos contra os objetos Git; os dois exemplos de MOB-04 foram preservados. Cadastro, login, refresh e logout já existem no backend; as outras 13 operações continuam stubs 501. Os testes usam `MockEngine` com esses exemplos, sem servidor ou credenciais reais.

DTOs em `data/remoto/dto` espelham os records oficiais, sem regras de negócio próprias. `ignoreUnknownKeys` permite novos campos; enums desconhecidos viram `OUTRO`. Perfil desconhecido não ativa sessão nem recebe permissões. IDs e datas usam os tipos Kotlin correspondentes; cursor permanece string opaca. Campos opcionais são anuláveis; não existe fallback que transforme uma resposta incompleta em uma sessão válida.

ProblemDetail é convertido em `ErroApi(codigo, regra, campos, status, traceId, atual)`. Código é string para preservar entradas futuras; nenhuma decisão usa `title`, `detail` ou mensagem de campo. `RespostaInvalida`, `FalhaComunicacao`, `SessaoAlterada` e `ArmazenamentoSeguroIndisponivel` são falhas locais, não novos códigos de backend. Exceções não carregam corpo, tokens, credenciais ou causas de biblioteca que possam expor esses dados. O cofre iOS conserva somente o OSStatus numérico para diagnóstico, sem a mensagem original da biblioteca. Não se instala logging de HTTP. DTOs com senhas/tokens ocultam conteúdo na representação textual.

## Ciclo de sessão

O plugin `Auth` usa `bearer { loadTokens; refreshTokens }`, sem cache independente do cofre/controlador. `SessaoLocal` serializa restauração, login, renovação e logout com um mutex. Um 401 tenta renovar uma vez; requisições concorrentes reutilizam o resultado da renovação. O novo par rotativo é persistido antes de liberar outras requisições. A renovação não é cancelada junto da requisição original, para não perder o sucessor já emitido pelo servidor.

Cada requisição pertence a uma geração da sessão. Um 401 antigo não renova nem apaga login novo; uma resposta bem-sucedida antiga não entrega dados após logout/troca de conta; login em voo não reativa uma sessão encerrada. Uma sessão escolhida exige tokens completos, expiração e perfil suportado entre os perfis disponíveis. A resposta de escolha de perfil, sem tokens, não persiste credenciais. Senha não é guardada.

`restaurarSessao()` recupera o cofre e os metadados locais. `sessao: StateFlow<ContextoSessao?>` expõe somente usuário/perfil para o consumidor de navegação; `null` indica desconexão. MOB-06 conectará esse fluxo às telas de autenticação. Nesta tarefa a UI existente permanece igual, sem login fictício ou telas antecipadas. `moduloApi` registra o cliente/cofre nos três entrypoints e fecha cliente e motor no encerramento do módulo. A criação continua preguiçosa.

`sair()` tenta revogar o refresh no servidor e, mesmo offline ou sob cancelamento, apaga credenciais e limpa `conflito`, `mutacao_pendente`, `cursor_sync` e `sessao` numa transação local. Falha na revogação é devolvida ao chamador, embora a sessão local já esteja encerrada; sem rede não se promete revogação remota. Rejeição definitiva do refresh (4xx, exceto 429), resposta inválida/inconsistente ou segundo 401 após renovação bem-sucedida também encerram a sessão. Rede/timeout, 5xx e 429 durante refresh propagam `FalhaComunicacao` sem processar o 401 original nem alterar cofre, tabelas locais ou StateFlow; a fila offline permanece disponível para tentar depois. Cancelamento não é convertido em rejeição. Novas tabelas clínicas deverão ser acrescentadas à limpeza nas tarefas que as criarem. Falhas do armazenamento seguro são explícitas, sem fallback; o estado em memória deixa de disponibilizar credenciais ao encerrar.

## Armazenamento seguro

| Plataforma | Mecanismo e requisitos |
| --- | --- |
| Android | AES-256-GCM com chave não exportável no Android Keystore (API 24+ do projeto); IV aleatório por escrita; arquivo privado em `noBackupFilesDir`, escrito/lido por `AtomicFile`, incluindo recuperação do `.bak`. Corrupção, chave ausente ou serviço indisponível causam falha explícita. Backup do app está desabilitado. |
| iOS | `KeychainSettings` da biblioteca já prevista, sobre Keychain, com `AfterFirstUnlockThisDeviceOnly`: disponível depois do primeiro desbloqueio, sem migração para outro dispositivo. Não usa `NSUserDefaults`. |
| JVM Windows | Arquivo cifrado DPAPI no escopo `CurrentUser`, em `~/.dietamais/seguro/sessao.dpapi`, com gravação atômica. Usa o Windows PowerShell/.NET do sistema; nenhum segredo fica em argumentos ou ambiente. |
| JVM macOS | Keychain do usuário via `/usr/bin/security`. Escrita pelo modo interativo em stdin, sem senha em argv. Requer Keychain disponível/desbloqueado e autorização do usuário quando solicitada pelo sistema. |
| JVM Linux | Secret Service via `/usr/bin/secret-tool` (pacote `libsecret-tools`), com sessão de keyring disponível/desbloqueada. Segredo passa por stdin. Ausência do utilitário/serviço ou keyring bloqueado gera falha explícita. |

No desktop, stdout privado é consumido em memória; stderr nunca é repassado a logs/exceções. Processos têm timeout e saída limitada; não há shell genérico com dados interpolados. No Windows somente ciphertext é escrito em arquivo; no macOS/Linux o próprio keyring guarda o conteúdo. Nenhum token entra em SQLite, settings simples ou arquivo de texto. A sessão completa no cofre evita separar o par rotativo em gravações independentes.

Android permite HTTP apenas no recurso de debug, para `10.0.2.2`, `localhost` e `127.0.0.1`; a configuração principal/release recusa cleartext. Não há exceção para domínio remoto ou validação TLS desativada. `INTERNET` está no manifesto.

## Verificação e limites

Testes JVM usam SQLite real, `MockEngine` e todos os exemplos do manifesto: contrato, campos/enums novos, código/regra/campos, headers, idempotência, refresh concorrente e cancelado, segundo 401, isolamento entre gerações e limpeza no logout offline. Windows executa DPAPI real com dados fictícios em diretório temporário; testes de comandos macOS/Linux verificam stdin e falha explícita sem executar keyrings reais.

Há testes instrumentados Android de Keystore/AES-GCM, reabertura, recuperação de `.bak` sem arquivo base e adulteração; são compilados localmente e exigem dispositivo/emulador para executar. O Windows não executa iOS/macOS/Secret Service; não se presume cobertura nativa desses mecanismos a partir dos testes JVM. Não há regras de domínio/ViewModels/telas novas nesta tarefa.

O executável de testes Kotlin iOS sem app host verifica explicitamente o OSStatus de falta de entitlement (-34018); qualquer outro status falha com o número real. A persistência nativa permanece no target XCTest `TestesCofre` do `iosApp`, com host e entitlement exclusivo de simulador. `:shared:iosSimulatorArm64Test` finaliza executando `:shared:testarCofreIosComHost`, que constrói o framework e chama `iosApp/testar-cofre.sh`; o comando já usado no CI cobre as duas suítes. O script cria um simulador próprio, executa `xcodebuild test` com assinatura ad hoc de simulador (sem conta/certificado de produção), guarda `.xcresult` em `shared/build/` e remove somente esse simulador ao terminar. Nenhum teste é ignorado; falha em qualquer suíte falha o comando. O problema do executável sem host é descrito em [KT-61470](https://youtrack.jetbrains.com/issue/KT-61470); o resultado real do CI confirma ou refuta esse diagnóstico.

A versão enviada no desktop vem de recurso gerado no build a partir da mesma `versaoDistribuicao` usada por `packageVersion` do Compose. Não há valor padrão de versão no entrypoint que esconda metadados ausentes.

Verificação: `:shared:jvmTest :shared:testAndroidHostTest :shared:verifySqlDelightMigration :shared:compileCommonMainKotlinMetadata :shared:compileAndroidDeviceTest :desktopApp:jar :androidApp:assembleDebug :androidApp:assembleRelease`. No Windows, mantenha as duas propriedades temporárias do processo documentadas em `banco-local.md`; não altere configurações globais.

Referências dos mecanismos: [Auth/Bearer do Ktor](https://ktor.io/docs/client-bearer-auth.html), [AES-GCM no Android Keystore](https://developer.android.com/reference/android/security/keystore/KeyGenParameterSpec), [acessibilidade do Keychain](https://developer.apple.com/documentation/security/ksecattraccessibleafterfirstunlockthisdeviceonly), [DPAPI CurrentUser](https://learn.microsoft.com/en-us/dotnet/api/system.security.cryptography.dataprotectionscope), [modo interativo do security](https://github.com/apple-oss-distributions/SecurityTool/blob/main/security.c) e [secret-tool](https://github.com/GNOME/libsecret/blob/main/tool/secret-tool.c).
