# MOB-55 — macros e lista do paciente

Execução da especificação já autorizada em `handoff-codex.md`, `padrao-ui.md` seção 8b e artboards P-Dieta-Macros, P-Refeicao-Macros, P-Lista-Substituicao, P-Lista-Substituicao-Vazio e T-Lista-Substituicao.

- [ ] Testar funções puras de percentuais energéticos dos três macros e agrupamento/busca exclusivamente na lista congelada, sem inventar referência ausente.
- [ ] Testar estado do ViewModel: abrir lista, pesquisar, filtrar e expandir um grupo; restaurar seleção ao trocar de snapshot; troca por item válida offline.
- [ ] Implementar roscas do dia/refeição pela biblioteca comum, com gramas e %, tabela, 128/96 dp, sem anel de meta; itens por porções, texto livre e à vontade.
- [ ] Implementar lista compacta recolhível e lista-detalhe ampla, com busca, chips e estados vazio/offline/erro; ligar em Android/iOS/desktop e web sem lógica por plataforma.
- [ ] Rodar `:shared:jvmTest`, conferir tamanhos e fonte ampliada, atualizar design/plano e abrir PR para QA independente.

O snapshot fornece alimentos e medidas congelados, mas a lista de alimentos não traz metadados de grupos. Nome do grupo vem dos itens das refeições do mesmo snapshot. Energia de uma porção-base só pode ser derivada de item por alimento (totais/porções); item por grupo pode usar uma opção diferente da base. Quando não houver referência suficiente, mostrar uma frase de dado ausente, nunca usar catálogo de outra nutri ou número fictício (RN-CAL-04). Essa limitação do contrato será declarada como divergência no PR.
