# Handoff para o Codex — dieta-mobile

Escrito em 01/11/2026 pelo agente executor (Claude) ao passar as tarefas restantes das ondas 20 a 23 para o Codex. Continue **exatamente** daqui.

## Como trabalhar

1. Leia `AGENTS.md` e, nesta ordem: `docs/arquitetura/plano-de-acao.md` (linhas MOB-49 em diante), `docs/arquitetura/plano-de-execucao.md` (marcos), `docs/arquitetura/mobile.md`, `integracao.md`, `regras-de-negocio.md` (RN-DIE-10 a 13, RN-MOD, RN-EXA-01, RN-RES-01, RN-CAL-04), `docs/design/padrao-ui.md` (**seção 8b** e as demais), `docs/design/graficos.md`, `docs/design/telas.md`, `docs/design/componentes.md`, `docs/design/divergencias.md`. O plano de origem é `plano-melhorias-planilhas.md` (cópia em `/mnt/project-files/planos/` do projeto Claude; o backend tem `docs/arquitetura/plano-melhorias-planilhas.md`).
2. **Design:** os artboards são HTML estáticos em `docs/design/artboards/*.dc.html` (abra no navegador; capturas em `docs/design/evidencias/`). Tela só entra com artboard; no PR cite o artboard e as divergências.
3. Uma tarefa por PR, branch `feature/MOB-NN-descricao` a partir da `develop`, PR para a `develop` com o ID no título. Commits em português no imperativo. Marque `[x]` no `plano-de-acao.md` no mesmo PR.
4. Antes do PR: `./gradlew :shared:jvmTest` (e `:shared:testAndroidHostTest` se mexer em Android). Se `dl.google.com` estiver bloqueado, escreva código e testes e confie no CI.
5. Endpoint ainda inexistente no backend: `MockEngine` com o JSON do contrato, dito no PR. As cópias de `contrato-fase-*`, `regras-de-negocio` e `integracao` vêm do backend: não divirja; mudou lá, copie.
6. Revisão: vale o fluxo de QA do `AGENTS.md` (você abre o PR e não aprova nem mescla o próprio trabalho).
7. **Release:** o app segue os mesmos marcos do backend (M8 = 1.2.0, M9 = 1.3.0, M10 = 1.4.0; ver `plano-de-execucao.md`). Fechou todas as tarefas de um marco: crie `release/X.Y.Z` a partir da `develop`, ajuste versão e `CHANGELOG`, rode os testes e abra o PR `release/X.Y.Z → main` para o Guilherme aprovar. **Nunca** faça merge na `main` nem edite `.github/`.

## O que já está implementado (na `develop`)

- Avaliação do adulto e necessidades (MOB-49 a 53), planejador por porções (MOB-54a/b), modelos de dieta: biblioteca (MOB-56a) e aplicar ao paciente (MOB-57, com `EscalaPorcoes`).
- Exames: o paciente vê "Meus exames" com a régua de faixa (MOB-60a, `MeusExamesTela`, só se `compartilhamento.exames`) e a nutri lança exames na avaliação (MOB-60b, `ExamesNutriTela`, catálogo online-only).
- Artboards prontos (DES-11, PR #59) para o MOB-55 e para os PDFs da lista.

## O que falta (nesta ordem)

### 1. MOB-55 — paciente: lista de substituição e roscas de macros
- Artboards: `P-Dieta-Macros` (rosca do dia), `P-Refeicao-Macros` (rosca da refeição), `P-Lista-Substituicao`, `P-Lista-Substituicao-Vazio` (estado vazio) e `T-Lista-Substituicao` (tablet). Regra: `padrao-ui.md` seção 8b. A rosca do paciente tem **3 fatias** (carboidrato, proteína, lipídio) e **sem anel de meta**.
- Fonte dos dados: o snapshot da dieta recebido (`/eu/dietas`, sync): itens com `porcoes`, `medidaCaseira`, `grupoNome`, totais e `listaSubstituicao`. Mostre "N porções · medida" e "Troque por 1 porção do grupo X" com o botão "Trocar" (reaproveite a tela de substituição existente). Telas leem do SQLDelight (offline-first); regra de cliente em função pura em `dominio/` com teste; ViewModel com `StateFlow`; strings em `strings.xml`; 3 tamanhos de janela.

### 2. MOB-61 — anamnese estruturada (depende do BE-57b no backend)
Artboards `A-Anamnese-Estruturada` (nutri) e `P-Anamnese` (paciente). Cálculos de PA, álcool, sal e óleo vêm derivados do servidor; o app mostra, não decide. Sem o BE-57b mesclado, use `MockEngine`.

### 3. MOB-58 e MOB-59 — criança e adolescente (dependem do BE-55b)
MOB-58: avaliação infantil e adolescente, artboard `A-Avaliacao-Infantil`. MOB-59: curva de crescimento da OMS para a nutri e o responsável, artboard `P-Curva-Crescimento` (siga `graficos.md`). Referência de curvas: `GET /referencias/oms` e Frisancho no backend.

### Bloqueadas (não faça)
- **MOB-56b** (editar o conteúdo do modelo e leitura offline): depende da escrita offline **MOB-29b** (dietas, listas e modelos), ainda não feita.
- **MOB-35a, MOB-27b, MOB-31b, MOB-18b, MOB-33b, MOB-43/45/46**: dependem de contas, lojas ou `.github/` do Guilherme, ou já têm pendência registrada no plano.

## Sem artboard ainda (peça ao agente de Design pelo coordenador)
Micronutrientes e adequação à DRI no planejador, produtos e suplementos, editar modelo e "salvar dieta como modelo" (ver `telas.md`, seção "Sem artboard").

## Para o relatório final
**G-15** (marcadores e os 9 modelos iniciais precisam da nutri Luciana) e **G-16** (Frisancho AMBc masc. 4 anos); os docs comuns devem ser cópias do backend.
