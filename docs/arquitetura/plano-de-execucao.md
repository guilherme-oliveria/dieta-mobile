# Dieta+ — plano de execução (backend + mobile)

> Documento comum ao `dieta-backend` e ao `dieta-mobile`. Cópia oficial em `dieta-backend/docs/arquitetura/`.
> Criado em 24/10/2026 a partir de `visao-geral`, `plano-de-acao` (BE e MOB), `docs/design/` (mobile), `diagramas.md` e do canvas https://claude.ai/artifact/6thej5zGsTo81ZosiigGxC.
> **Status: execução contínua autorizada pelo Guilherme.** As tarefas avançam por dependências e ondas, sem nova confirmação entre elas; QA aprovado e merge na develop continuam obrigatórios. O progresso por tarefa consta no plano de ação e na coordenação do projeto.

Os `plano-de-acao.md` de cada repositório continuam sendo a lista de tarefas (o que fazer e o critério de pronto). Este arquivo diz **em que ordem, em paralelo com o quê e por qual branch** cada tarefa entra, e completa o que faltava nos planos.

## 1. Como o trabalho anda

### Gitflow

| Branch | Para quê | Sai de | Volta para |
| --- | --- | --- | --- |
| `main` | Só versões publicadas, com tag `vX.Y.Z` | — | — |
| `develop` | Integração de tudo que está pronto | `main` (uma vez) | `main` via `release/` |
| `feature/BE-NN-…`, `feature/MOB-NN-…`, `feature/DES-NN-…` | Uma tarefa | `develop` | `develop` (PR) |
| `release/X.Y.Z` | Fechar um marco (versão, changelog, testes, correções) | `develop` | `main` (PR aprovado pelo Guilherme) + back-merge em `develop` |
| `hotfix/X.Y.Z` | Correção urgente em produção | `main` | `main` + `develop` |

- Sessões na nuvem que já recebem uma branch (`claude/…`, `codex/…`) usam a dela, com PR para `develop`.
- Uma tarefa = uma branch = um PR, como já dizem os `AGENTS.md`. PR de tarefa sempre para `develop`, nunca para `main`.
- Todo PR de tarefa passa pelo **agente QA** do repositório antes do merge (seção "Revisão por QA" do `AGENTS.md`): o agente de código abre e corrige, o QA aprova e faz o merge na `develop`, e a próxima tarefa só começa depois do veredito.
- Cada **marco** (seção 3) vira uma `release/`: `0.1.0` no M1, `0.2.0` no M2 e assim por diante; `1.0.0` é a publicação nas lojas.
- Os workflows e as regras de proteção do Gitflow (pasta `gitflow/` dos arquivos do projeto) entram pela tarefa **G-01**, feita pelo Guilherme, porque `.github/` e as configurações do GitHub não podem ser alteradas pela sessão na nuvem.

### Contrato primeiro, backend um passo à frente

1. No começo de cada fase, o backend abre **BE-Cn (contrato da fase n)**: records de requisição/resposta, controllers anotados no OpenAPI devolvendo `501`, os `CodigoErro` novos e um JSON de exemplo por endpoint em `src/test/resources/contrato/`. É um PR pequeno, revisado olhando só o `/v3/api-docs`.
2. Assim que o BE-Cn está em `develop`, o mobile copia os JSONs para `shared/src/commonTest/resources/contrato/` e começa as telas da fase contra o Ktor `MockEngine`. O mobile não espera o endpoint real.
3. O backend implementa as tarefas da fase **uma onda à frente** do mobile. Quando o endpoint real chega em `develop`, a tarefa MOB correspondente troca o mock pela API (ou a tarefa seguinte faz isso, dito no PR).
4. Mudança de contrato durante a fase: primeiro o backend atualiza o JSON de exemplo e o OpenAPI, depois o mobile acompanha. O teste de contrato dos dois lados quebra se divergirem.

### Design um passo à frente do mobile

O canvas tem 13 artboards. Faltam cerca de 30 telas ou estados (lista em `dieta-mobile/docs/design/telas.md`, status "faltando"). Para nenhuma tela ser feita sem desenho, as tarefas **DES-nn** (seção 4) desenham no canvas o que a próxima onda do mobile vai precisar. Se o desenho não ficar pronto a tempo, a tarefa MOB segue a descrição de `telas.md` e os componentes de `componentes.md`, como os docs de design já permitem, e o PR diz isso.

## 2. Ondas

Cada onda tem o que o backend, o mobile e o design fazem ao mesmo tempo. Uma onda começa quando as tarefas de backend da onda anterior estão em `develop` (o mobile pode atrasar meia onda sem bloquear ninguém). Tarefas novas criadas por este plano estão marcadas com **(novo)** e descritas na seção 5.

| Onda | Backend | Mobile | Design | Integração ao fim da onda |
| --- | --- | --- | --- | --- |
| 0. Preparação | G-01 develop e regras · **BE-C1** contrato da fase 1 | MOB-01 dependências · MOB-02 tema e componentes (inclui correção do `Color.kt`) | **DES-01** entrada e conta | Mobile compila nas 3 plataformas com o tema certo; contrato da fase 1 publicado |
| 1 | BE-01 base transversal · BE-02 cadastro da nutri | MOB-03 navegação e shell · MOB-04 banco local e fila | **DES-02** núcleo nutri (convidar, limite, painel celular, ficha lista-detalhe) | ProblemDetail real; shell com abas vazias |
| 2 | BE-03 login e tokens · BE-04 e-mail | MOB-05 cliente da API (mock) · MOB-06 entrar e cadastro (mock) | — | Mobile entra e cadastra contra a API real em dev |
| 3 | BE-05 UsuarioAtual e `/eu` (com perfis múltiplos) · BE-06 pacientes | MOB-06 integra · MOB-07 pacientes (mock) | **DES-03** núcleo paciente (perfil, sem dieta, orientações, água, peso) | — |
| 4 | BE-07 convite · BE-08 sync leitura · **BE-C2** contrato da fase 2 | MOB-07 integra · MOB-08 aceitar convite · MOB-09 sync de leitura (mock) | — | **Marco M1** |
| 5 | BE-09 catálogo · BE-10 listas · **BE-33** perfil da nutri | MOB-09 integra · MOB-10 alimentos | **DES-04** dieta e alimentos (alimentos, montar dieta celular, publicar) | — |
| 6 | BE-11 rascunho de dieta · BE-12 publicar e duplicar | MOB-11 lista do paciente · MOB-12 montador · **MOB-36** perfil da nutri | — | Totais de 2509 kcal batem nos dois lados |
| 7 | BE-13 dieta do paciente · BE-14 fontes de sync · **BE-35** dados de exemplo · **BE-C3** | MOB-13 publicar · MOB-14 Hoje e Dieta · MOB-15 trocar alimento | — | **Marco M2** |
| 8 | BE-15 rotina e sync de escrita · BE-17 .ics · BE-18 dispositivos | MOB-16 envio da fila · MOB-17 check-in e água · MOB-20 compras | **DES-05** acompanhamento (agenda, histórico de avaliações, anamnese da nutri, avisos, avaliação no celular) | — |
| 9 | BE-16 adesão · BE-32a deploy beta (novo) | MOB-18 lembretes · MOB-19 perfil e preferências · MOB-21 agenda .ics · **MOB-35a** beta interno | — | **Marco M3 (beta com a nutri real)** |
| 10 | **BE-C4** · BE-19 avaliações · BE-20 evolução | MOB-22 avaliação (+ histórico) · MOB-23 evolução (+ registrar peso) | **DES-06** conta, assinatura, privacidade, minhas nutris, sync e conflito no desktop | — |
| 11 | BE-21 modelos · BE-22 envio e resposta | MOB-24 anamnese da nutri · MOB-25 anamnese do paciente | — | — |
| 12 | BE-23 consultas · BE-24 push · **BE-34** central de avisos | MOB-26 agenda · MOB-27 push e avisos · MOB-28 painel | — | **Marco M4** |
| 13 | **BE-C5** · BE-25 sync com conflito | MOB-29 escrita offline · MOB-30 conflitos · MOB-31 desktop | — | **Marco M5** |
| 14 | **BE-C6** · BE-26 gateway · BE-27 ciclo da assinatura · BE-28 PDFs | MOB-32 assinatura · MOB-33 PDF | — | — |
| 15 | BE-29 LGPD · BE-30 admin · BE-31 endurecimento · BE-32 produção | MOB-34 privacidade · MOB-35 lojas | — | **Marco M6 = 1.0.0** |
| 16. Web (P1) | **BE-37** modo navegador | **MOB-40** alvo web · **MOB-41** separar offline → MOB-42 sessão no navegador → MOB-43 telas da nutri (ponto de decisão) → MOB-44 paciente · MOB-45 navegador e acessibilidade → MOB-46 publicação | **DES-07** web | **Marco M7 = 1.1.0** (web da nutri e do paciente) |

Tarefas sem dependência entre si dentro da mesma coluna e onda podem ir em PRs paralelos (por exemplo BE-17 e BE-18, ou MOB-17 e MOB-20).

A onda 16 não espera o M6: MOB-40 e MOB-41 não mudam comportamento do app e podem entrar já; BE-37 também. Só MOB-46 depende da VPS (BE-32b). MOB-41 mexe em muitos arquivos do `shared`: faça num PR só, sem outra tarefa do mobile aberta ao mesmo tempo, para evitar conflito.

## 3. Marcos (cada um vira uma `release/`)

| Marco | Versão | O que a nutri real consegue fazer | Como conferir |
| --- | --- | --- | --- |
| M1 | 0.1.0 | Criar conta (teste de 5 pacientes), cadastrar paciente, convidar; o paciente aceita com consentimento | No Android e no desktop contra o backend em dev; teste de isolamento verde |
| M2 | 0.2.0 | Montar a dieta do PDF (2509 kcal), publicar; o paciente vê o dia, a refeição e troca alimentos sem internet | Modo avião no celular do paciente depois da primeira sync |
| M3 | 0.3.0 | Paciente recebe lembretes, marca Comi/Troquei/Pulei, água, lista de compras, agenda .ics; nutri vê adesão | **Beta**: backend na VPS, Android teste interno, TestFlight, desktop por artefato do CI |
| M4 | 0.4.0 | Avaliação com IMC, evolução, anamnese, agenda de consultas, push e central de avisos | Push chega no Android e no iPhone sem dado clínico no texto |
| M5 | 0.5.0 | Nutri edita sem internet no tablet/desktop e resolve conflitos | Teste de ponta a ponta com duas edições concorrentes |
| M6 | 1.0.0 | Assinatura paga, PDFs, exportar/excluir dados, app nas lojas | Checklist de loja (seção 6) e revisão ASVS de BE-31 |
| M7 | 1.1.0 | Usar o Dieta+ no navegador do computador ou do celular, nutri e paciente, sem instalar nada (online) | Chrome, Firefox e Safari; login, montar e publicar dieta, check-in do paciente; recarregar a página mantém a sessão |

As fases 1 a 3 do roadmap (`visao-geral.md`) já substituem o PDF da nutri; por isso o deploy de beta (BE-32a) foi antecipado para a onda 9, como o plano do backend recomendava.

## 4. Tarefas de design (canvas)

Feitas no canvas https://claude.ai/artifact/6thej5zGsTo81ZosiigGxC, seguindo `componentes.md` e as correções de `validacao.md`. Cada uma termina atualizando o status em `dieta-mobile/docs/design/telas.md` para "desenhada" e copiando os artboards novos ou alterados para `dieta-mobile/docs/design/artboards/` (agentes sem acesso ao claude.ai leem de lá). As tarefas DES ficam com o Claude, porque só ele edita o canvas. Branch `feature/DES-NN-…` no `dieta-mobile` para essa atualização de docs.

| Tarefa | Telas (composable) | Precisa estar pronta para |
| --- | --- | --- |
| DES-01 Entrada e conta | `CadastroPacienteScreen`, `ConsentimentoScreen`, `CadastroNutriScreen`, `RecuperarSenhaScreen`, `EscolherPerfilScreen`, correção do "Sou nutricionista" em Entrar | MOB-06, MOB-08 |
| DES-02 Núcleo da nutri | `ConvidarSheet`, `FolhaLimitePlano`, `PainelScreen` compacto, `FichaScreen` expandida (lista-detalhe), navegação da nutri com 5 entradas | MOB-07, MOB-28 |
| DES-03 Núcleo do paciente | `PerfilScreen`, estado "sem dieta" do `HojeScreen`, `OrientacoesScreen`, `AguaSheet`, `RegistrarPesoSheet`, os outros 11 passos da `AnamneseScreen` (molde do passo 7) | MOB-14, MOB-17, MOB-19, MOB-23, MOB-25 |
| DES-04 Dieta e alimentos | `AlimentosScreen`, `MontarDietaScreen` compacto, `PublicarDialog` (resumo do que mudou + aviso de push) | MOB-10 a MOB-13 |
| DES-05 Acompanhamento | `AgendaScreen` (dia e semana), `AvaliacaoScreen` compacto, `HistoricoAvaliacoesScreen`, `ModeloAnamneseScreen`, `RespostasAnamneseScreen`, `AvisosScreen` | MOB-22 a MOB-28 |
| DES-06 Conta, privacidade e desktop | `ContaScreen` (5 estados de assinatura), `AvisoSomenteLeitura`, `MinhasNutrisScreen`, `PrivacidadeScreen`, `PdfDietaScreen`, `IndicadorSync`, `DialogoConflito`, `PrimeiraSyncScreen` | MOB-29 a MOB-34 |
| DES-07 Web | Conferir no expandido (≥1280 px) as telas da nutri que o tablet já tem; `index.html` (carregando e "navegador não suportado"); aviso "instale o app para receber lembretes" do paciente | MOB-40, MOB-44 |

Estados carregando, vazio, erro e sem internet não ganham artboard próprio: usam `EstadoVazio`, `EstadoErro` e `FaixaOffline` de `componentes.md`.

## 5. O que este plano acrescenta aos planos de ação

Lacunas encontradas ao cruzar os planos com o canvas e os docs de design. Já estão incluídas nos `plano-de-acao.md` de cada repositório.

### Backend

| Tarefa | O que faz | Por quê |
| --- | --- | --- |
| BE-C1 a BE-C6 Contrato da fase | Records, controllers `501` anotados, `CodigoErro` novos e JSONs de exemplo da fase; teste que valida os exemplos contra os DTOs | Mobile trabalha em paralelo com mock fiel ao contrato |
| BE-05 (ampliada) | `GET /eu` devolve `perfis` (NUTRI e/ou PACIENTE) e o login aceita `perfil` opcional | O design tem `EscolherPerfilScreen` (mesma conta nutri e paciente) e o JWT tinha um perfil só |
| BE-32a Deploy de beta | A parte mínima de BE-32 (Dockerfile, `compose.prod.yaml` com Caddy, backup diário) num ambiente de beta | Nutri real testa a partir do M3 |
| BE-33 Perfil da nutri | `GET/PATCH /eu/nutri` (nome de exibição, CRN, telefone, orientação padrão); logo fica para BE-28 | A `ContaScreen` e a orientação padrão (RN-DIE) não tinham endpoint de edição |
| BE-34 Central de avisos | Tabela `aviso` (o que virou push também fica no histórico), `GET /eu/avisos`, `POST /eu/avisos/{id}/lido`; `FonteSync` | `AvisosScreen` (sino) não tinha fonte de dados |
| BE-35 Dados de exemplo | Changeset com `context: dev` (nunca roda em produção): nutri, 3 pacientes, a dieta de 2509 kcal e a lista de substituição dos PDFs | Integração do mobile e prints sem digitar tudo de novo |
| BE-37 Modo navegador | `WEB` em `Plataforma`, refresh só no cookie para a origem do site, versão mínima própria da web | A web em KMP (onda 16) usa o mesmo backend sem guardar refresh token no JavaScript |

### Mobile

| Tarefa | O que acrescenta |
| --- | --- |
| MOB-02 (ampliada) | Fase 0 de `plano-de-acao-ui.md`: corrigir `Color.kt` com todos os tokens, `Dimens`, `Shapes`, ícones |
| MOB-05 (ampliada) | Teste de contrato com os JSONs de `commonTest/resources/contrato/` |
| MOB-06 (ampliada) | `RecuperarSenhaScreen` e `EscolherPerfilScreen` |
| MOB-14 (ampliada) | `OrientacoesScreen` e o estado "sua nutri está montando sua dieta" |
| MOB-17 (ampliada) | `AguaSheet` |
| MOB-19 (ampliada) | `MinhasNutrisScreen` só leitura (lista de vínculos e nutri ativa); troca de nutri com cópia fica em P2 |
| MOB-22 (ampliada) | `HistoricoAvaliacoesScreen` com comparação de duas datas |
| MOB-23 (ampliada) | `RegistrarPesoSheet` (peso em casa, enviado pela fila de BE-15) |
| MOB-27 (ampliada) | `AvisosScreen` lendo BE-34 |
| MOB-35a Beta interno | Assinatura de debug/beta no CI, Play Console teste interno, TestFlight pelo Mac, MSI/DMG como artefato |
| MOB-36 Perfil da nutri | Parte "dados da nutri" da `ContaScreen` (nome, CRN, orientação padrão) com BE-33; assinatura entra em MOB-32 |
| MOB-40 a MOB-46 Web | Alvo `wasmJs`, separação do código offline (`offlineMain`), Portas só-API sobre `RecursoOnline`, publicação estática no Caddy (`web.md`) |

### Tarefas do Guilherme (ninguém mais consegue fazer)

| Tarefa | Quando | O quê |
| --- | --- | --- |
| G-01 Gitflow no GitHub | Onda 0 | Copiar `gitflow/workflows/*.yml` para `.github/workflows/` dos dois repos (e ajustar o `ci.yml` para rodar em `develop`), rulesets de `main` e `develop`, ruleset de tags `v*`, permissões do Actions, apagar branch depois do merge |
| G-02 Rede do ambiente | Onda 0 | Liberar `dl.google.com` e `api.adoptium.net` no Network access do ambiente na nuvem, para o mobile compilar aqui |
| G-03 Suposições | Até a onda 4 | Confirmar ou mudar S1 a S10 (`visao-geral.md`) e as suposições de design (`validacao.md`) |
| G-04 PDFs da nutri | Onda 5 | Deixar a pasta `doc-dieta` acessível para extrair o catálogo (BE-09) e a dieta de referência (BE-35) |
| G-05 Firebase | Onda 8 | Criar o projeto Firebase (só FCM) e guardar `google-services.json`, `GoogleService-Info.plist` e a credencial do servidor como secrets |
| G-06 Contas de loja | Onda 9 | Google Play Console e Apple Developer (com o Mac do amigo para o TestFlight) |
| G-07 VPS e domínio | Onda 9 | Escolher a VPS e o domínio (`api.dietamais.com.br`?) para o beta |
| G-08 Gateway | Onda 14 | Conta sandbox no Asaas (ou outro, S3) e a chave do webhook como secret |
| G-09 Releases | Cada marco | Aprovar o PR `release/X.Y.Z → main` |
| G-10 Arquivar o site Angular | Depois do ponto de decisão do MOB-43 | Arquivar `guilherme-oliveria/dieta-frontend` no GitHub (Settings → Archive) e tirá-lo do projeto; até lá ele fica parado como plano B |

## 6. Antes da loja (M6)

Fase 5 de `plano-de-acao-ui.md`: rodar cada tela em Android (celular e tablet), iPhone, iPad e desktop; safe area, gesto de voltar, permissões de notificação; conferir as regras da App Store e da Play Store para assinatura contratada fora do app (S4); ícone, splash e nome "Dieta+"; política de privacidade publicada; `426 VERSAO_APP_OBSOLETA` funcionando.

## 7. Como um agente pega a próxima tarefa

1. Abre este arquivo e acha a primeira onda que não terminou.
2. Pega, na coluna do seu repositório, a primeira tarefa `[ ]` do `plano-de-acao.md` cujas dependências estão `[x]`.
3. Cria a branch a partir de `develop` (ou usa a que o ambiente deu) e abre o PR para `develop`.
4. Se o endpoint ainda não existe, usa os JSONs do BE-Cn da fase com `MockEngine` e diz isso no PR.
5. No mesmo PR, marca `[x]` no `plano-de-acao.md`. Quando todas as tarefas de uma onda estiverem `[x]`, a última PR da onda marca a onda como concluída aqui (coluna "Integração").
