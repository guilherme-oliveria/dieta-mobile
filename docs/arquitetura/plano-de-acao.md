# Dieta+ mobile — plano de ação

Tarefas em ordem de execução. Cada tarefa = **uma branch + um PR** (`feature/MOB-NN-descricao-curta`). Marque `[x]` aqui no mesmo PR.
Dependências do backend usam o prefixo `BE-` (`dieta-backend/docs/arquitetura/plano-de-acao.md`). Se o endpoint ainda não existe, desenvolva contra o Ktor `MockEngine` com o JSON de exemplo do contrato e deixe o PR dizendo isso.
A parte visual de cada tela (layout, componentes, estados) está em `docs/design/telas.md` e a ordem das telas em `docs/design/plano-de-acao-ui.md`. Este arquivo é a ordem técnica; cada tarefa de tela aqui corresponde às telas listadas lá.

Definição de pronto de toda tarefa:
- Regras do cliente em `dominio/` com teste em `commonTest`; ViewModel com teste.
- Tela funciona nos 3 tamanhos (compacto, médio, expandido) e confere com o canvas.
- Leitura vem do banco local; nada trava sem internet (ou o botão fica desabilitado se a ação exige online).
- `./gradlew :shared:jvmTest :shared:testAndroidHostTest` verde e CI verde (inclui iOS).

## Fase 1 — Fundação do app, contas e vínculo

- [ ] **MOB-01 Dependências e estrutura.** Ktor, kotlinx.serialization, coroutines, kotlinx-datetime, Koin, Navigation Compose, SQLDelight, multiplatform-settings, Kermit no `libs.versions.toml`; pacotes de `mobile.md` criados; Koin iniciado no `App.kt`; `Config` com `URL_API` por plataforma.
- [ ] **MOB-02 Tipografia e componentes base.** Plus Jakarta Sans em `composeResources`; tipografia no `DietaTheme`; primeiros componentes de `ui/comum` conforme `docs/design/componentes.md` (botões, campos, cartão de refeição, estado vazio, erro, carregando, banner offline). *Depende de:* MOB-01.
- [ ] **MOB-03 Navegação e shell adaptativo.** Rotas `@Serializable`; shell por `WindowSizeClass` (bottom nav no compacto, rail + lista-detalhe no médio/expandido); grafos separados para nutri e paciente; janela do desktop (tamanho mínimo, título). *Depende de:* MOB-02.
- [ ] **MOB-04 Banco local e fila.** SQLDelight com drivers Android/iOS/JVM; tabelas `sessao`, `cursor_sync`, `mutacao_pendente`, `conflito`; `FilaMutacoes` com teste (gravar + enfileirar na mesma transação). *Depende de:* MOB-01.
- [ ] **MOB-05 Cliente da API e sessão.** `ApiDieta` (Ktor) com headers `X-App-Versao`, `X-Plataforma`, `Idempotency-Key`; leitura de ProblemDetail → `ErroApi(codigo, regra, campos)`; plugin `Auth` com refresh; `ArmazenamentoSeguro` expect/actual nas 3 plataformas; logout limpa dados locais. *Depende de:* MOB-04, BE-03.
- [ ] **MOB-06 Entrar e cadastro da nutri.** Telas de `ui/auth`: entrar, cadastro da nutri (com CRN e termos), esqueci a senha; aviso de e-mail não confirmado; bloqueio de login com mensagem do `codigo`. *Depende de:* MOB-03, MOB-05, BE-02 a BE-04.
- [ ] **MOB-07 Pacientes da nutri.** `ui/nutri/pacientes` (lista, busca, filtro por status) e `ui/nutri/paciente` (ficha, abas vazias); criar paciente; gerar e compartilhar convite (link + código pelo share nativo); tratar `LIMITE_PACIENTES_ATINGIDO` com aviso de plano. *Depende de:* MOB-06, BE-06, BE-07.
- [ ] **MOB-08 Aceitar convite.** Deep link `convite/{codigo}` (Android App Links, iOS Universal Links, esquema `dietamais://`) e campo de código; tela com nutri + termo de consentimento; criar senha ou entrar; erros `CONVITE_EXPIRADO`/`CONVITE_INVALIDO`. *Depende de:* MOB-06, BE-07.

## Fase 2 — Alimentos e dieta

- [ ] **MOB-09 Sync de leitura.** `Sincronizador.puxar()` com cursor e `temMais`; aplica mudanças e exclusões no SQLite; `Conectividade` expect/actual; gatilhos (abrir, voltar do fundo, conexão voltou, 15 min); WorkManager no Android; indicador offline no shell. *Depende de:* MOB-04, MOB-05, BE-08.
- [ ] **MOB-10 Alimentos da nutri.** `ui/nutri/alimentos`: lista por grupo, busca, criar, editar, desativar (online nesta fase). *Depende de:* MOB-09, BE-10, BE-14.
- [ ] **MOB-11 Lista de substituição do paciente.** Aba na ficha: remover/adicionar alimentos para aquele paciente. *Depende de:* MOB-10.
- [ ] **MOB-12 Montador de dieta.** `ui/nutri/dieta`: refeições (nome, horário, reordenar), itens (buscar alimento, porções), totais em tempo real com `CalculadoraTotais` em `dominio` (mesma fórmula do backend, teste com a dieta de 2509 kcal); salvar rascunho. Layout de duas colunas no expandido. *Depende de:* MOB-10, BE-11.
- [ ] **MOB-13 Publicar e duplicar dieta.** Publicar (exige online, confirmação), duplicar, ver histórico de versões; tratar `DIETA_VAZIA`. *Depende de:* MOB-12, BE-12.
- [ ] **MOB-14 Hoje e Dieta (paciente).** `ui/paciente/hoje` (refeições do dia, próxima em destaque) e `ui/paciente/dieta` (refeição detalhada), lendo o snapshot local; seletor de dieta em uso quando houver mais de uma nutri. *Depende de:* MOB-09, BE-13.
- [ ] **MOB-15 Trocar alimento.** `ui/paciente/substituicao`: substitutos do mesmo grupo da lista congelada, com quantidade equivalente (`calcularSubstituto` em `dominio`, com teste); escolher grava check-in TROCADO (local, enviado na fase 3). *Depende de:* MOB-14.

## Fase 3 — Rotina do paciente

- [ ] **MOB-16 Envio da fila.** `Sincronizador.enviar()` com lotes, `idMutacao`, tratamento de APLICADA/REJEITADA/CONFLITO; reenvio com backoff. *Depende de:* MOB-09, BE-15.
- [ ] **MOB-17 Check-in e água.** Marcar refeição FEITO/TROCADO/PULADO (até 3 dias para trás), contador de água com meta 35 ml/kg; tudo offline via fila. *Depende de:* MOB-14, MOB-16.
- [ ] **MOB-18 Lembretes locais.** `planejarLembretes` em `dominio` (com teste: horários, silêncio, limite de 64 no iOS); `NotificadorLocal` expect/actual (AlarmManager + boot no Android, UNUserNotificationCenter no iOS, bandeja no desktop); ações "Comi/Troquei/Pulei"; pedido de permissão. *Depende de:* MOB-17.
- [ ] **MOB-19 Perfil e preferências.** `ui/paciente/perfil`: tipos de lembrete, horário de silêncio, nutris vinculadas, sair; sincroniza `preferencias-notificacao`. *Depende de:* MOB-18, BE-18.
- [ ] **MOB-20 Lista de compras.** `ui/paciente/compras`: `gerarListaCompras(dieta, dias)` em `dominio` (com teste), agrupada por grupo, marcar itens (local), compartilhar como texto. *Depende de:* MOB-14.
- [ ] **MOB-21 Agenda do celular.** No perfil: gerar link .ics, botões Google Agenda e Calendário da Apple, revogar link. *Depende de:* MOB-19, BE-17.

## Fase 4 — Acompanhamento

- [ ] **MOB-22 Avaliação.** `ui/nutri/avaliacao`: formulário em seções (medidas básicas, perímetros, dobras, composição, parecer), validação de intervalos (RN-AVA-03), IMC mostrado ao salvar; paciente vê avaliações compartilhadas. *Depende de:* BE-19.
- [ ] **MOB-23 Evolução.** `ui/paciente/evolucao` e aba na ficha da nutri: gráficos de linha (peso, % gordura, perímetros, peso em casa) com `Canvas`. *Depende de:* MOB-22, BE-20.
- [ ] **MOB-24 Anamnese da nutri.** `ui/nutri/anamnese`: editar modelo (blocos, perguntas, tipos, condição), enviar ao paciente, ler respostas na ficha. *Depende de:* BE-21, BE-22.
- [ ] **MOB-25 Anamnese do paciente.** `ui/paciente/anamnese`: wizard por bloco, salvar parcial (offline), enviar (online); condição de exibição. *Depende de:* MOB-24, BE-22.
- [ ] **MOB-26 Agenda de consultas.** `ui/nutri/agenda` (dia/semana, criar, remarcar, cancelar) e próxima consulta no Hoje do paciente. *Depende de:* BE-23.
- [ ] **MOB-27 Push e central de avisos.** FCM no `androidApp` e no `iosApp` (token → `POST /eu/dispositivos`); ao receber, sync + navegação pela `rota`; `ui/notificacoes` com histórico. Arquivos do Firebase fora do Git (secrets do CI). *Depende de:* BE-24.
- [ ] **MOB-28 Painel da nutri.** `ui/nutri/painel`: consultas do dia, pacientes sem check-in há 3 dias, questionários respondidos, uso do plano. *Depende de:* MOB-26, BE-16.

## Fase 5 — Offline da nutri

- [ ] **MOB-29 Escrita offline da nutri.** Ficha, alimentos, listas, rascunho de dieta, avaliação, modelos e consultas passam a gravar local + fila (ATUALIZAR/EXCLUIR com `versaoBase`). Ações que exigem online continuam desabilitadas sem rede. *Depende de:* MOB-16, BE-25.
- [ ] **MOB-30 Conflitos e pendências.** Tela de conflito (lado a lado: minha versão × servidor; "manter a minha" / "usar a do servidor"); contador de pendências e lista do que falta enviar; teste de ponta a ponta com `MockEngine` devolvendo CONFLITO. *Depende de:* MOB-29.
- [ ] **MOB-31 Desktop pronto para consultório.** Sync a cada 5 min, menu e notificações na bandeja, atalhos de teclado, `nativeDistributions` (MSI, DMG, DEB) gerados no CI como artefato. *Depende de:* MOB-29.

## Fase 6 — Assinatura, PDF, privacidade e lojas

- [ ] **MOB-32 Assinatura.** `ui/nutri/assinatura`: plano, status, uso x limite, aviso de atraso/carência; "Assinar" abre o checkout no navegador externo (nunca compra dentro do app); modo só leitura quando `ASSINATURA_INATIVA`. *Depende de:* BE-26, BE-27.
- [ ] **MOB-33 PDF.** Gerar e compartilhar PDF da dieta e da avaliação (download da URL pré-assinada + share nativo). *Depende de:* BE-28.
- [ ] **MOB-34 Privacidade.** Termos e política, exportar meus dados, excluir conta (com confirmação). *Depende de:* BE-29.
- [ ] **MOB-35 Publicação.** Ícones e splash do design, nome "Dieta+", assinatura de release no CI (secrets), Play Console (teste interno), App Store Connect/TestFlight pelo Mac, `426 VERSAO_APP_OBSOLETA` → tela "atualize o app". *Depende de:* todas as anteriores.

## Pós-MVP (P2)

Troca de nutri com cópia do histórico, vitrine de nutris, consultoria online paga, foto do prato, chat, integração Google Fit/Apple Saúde, anúncios.
