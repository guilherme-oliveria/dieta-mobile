# Índice: artboard por tarefa

Para quem vai implementar (Codex, Claude, pessoas). Cada `.dc.html` desta pasta é a **especificação visual** de uma tela: abra o arquivo e leia o HTML (layout, cores, tamanhos, textos e dados de exemplo). Eles não abrem sozinhos no navegador (dependem do `support.js` do canvas), mas as fotos (PNG) de cada um estão em [../evidencias/DES-08-10/](../evidencias/DES-08-10/) e [../evidencias/DES-11/](../evidencias/DES-11/). Antes de implementar leia [../padrao-ui.md](../padrao-ui.md) e [../graficos.md](../graficos.md); as divergências conhecidas do app estão em [../divergencias.md](../divergencias.md). Regra: o PR cita o artboard e lista as divergências.

Estado conferido na `develop` em 01/11/2026 (plano de ação, linha de MOB-49 em diante).

## Mobile pendente

| Tarefa | O que falta | Artboards (nesta pasta) | Ver também |
| --- | --- | --- | --- |
| **MOB-55** Paciente: rosca de macros e lista de substituição | Implementado; limitações de metadados em divergencias.md | `P-Dieta-Macros`, `P-Refeicao-Macros`, `P-Lista-Substituicao`, `P-Lista-Substituicao-Vazio`, `T-Lista-Substituicao` | `padrao-ui.md` seção 8b; `graficos.md` (rosca sem anel de meta para o paciente) |
| **MOB-56b** Editar o conteúdo do modelo e leitura offline | Edição do modelo reaproveita o planejador | `A-Planejador-Porcoes`, `T-Planejador-Porcoes` (edição); biblioteca em `A-Modelos-Biblioteca` (já feita, 56a) | Sem artboard próprio da edição: use o planejador com o título "Editar modelo" e sem paciente |
| **MOB-58** Avaliação infantil e adolescente | Tudo | `A-Avaliacao-Infantil` | `plano-melhorias-planilhas.md` 3.3 (campos por faixa) |
| **MOB-59** Curva de crescimento (nutri e responsável) | Tudo | `P-Curva-Crescimento` (usa `CurvaCrescimento`, já existe em `ui/comum/graficos`) | `graficos.md` item "Curva" |
| **MOB-61** Anamnese estruturada (álcool, sal, óleo) | Tudo | `A-Anamnese-Estruturada` | `plano-melhorias-planilhas.md` 3.5 (fórmulas) |
| Divergências D-03, D-05, D-06 | Série de gordura em Meus resultados, "Gerar PDF" no Resumo, botão "i" de explicação | `P-Resultados-Evolucao`, `A-Avaliacao-Resumo` | `divergencias.md` |

## Mobile já entregue (a tela conferida com o artboard)

| Tarefa | Artboards |
| --- | --- |
| MOB-50 Avaliação em passos | `A-Avaliacao-Medidas`, `A-Avaliacao-Composicao`, `A-Avaliacao-Resumo`, `T-Avaliacao-Passos` |
| MOB-51 e MOB-51b Meus resultados | `P-Resultados`, `P-Resultados-Evolucao`, `P-Resultados-Vazio` |
| MOB-52b Necessidades | `A-Avaliacao-Necessidades` |
| MOB-53 Meus protocolos | `N-Protocolos` |
| MOB-54b Planejador por porções | `A-Planejador-Porcoes`, `T-Planejador-Porcoes` |
| MOB-56a e MOB-57 Modelos | `A-Modelos-Biblioteca`, `A-Modelo-Aplicar` |
| MOB-60a e MOB-60b Exames | `P-Exames`, `A-Exames-Nutri` |

## Referência de PDF (para o backend; o app só abre o PDF)

| Tarefa | Artboards |
| --- | --- |
| BE-46 PDF da avaliação | `PDF-Avaliacao` |
| BE-50b PDF da lista de substituição (pequena, média, grande, com contagem de carboidratos) | `PDF-Lista-Pequena`, `PDF-Lista-Media`, `PDF-Lista-Grande`, `PDF-Lista-Carboidratos` |
| BE-50 PDF da dieta | **sem artboard**: use o layout de `PDF-Avaliacao` (cabeçalho, rodapé com CRN) com a coluna "Substituições: 1 porção do grupo X" |

Nos PDFs de lista o desenho varia só as gramas; a medida caseira também deve escalar (arredondando para ¼).

## Padrão e catálogo

`G-Graficos` (catálogo dos gráficos e estados) e `canvas.json` (posição de cada artboard no canvas https://claude.ai/artifact/6thej5zGsTo81ZosiigGxC). Telas anteriores a 24/10 (Hoje, Refeição, Trocar, Compras, Evolução, Lembretes, Anamnese passo 7, Entrar, Pacientes, Ficha, Painel, Dieta, Avaliação tablet) estão listadas em [README.md](README.md).

## Sem artboard ainda (peça ao Design antes, ou siga `padrao-ui.md` e os componentes)

Micronutrientes e adequação à DRI (aba do planejador), suplementos e produtos no planejador, editar modelo (usa o planejador), PDF da dieta (BE-50).
