# Tarefas pequenas de usabilidade (PO, 12/11/2026)

Lote de 10 tarefas de poucas horas cada, uma tela ou componente por tarefa, todas no `dieta-mobile` (a `develop` em `be180d3`). Os IDs usam o prefixo `MOB-UX-nn` para não colidir com a numeração do `plano-de-acao.md`; ao entrar no plano, o Guilherme (ou o coordenador) renumera para o próximo `MOB-nn` livre (hoje o último é MOB-80).

## Como estas tarefas foram escolhidas

- Li `docs/design/padrao-ui.md`, `divergencias.md`, o `plano-de-acao.md` e o código das telas `PacientesTela`, `HojeTela`, `ComprasTela`, `AgendaTela` e `MontarDietaTela`. **Não rodei o app**; cada problema abaixo vem do código ou da documentação, e a coluna "Base" diz qual dos dois. Onde marquei **(conferir)**, o dev confirma o comportamento antes de começar; se já estiver certo, a tarefa vira só um teste.
- Dois públicos pesaram na escolha: a **nutri com 100 a 1000 pacientes**, no celular ou tablet (achar, filtrar e agir rápido), e o **paciente no celular**, com uma mão e pouca atenção (registrar a refeição sem errar).
- Telas já desenhadas seguem o artboard; nenhuma tarefa abaixo cria tela nova, então **não precisam de artboard novo**. As que mudam uma tela desenhada devem pedir a atualização do artboard no mesmo PR (regra 4 de `padrao-ui.md`).
- Regras de sempre: texto em `strings.xml`, componentes de `ui/comum`, alvo de toque de 48 dp, fonte a 200%, teste de ViewModel ou `dominio` quando houver regra, sem `if (plataforma)`.

## Lista

| ID | Título | Tela | Quem ganha |
| --- | --- | --- | --- |
| MOB-UX-01 | Busca fixa e filtros recolhidos na lista de pacientes | Pacientes (nutri) | Nutri, 100+ pacientes |
| MOB-UX-02 | Contagem, "Limpar filtros" e vazio específico na lista de pacientes | Pacientes (nutri) | Nutri, 100+ pacientes |
| MOB-UX-03 | Ordenar a lista de pacientes (A-Z, retorno mais próximo) | Pacientes (nutri) | Nutri, 100+ pacientes |
| MOB-UX-04 | Desfazer o registro de refeição | Hoje (paciente) | Paciente |
| MOB-UX-05 | Água: desfazer o último copo e valor livre | Hoje (paciente) | Paciente |
| MOB-UX-06 | Seletor de dia do Hoje com rótulos claros e alvo de 48 dp | Hoje (paciente) | Paciente |
| MOB-UX-07 | Lista de compras lembra o que já foi marcado | Compras (paciente) | Paciente |
| MOB-UX-08 | Ações da consulta num menu "mais" | Agenda (nutri) | Nutri, celular |
| MOB-UX-09 | Totais da dieta sempre visíveis ao montar | Montar dieta (nutri) | Nutri, celular e tablet |
| MOB-UX-10 | Teclado certo e "Próximo" nos formulários de medidas | Avaliação (nutri) | Nutri, tablet e celular |

## Detalhe

### MOB-UX-01 · Busca fixa e filtros recolhidos na lista de pacientes
- **Base:** código (`PacientesTela.kt`). Antes do primeiro paciente a tela empilha título, uso do plano, dois botões, busca, 4 chips de status, o resumo de atenção, 3 chips de atenção, o índice A-Z e os atalhos. No celular, o primeiro nome da lista fica abaixo da dobra, e com 500 pacientes a busca some assim que a nutri rola.
- **Problema:** a ação mais frequente (achar um paciente pelo nome) fica longe e some ao rolar.
- **Critério de aceite:** (1) a caixa de busca fica fixa no topo da lista ao rolar (`stickyHeader`); (2) os chips de status e de atenção ficam atrás de um único chip "Filtros" com a contagem de filtros ativos, que abre uma folha com as opções; no tablet e no desktop (largura grande) os chips continuam visíveis; (3) o primeiro paciente aparece sem rolar num celular de 390 × 800 dp; (4) os testes de e2e que usam `buscarPaciente` continuam verdes; (5) o artboard `ART-P1-01` é atualizado no mesmo PR.
- **Tela afetada:** `ui/nutri/pacientes/PacientesTela`.
- **Estimativa:** 3 a 4 h.

### MOB-UX-02 · Contagem, "Limpar filtros" e vazio específico na lista de pacientes
- **Base:** código. Com busca ou filtro que não acha ninguém, a lista mostra o vazio genérico (`pacientes_lista_vazia`), igual ao de quem ainda não tem paciente nenhum. Também não há como saber quantos pacientes o filtro devolveu nem como desligar todos de uma vez.
- **Problema:** a nutri com muitos pacientes não sabe se o filtro está escondendo gente.
- **Critério de aceite:** (1) uma linha "N pacientes" (ou "N de M" com filtro) acima da lista, em região ao vivo; (2) com busca ou filtro sem resultado, o vazio diz "Nenhum paciente com 'texto'" ou "Nenhum paciente neste filtro" e traz o botão "Limpar filtros"; (3) o botão "Limpar filtros" também aparece quando há filtro ativo e há resultado; (4) o vazio de "ainda sem pacientes" continua como está; (5) teste de ViewModel para a contagem.
- **Tela afetada:** `PacientesTela`, `PacientesViewModel`.
- **Estimativa:** 2 a 3 h.

### MOB-UX-03 · Ordenar a lista de pacientes
- **Base:** código. A lista é sempre A-Z (agrupada por inicial); o resumo já traz `proximaConsulta` e `ultimaAvaliacaoEm`.
- **Problema:** com 100+ pacientes, a nutri quer ver "quem vem primeiro" sem filtrar.
- **Critério de aceite:** (1) duas opções de ordem, "Nome (A-Z)" e "Próximo retorno", dentro da folha de Filtros do MOB-UX-01 (ou chips, se o MOB-UX-01 não entrou); (2) em "Próximo retorno", quem não tem consulta marcada vai para o fim; o índice A-Z e as letras de grupo somem; (3) a escolha vale até a nutri trocar (guardada no aparelho); (4) função pura de ordenação em `dominio/pacientes` com teste; (5) offline, sem resumo carregado, a opção fica desabilitada com o motivo em texto. *Depende de:* MOB-UX-01.
- **Tela afetada:** `PacientesTela`, `dominio/pacientes`.
- **Estimativa:** 3 h.

### MOB-UX-04 · Desfazer o registro de refeição
- **Base:** código (`HojeTela.kt`). "Comi", "Troquei" e "Pulei" gravam o check-in direto; não vi caminho para corrigir um toque errado (a refeição deixa de mostrar os botões quando já tem check-in). **(conferir)** se a Dieta ou outra tela permite alterar.
- **Problema:** um toque errado no botão (eles ficam lado a lado, com a mão ocupada) vira dado errado no relatório da nutri, sem saída.
- **Critério de aceite:** (1) depois de registrar, aparece um aviso "Refeição registrada · Desfazer" por 5 s; (2) tocar em "Desfazer" remove ou restaura o check-in anterior (usando o contrato que já existe; se o backend não aceitar remover, usar a regra "último check-in vale" e dizer isso no PR); (3) o aviso é lido pelo leitor de tela e tem alvo de 48 dp; (4) funciona offline (entra na fila como qualquer check-in); (5) teste de ViewModel.
- **Tela afetada:** `ui/paciente/hoje/HojeTela`, `ui/paciente/dieta` (ViewModel).
- **Estimativa:** 3 a 4 h.

### MOB-UX-05 · Água: desfazer o último copo e valor livre
- **Base:** código. Há três botões fixos (+200, +300, +500 ml) e nenhum jeito de corrigir.
- **Problema:** toque duplo acidental soma água que o paciente não bebeu; quem bebe 250 ml não tem botão.
- **Critério de aceite:** (1) botão "Desfazer último" (texto, 48 dp) que subtrai o último lançamento da sessão, desabilitado quando não há nenhum; (2) botão "Outro valor" que abre um campo numérico em ml (10 a 2.000, mensagem com a faixa aceita); (3) o progresso "X de Y ml" muda em região ao vivo; (4) o backend continua decidindo o limite; (5) teste do cálculo.
- **Tela afetada:** cartão de água do `HojeTela`.
- **Estimativa:** 2 a 3 h.

### MOB-UX-06 · Seletor de dia do Hoje com rótulos claros e alvo de 48 dp
- **Base:** código. Os dias são `BotaoTexto` com "Hoje" ou "09/11"; só o dia selecionado muda de estilo e não há "Ontem".
- **Problema:** o paciente que esqueceu de registrar ontem precisa ler datas e adivinhar; os botões de texto são pequenos para toque rápido.
- **Critério de aceite:** (1) rótulos "Hoje", "Ontem" e depois "dd/MM" com o dia da semana abreviado ("seg 09/11"); (2) o dia selecionado usa `ChipEscolha` com check (padrão de escolhas); (3) cada chip com 48 dp de altura, em linha com rolagem horizontal quando não couber; (4) descrição de acessibilidade com a data por extenso; (5) com fonte a 200% nada é cortado.
- **Tela afetada:** `HojeTela` (função `rotuloDia`).
- **Estimativa:** 2 h.

### MOB-UX-07 · Lista de compras lembra o que já foi marcado
- **Base:** documentação (`plano-de-acao.md`, MOB-44: "itens marcados só em memória") e código (`ComprasTela.kt`: `estado.marcados`). **(conferir)** se no Android e no iOS os marcados já persistem; na web continuam só em memória por regra.
- **Problema:** no mercado o paciente alterna entre apps; ao voltar, a lista perdeu o que ele já pegou.
- **Critério de aceite:** (1) no app (Android, iOS, desktop) os itens marcados ficam guardados no aparelho, por lista e por semana, e voltam ao reabrir; (2) "Limpar marcados" apaga também o guardado; (3) o cabeçalho mostra "X de Y itens" e os itens marcados vão para o fim da lista (ou ficam riscados, o que o artboard `P-Compras` indicar); (4) não é dado clínico: sem log; (5) na web nada muda (sem fila nem banco); (6) teste de repositório ou ViewModel.
- **Tela afetada:** `ui/paciente/compras/ComprasTela`, `ComprasViewModel`.
- **Estimativa:** 3 a 4 h.

### MOB-UX-08 · Ações da consulta num menu "mais"
- **Base:** código (`AgendaTela.kt`). Cada cartão de consulta mostra "Remarcar", "Cancelar" e "Financeiro" (e "Abrir financeiro" fora de cancelada), cada um um botão.
- **Problema:** três botões por cartão, em dia cheio, deixam a agenda poluída e o "Cancelar" fica colado ao "Remarcar" no toque, com risco de cancelar sem querer.
- **Critério de aceite:** (1) o cartão mostra a ação mais provável no contexto como botão (consulta futura: "Remarcar"; consulta que já passou: "Financeiro") e as demais num menu "Mais ações" (ícone de 48 dp com `contentDescription`); (2) "Cancelar" pede confirmação ("Cancelar a consulta de Maria às 14:00?") com o botão do diálogo "Cancelar consulta" e o neutro "Manter"; (3) continua desabilitado sem rede, com o motivo em texto; (4) ordem de foco segue a ordem visual.
- **Tela afetada:** `ui/nutri/agenda/AgendaTela`.
- **Estimativa:** 3 h.

### MOB-UX-09 · Totais da dieta sempre visíveis ao montar
- **Base:** código (`MontarDietaTela.kt`). O cartão de totais (kcal e macros) e os botões Salvar/Publicar ficam no fluxo da rolagem, junto das refeições. O documento `padrao-ui.md` pede barra de ações fixa no pé (`RodapeAcoes`) e o total em número grande com "de {meta} kcal".
- **Problema:** ao adicionar alimento em uma refeição lá embaixo, a nutri perde o total de vista e precisa rolar para conferir; em dietas longas isso é a repetição mais comum.
- **Critério de aceite:** (1) uma faixa compacta fixa no topo da área de rolagem com kcal do dia e os 3 macros em texto ("1.850 de 2.000 kcal · C 52% · P 22% · L 26%"), atualizada em região ao vivo; (2) Salvar e Publicar no `RodapeAcoes`, com a nota "salve antes de publicar" mantida; (3) no tablet o cartão de totais continua na coluna lateral e a faixa não aparece; (4) sem mudança de regra nem de contrato; (5) o artboard do planejador (`A/T-Planejador-Porcoes`) é conferido e, se divergir, atualizado.
- **Tela afetada:** `ui/nutri/dieta/MontarDietaTela` e o planejador por porções.
- **Estimativa:** 4 h.

### MOB-UX-10 · Teclado certo e "Próximo" nos formulários de medidas
- **Base:** não verificado no código. **(conferir)** em `AvaliacaoTela` e `CampoNumero` se todo campo numérico abre o teclado numérico com ponto/vírgula e se a tecla do teclado vai para o próximo campo.
- **Problema:** medir é digitar dezenas de números em sequência, muitas vezes no tablet em pé; fechar o teclado ou trocar de teclado a cada campo custa minutos por avaliação.
- **Critério de aceite:** (1) todo `CampoNumero` abre o teclado decimal; (2) a tecla de ação do teclado é "Próximo" e passa o foco ao campo seguinte, e "Concluir" no último do passo; (3) vírgula e ponto aceitos como separador decimal; (4) a rolagem leva o campo com foco para fora do teclado; (5) teste de `CampoNumero` para o parse e o `ImeAction`. Se já estiver tudo certo, a tarefa se reduz ao teste e ao registro disso em `padrao-ui.md`.
- **Tela afetada:** `ui/comum/CampoNumero` e passos de `ui/nutri/avaliacao`.
- **Estimativa:** 2 a 3 h.

## Ordem sugerida

1. MOB-UX-04, 05 e 06 (paciente, nada depende de nada, ganho imediato no uso diário).
2. MOB-UX-01, 02 e depois 03 (nutri com muitos pacientes; a 03 depende da 01).
3. MOB-UX-07, 08, 09 e 10 em qualquer ordem.

## Fora desta lista (ideias para depois)

- Busca que ignora acento e abreviações no nome do paciente: **(conferir)** se já funciona; se não, vira tarefa própria.
- Ver todas as avaliações de um paciente em uma tabela única, e atalho "ligar/WhatsApp" na ficha: precisam de decisão de produto.
- Lembretes de refeição configuráveis por horário (MOB-27b e push iOS dependem do Mac).
