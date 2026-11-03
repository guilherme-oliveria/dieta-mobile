# MOB-08 — aceitar convite

**Objetivo:** entrada por código/link, consulta com nutri e consentimento real, login ou cadastro de paciente, aceite explícito e sessão segura.

**Fontes:** MOB-08 e RN-VIN-02..04/07, contrato BE-C1/BE-07 develop `2d9e231`, DTOs e 49 JSONs oficiais preservados; DES-01 sem artboards de cadastro paciente/consentimento, usar componentes existentes. Branch sai de develop mobile `924bda3`. Execução nativa autorizada, sem subagentes; revisão independente pelo QA.

**Arquitetura:** parser puro `dominio/convite` aceita código ou links estritos (esquema dietamais singular/plural; HTTPS apenas domínio configurado). Canal de entrada em memória recebe cold/warm links Android/SwiftUI. Configuração de associação Android/iOS é explícita; sem domínio oficial, código e esquema customizado funcionam, associação HTTPS fica pendente de domínio/assetlinks/AASA. Não publicar infra nem registrar domínio.

`ConvitesRepositorio` consulta pública sem Bearer e aceita com Bearer para conta existente ou cadastro público, mantendo chave idempotente. `ApiDieta.aceitarConvite` valida sessão PACIENTE/vínculo antes de persistir usando geração da sessão atual; resposta atrasada após logout/troca é rejeitada. Credenciais, código e consentimento ficam somente em memória, sem logs/cache público durável. UI nunca aceita automaticamente ao abrir link.

`ConviteViewModel` preserva convite durante login, exige leitura disponível e checkbox inicialmente falso, redefine aceite ao mudar código/versão; mapeia erros por código e conserva chave na retentativa sem edição. Conclusão fecha o fluxo e atualiza o shell com sessão PACIENTE real. Nova conta segue bloqueada até aprovação E publicação dos termos de uso 1.0.0. Consentimento clínico é distinto e vem da consulta BE-07, nunca de texto inventado.

**Atualização autorizada durante execução:** BE-36 foi aprovada/mergeada em develop `f3aef14`. Sincronizar documentos oficiais e preservar os 50 objetos JSON/manifesto. Integrar `GET /legal/terms-of-use` público na raiz da mesma origem, metadados/Markdown e campos opcionais nested `aceitouTermos`/`versaoTermos`. Liberar envio somente com PUBLICADO + ambos os flags/datas + cadastroHabilitado e aceite explícito separado. A fonte real continua RASCUNHO; fixtures publicadas são somente fictícias de teste. Cadastro da nutri/MOB-06 continua configuração bloqueada; esta integração pertence ao paciente convidado.

## Etapas

- [x] RED parser: código normalizado, caminhos singular/plural, HTTPS permitido/configuração ausente, host falso/userinfo/query/fragment/encoding/tamanho recusados.
- [x] RED API/repositório: consulta pública sem Bearer, aceite autenticado/nova conta, payload/chave oficiais, persistência no cofre, sessão divergente e resposta após logout recusadas.
- [x] Parser/repositório/API e DI mínimos; testes verdes.
- [x] RED metadados BE-36/rota pública sem Bearer/campos opcionais e independência dos dois aceites; somente fonte aprovada.
- [x] RED ViewModel: consentimento explícito, convite preservado no login, login não aceita sozinho, termos ausentes bloqueiam nova conta, expiração/invalidez/rate-limit, cancelamento/resposta antiga, retentativa idempotente. Repetição de link RED corrigida; consulta de termos pendente não bloqueia login existente.
- [x] UI de código/nutri/consentimento/login/cadastro e integração de sessão; recepção cold/warm e configuração App/Universal Links. Associação externa depende do domínio ainda indefinido.
- [x] UI três tamanhos/fonte 200%, 16 capturas/previews; local final 132 JVM/42 Android host sem falhas, build/migração/metadados verdes (1m16s/94 tarefas, `.gradle/mob08-final.log`).
- [ ] Revisão independente e CI real no PR develop; iOS/Swift e associação de domínio não executados no host Windows.

## Foco da revisão

- Conhecer o código/e-mail não autentica; conta existente usa Bearer e cadastro nulo.
- Link não pode enviar requisição/token a origem arbitrária nem fazer aceite automático.
- Termos de uso e consentimento clínico têm aceites e fontes distintos; rascunho nunca libera cadastro.
- Logout/cancelamento/troca de código invalidam operações em voo; senha não persiste fora do cofre/sessão.
- iOS/Android associação real exige arquivos de domínio e credenciais de assinatura, não confundir configuração compilável com verificação no dispositivo.
