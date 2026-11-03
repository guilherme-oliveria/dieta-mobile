# Dieta+ — diagramas de arquitetura de software

> Documento comum aos 3 repositórios (`dieta-backend`, `dieta-mobile` e o `dieta-frontend`, arquivado), com o mesmo conteúdo nos repositórios ativos. Ao mudar, atualize as 3 cópias.
> Os diagramas são Mermaid e o GitHub os desenha direto no navegador. O texto de cada regra está em `visao-geral.md`, `integracao.md`, `backend.md`, `mobile.md` e `web.md`. Se algum diagrama divergir deles, o texto vale.

Níveis, no estilo C4:

1. [Contexto](#1-contexto): quem usa o Dieta+ e com quais sistemas externos ele conversa.
2. [Containers](#2-containers): as aplicações dos 3 repositórios e a infraestrutura.
3. [Componentes do backend](#3-componentes-do-backend): os módulos do monólito.
4. [Componentes do app](#4-componentes-do-app-kmp): as camadas do app KMP.
5. [Componentes do site](#5-componentes-do-site-angular): o site Angular (pós-MVP).
6. [Fluxos principais](#6-fluxos-principais): login, sync offline, publicar dieta, convite e pagamento.
7. [Implantação](#7-implantação): o que roda onde.

## 1. Contexto

```mermaid
flowchart LR
    nutri["Nutricionista<br/>(celular, tablet, desktop; web depois)"]
    paciente["Paciente<br/>(celular)"]
    admin["Admin da plataforma"]

    dieta(["Dieta+"])

    gateway["Gateway de pagamento<br/>(Asaas, suposição S3)"]
    fcm["Firebase Cloud Messaging<br/>(só push)"]
    smtp["Servidor de e-mail<br/>(SMTP)"]
    agenda["Google Agenda / Apple Calendário"]
    lojas["App Store / Google Play"]

    nutri -- "cadastra pacientes, avalia, monta e publica dieta" --> dieta
    paciente -- "vê a dieta, troca alimento, faz check-in, responde anamnese" --> dieta
    admin -- "planos, assinaturas, CRN" --> dieta
    dieta -- "checkout hospedado; recebe webhook" --> gateway
    dieta -- "push genérico" --> fcm
    dieta -- "confirmação, senha, lembretes" --> smtp
    agenda -- "assina o feed .ics por token" --> dieta
    lojas -. "distribui o app" .-> paciente
    lojas -. "distribui o app" .-> nutri
```

## 2. Containers

```mermaid
flowchart TB
    subgraph clientes["Clientes"]
        direction LR
        mob["<b>dieta-mobile</b><br/>Kotlin Multiplatform + Compose MP<br/>Android · iOS · Desktop<br/>offline-first"]
        web["<b>web (alvo wasmJs do dieta-mobile)</b><br/>Compose para web<br/>nutri e paciente, só online"]
    end

    sqlite[("SQLite local<br/>SQLDelight")]
    seguro[["Keystore / Keychain /<br/>arquivo cifrado"]]

    subgraph servidor["VPS (Docker Compose)"]
        direction TB
        caddy["Caddy<br/>HTTPS automático, proxy reverso"]
        api["<b>dieta-backend</b><br/>Java 25 + Spring Boot 4.1<br/>monólito modular, REST /api/v1"]
        pg[("Postgres 18<br/>Liquibase, multi-inquilino<br/>por nutricionista_id")]
        s3[("Storage S3<br/>MinIO / S3: PDFs e fotos")]
        nginx["nginx<br/>arquivos do site"]
    end

    fcm["FCM"]
    smtp["SMTP"]
    gw["Gateway de pagamento"]
    cal["Google / Apple Calendário"]

    mob -- "HTTPS JSON /api/v1<br/>JWT + refresh rotativo<br/>GET/POST /sync" --> caddy
    web -- "HTTPS JSON /api/v1<br/>JWT em memória + cookie HttpOnly" --> caddy
    caddy --> api
    caddy --> nginx
    mob --- sqlite
    mob --- seguro
    api -- "JPA / JDBC" --> pg
    api -- "URL pré-assinada 5 min" --> s3
    api -- "outbox → push" --> fcm
    fcm -- "push {tipo, sync, rota}" --> mob
    api -- "outbox → e-mail" --> smtp
    api -- "POST /assinatura/checkout" --> gw
    gw -- "POST /webhooks/pagamento" --> caddy
    cal -- "GET /cal/{token}.ics" --> caddy
```

Regras que o diagrama resume:

- O backend decide tudo: permissão, plano, limite e regra. App e site só mostram.
- O app lê só do SQLite local e sincroniza em segundo plano. O site não tem banco local e usa os endpoints REST com `If-Match`.
- O desktop não recebe FCM: ele sincroniza periodicamente.
- Nada de fila externa no MVP. Push e e-mail saem por uma tabela de pendências (outbox) lida por jobs `@Scheduled`.

## 3. Componentes do backend

Pacote raiz `com.br.dietamais`. Cada módulo só expõe o subpacote `api` aos outros, e o Spring Modulith (ou ArchUnit) verifica essa fronteira num teste.

```mermaid
flowchart TB
    subgraph entrada["Entrada HTTP"]
        direction LR
        rest["Controllers REST<br/>/api/v1/*"]
        syncc["sync<br/>GET/POST /sync"]
        wh["Webhook de pagamento"]
        ics["Feed /cal/{token}.ics"]
    end

    subgraph comum["comum"]
        direction LR
        seg["Segurança JWT<br/>UsuarioAtual"]
        guarda["GuardaAssinatura<br/>(RN-ASS-04)"]
        erros["ProblemDetail<br/>CodigoErro"]
        idem["Idempotência<br/>Trace id · log_acesso"]
    end

    subgraph negocio["Módulos de negócio"]
        direction LR
        contas["contas"]
        assin["assinaturas"]
        pac["pacientes"]
        alim["alimentos"]
        diet["dietas"]
        rot["rotina"]
        aval["avaliacoes"]
        quest["questionarios"]
        ag["agenda"]
    end

    subgraph suporte["Módulos de suporte"]
        direction LR
        notif["notificacoes<br/>outbox, FCM, SMTP, jobs"]
        rel["relatorios<br/>PDF, exportação LGPD"]
        adm["admin"]
    end

    rest --> seg --> guarda --> negocio
    syncc --> seg
    syncc -- "despacha mutações ao módulo dono;<br/>junta mudanças por seq" --> negocio
    wh --> assin
    ics --> ag
    rest --> erros
    rest --> idem

    pac -- "consulta limite" --> assin
    diet -- "lista do paciente" --> alim
    diet -- "vínculo / dono" --> pac
    rot -- "dieta em uso" --> diet
    aval -- "vínculo / dono" --> pac
    quest -- "vínculo / dono" --> pac
    ag -- "refeições da dieta" --> diet

    diet -. "evento DietaPublicada" .-> notif
    diet -. "evento DietaPublicada" .-> ag
    quest -. "evento QuestionarioRespondido" .-> notif
    assin -. "evento AssinaturaMudou" .-> notif
    rot -. "job: 3 dias sem check-in" .-> notif
    diet --> rel
    aval --> rel

    negocio --> pg[("Postgres 18")]
    suporte --> pg
    rel --> s3[("S3")]
```

Camadas dentro de cada módulo:

```mermaid
flowchart LR
    api["api/<br/>Controller, DTOs record,<br/>eventos públicos"] --> aplic["aplicacao/<br/>casos de uso transacionais"]
    aplic --> dom["dominio/<br/>entidades, enums,<br/>regras puras"]
    aplic --> infra["infra/<br/>repositories Spring Data,<br/>clientes externos, adaptador do sync"]
    infra --> dom
```

## 4. Componentes do app KMP

Pacote `org.br.dietamais` em `shared/src/commonMain/kotlin`. Os apps `androidApp`, `iosApp` e `desktopApp` só inicializam a plataforma.

```mermaid
flowchart TB
    subgraph entradas["Pontos de entrada"]
        direction LR
        andr["androidApp<br/>MainActivity, FCM, permissões"]
        ios["iosApp<br/>Swift + MainViewController, APNs via FCM"]
        desk["desktopApp<br/>janela, sem FCM"]
    end

    subgraph shared["shared (commonMain)"]
        direction TB
        appk["App.kt · app/<br/>DietaTheme, Koin, navegação,<br/>shell adaptativo (WindowSizeClass)"]
        ui["ui/<br/>auth · paciente/* · nutri/* ·<br/>notificacoes · comum · theme"]
        vm["ViewModels<br/>StateFlow&lt;Estado&gt;, eventos"]
        dom["dominio/<br/>modelos e regras puras:<br/>kcal, substituto, lista de compras, lembretes"]
        repo["data/repositorio<br/>junta local + remoto"]
        local["data/local<br/>SQLDelight (.sq / .sqm)"]
        remoto["data/remoto<br/>Ktor + kotlinx.serialization,<br/>DTOs, ProblemDetail"]
        sync["data/sync<br/>Sincronizador, FilaMutacoes, cursor"]
        plat["plataforma/ (expect)<br/>ArmazenamentoSeguro, NotificadorLocal,<br/>Conectividade, Compartilhador, AbridorLink"]
    end

    subgraph actuals["actual por plataforma"]
        direction LR
        aA["androidMain"]
        aI["iosMain"]
        aJ["jvmMain"]
    end

    andr & ios & desk --> appk --> ui --> vm --> dom
    vm --> repo
    repo -- "telas leem daqui (Flow)" --> local
    repo -- "ações online:<br/>login, convite, publicar, PDF" --> remoto
    repo -- "grava + enfileira mutação" --> sync
    sync --> local
    sync --> remoto
    vm --> plat
    plat --> actuals
    remoto -- "HTTPS /api/v1" --> api["dieta-backend"]
```

Layout adaptativo (detalhe em `docs/design/plataformas.md` no `dieta-mobile`):

```mermaid
flowchart LR
    w{"WindowSizeClass"} -- "compacta < 600 dp<br/>celular" --> c["Barra inferior<br/>uma coluna"]
    w -- "média 600–840 dp<br/>tablet em pé" --> m["Trilho lateral<br/>coluna larga"]
    w -- "expandida ≥ 840 dp<br/>tablet deitado, desktop" --> e["Trilho lateral<br/>lista-detalhe"]
```

## 5. Componentes do site Angular

Fase P1, depois do MVP. O site só funciona online e não tem regra própria.

```mermaid
flowchart TB
    rotas["app.routes.ts<br/>lazy por área"] --> layout["layout/<br/>shell, sidenav da nutri"]
    layout --> auth["auth/<br/>entrar, cadastro, convite"]
    layout --> nutri["nutri/<br/>painel, pacientes, paciente, avaliacao,<br/>dieta, alimentos, anamnese, agenda, assinatura"]
    layout --> pacw["paciente/<br/>(por último)"]
    auth --> nucleo
    nutri --> nucleo
    pacw --> nucleo
    nutri --> comp["compartilhado/<br/>componentes, pipes"]
    nucleo["nucleo/<br/>SessaoService (signals), guards por perfil,<br/>interceptors: token, erro, trace"] --> apig["api/<br/>cliente GERADO do OpenAPI"]
    apig -- "HTTPS /api/v1, If-Match" --> be["dieta-backend"]
```

## 6. Fluxos principais

### Login e renovação de token

```mermaid
sequenceDiagram
    autonumber
    participant C as App ou site
    participant B as dieta-backend
    participant DB as Postgres
    C->>B: POST /auth/login {email, senha, dispositivo}
    B->>DB: confere usuário (Argon2) e tentativas
    B-->>C: accessToken (JWT 15 min) + refreshToken (30 dias)
    Note over C: app: Keystore/Keychain · site: access em memória, refresh em cookie HttpOnly
    C->>B: GET /api/v1/... Authorization: Bearer
    B->>DB: confere vínculo, dono e assinatura
    B-->>C: 200
    C->>B: chamada com token vencido
    B-->>C: 401
    C->>B: POST /auth/refresh {refreshToken}
    B->>DB: rotaciona (reuso do antigo revoga a família)
    B-->>C: novo par de tokens
```

### Sincronização offline do app

```mermaid
sequenceDiagram
    autonumber
    participant T as Tela (Compose)
    participant L as SQLite local
    participant S as Sincronizador
    participant B as dieta-backend
    T->>L: grava a mudança (ex.: avaliação)
    T->>L: enfileira mutação {idMutacao, entidade, versaoBase}
    L-->>T: Flow atualiza a tela na hora
    Note over S: ao abrir, ao voltar do 2º plano,<br/>a cada 15 min, ao receber push "sync"
    S->>B: POST /sync {mutacoes[≤200]}
    B-->>S: APLICADA / CONFLITO (atual) / REJEITADA (codigo)
    alt CONFLITO_VERSAO
        S->>T: DialogoConflito: manter o meu ou usar o do servidor
    else REJEITADA
        S->>L: desfaz local e mostra a mensagem do codigo
    end
    S->>B: GET /sync?cursor=...
    B-->>S: mudancas por entidade + novo cursor (temMais)
    S->>L: aplica mudanças e excluídos
```

### Publicar dieta e avisar o paciente

```mermaid
sequenceDiagram
    autonumber
    actor N as Nutri (app)
    participant B as dieta-backend
    participant DB as Postgres
    participant J as Job de notificações
    participant F as FCM
    actor P as Paciente (app)
    N->>B: POST /dietas/{id}/publicar (online, Idempotency-Key)
    B->>B: GuardaAssinatura + dono + DIETA_VAZIA?
    B->>DB: RASCUNHO → PUBLICADA, anterior → SUBSTITUIDA,<br/>snapshot da lista de substituição
    B->>DB: evento DietaPublicada → notificacao_pendente (mesma transação)
    B-->>N: 200
    J->>DB: lê pendentes (a cada 30 s)
    J->>F: push {tipo: NOVA_DIETA, sync: true, rota}
    F-->>P: notificação
    P->>B: GET /sync
    B-->>P: nova dieta
    Note over P: reagenda lembretes locais.<br/>O feed .ics passa a mostrar os novos horários
```

### Convite e vínculo com consentimento

```mermaid
sequenceDiagram
    autonumber
    actor N as Nutri
    participant B as dieta-backend
    actor P as Paciente
    N->>B: POST /pacientes/{id}/convite
    B->>B: limite do plano (RN-ASS-03)
    alt limite atingido
        B-->>N: 422 LIMITE_PACIENTES_ATINGIDO
    else dentro do limite
        B-->>N: código de 8 caracteres + link (7 dias, uso único)
    end
    N-->>P: envia código/link (WhatsApp, e-mail)
    P->>B: GET /convites/{codigo}
    B-->>P: nome da nutri + termo de consentimento
    P->>B: POST /convites/{codigo}/aceitar (aceite LGPD)
    B-->>P: conta + vínculo + tokens
```

### Assinatura da nutri (fase 6)

```mermaid
sequenceDiagram
    autonumber
    actor N as Nutri
    participant B as dieta-backend
    participant G as Gateway (Asaas)
    N->>B: POST /assinatura/checkout
    B->>G: cria cobrança recorrente
    G-->>B: URL do checkout hospedado
    B-->>N: URL (abre no navegador externo, nunca dentro do app)
    N->>G: paga (PIX, cartão, boleto)
    G->>B: POST /webhooks/pagamento (assinado)
    B->>B: valida, ignora evento_id repetido
    B->>B: assinatura TESTE/EM_ATRASO → ATIVA
    Note over B: EM_ATRASO por mais de 7 dias → nutri só leitura.<br/>Paciente sempre vê a última dieta publicada
```

## 7. Implantação

```mermaid
flowchart TB
    subgraph disp["Aparelhos"]
        direction LR
        a["Android<br/>celular e tablet"]
        i["iPhone e iPad"]
        d["Desktop<br/>Windows, Mac, Linux"]
        nav["Navegador<br/>(web KMP)"]
    end

    subgraph vps["VPS · Docker Compose (compose.prod.yaml)"]
        direction TB
        caddy["Caddy :443<br/>TLS automático"]
        be["dieta-backend<br/>imagem Docker, Java 25"]
        fe["web KMP<br/>arquivos estáticos servidos pelo Caddy"]
        pg[("Postgres 18<br/>volume + backup diário criptografado")]
        minio[("MinIO ou S3 externo")]
    end

    subgraph ext["Serviços externos"]
        direction LR
        fcm["FCM"]
        mail["SMTP"]
        gw["Gateway"]
    end

    subgraph ci["GitHub"]
        direction LR
        r1["dieta-backend<br/>Actions: ./gradlew build"]
        r2["dieta-mobile<br/>Actions: Android + desktop no Linux,<br/>iOS no macOS"]
        r3["dieta-mobile (web)<br/>Actions: wasmJsBrowserDistribution"]
    end

    a & i & d -- "api.dietamais.com.br" --> caddy
    nav -- "dietamais.com.br" --> caddy
    caddy --> be
    caddy --> fe
    be --> pg
    be --> minio
    be --> fcm & mail & gw
    r1 -. "imagem" .-> be
    r3 -. "artefato da web" .-> fe
    r2 -. "AAB / IPA / instaladores" .-> lojas["Play Store, App Store,<br/>instaladores do desktop"]
    lojas -.-> a & i & d
```

Em dev, o `compose.yaml` do backend sobe Postgres 18, Mailpit (`localhost:8025`) e MinIO. O site em dev usa o `compose.yaml` do próprio repositório, e o emulador Android acessa a API em `http://10.0.2.2:8080`.
