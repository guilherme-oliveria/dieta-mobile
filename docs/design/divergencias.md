# Divergências entre o app e o padrão de design

Revisão do Design e Usabilidade sobre a `develop` do `dieta-mobile` (até MOB-53), em 01/11/2026, contra [padrao-ui.md](padrao-ui.md), [graficos.md](graficos.md) e os artboards novos. Cada item diz onde está, o que o padrão pede e quem resolve. Marque `[x]` quando corrigido e cite o PR.

## Para corrigir (acessibilidade)

- [x] **D-01 · Paleta de gráficos** (`ui/comum/graficos/Graficos.kt`, `PaletaGraficos.serie`). O âmbar `#D9A21B` tem 2,3:1 sobre branco (mínimo 3:1 para gráfico) e o turquesa `#3C9C9A` e o cinza `#7A8B80` ficam em 3,3 e 3,6, no limite. Trocar para a série de [padrao-ui.md](padrao-ui.md#3-cores): `#B07A00`, `#7B5BA6`, `#2A7F7D`, `#66736A`. Mover as cores para `ui/theme` (hoje há `Color(0x…)` solto no arquivo, contra a regra de tokens). Resolve: executor, junto da próxima tarefa que mexer em gráfico (MOB-54). *(corrigido: séries em `ui/theme/Color.kt` (`SerieAmbar` `#B07A00` etc.) e `PaletaGraficos` usa os tokens (MOB-51b).)*

## Para alinhar com os artboards

- [x] **D-02 · Meus resultados sem régua e sem medidor** (`ui/paciente/evolucao/MeusResultadosTela.kt`). O artboard `P-Resultados` e o plano (seção 4.4) pedem o IMC numa **régua colorida** com a faixa marcada e a % de gordura num **medidor**. O app mostra só o chip e o texto; `ReguaFaixas` e `Medidor` existem e ainda não são usados fora dos previews. Resolve: executor (MOB-51b, pequena). *(corrigido: IMC na `ReguaFaixas` e % de gordura no `Medidor` em Meus resultados (MOB-51b).)*
- [ ] **D-03 · Meus resultados: série de gordura** O artboard `P-Resultados-Evolucao` mostra a linha de % de gordura abaixo da de peso. O app desenha só o peso (`seriePeso`). Depende de o backend devolver a série (verificar BE-41). Resolve: executor, sem urgência.
- [x] **D-04 · Régua de IMC no preview** (`PreviewsGraficos.kt`) usa o laranja de alerta para sobrepeso e obesidade. Pelo padrão, a régua usa as cores de faixa (`#9DB7D1`, `#7DB896`, `#E0B15C`, `#D98A5B`) e o laranja fica para o semáforo e o texto de alerta. Criar `PaletaFaixas` em `ui/theme`. Resolve: executor, junto do D-02. *(corrigido: `PaletaFaixas` em `ui/theme/Color.kt` (`FaixaAbaixo` a `FaixaObesidade`) e `trechosReguaImc` (MOB-51b).)*
- [ ] **D-05 · "Gerar PDF" no passo Resumo.** O artboard `A-Avaliacao-Resumo` tem o botão ao lado de "Salvar avaliação"; o app ainda não tem (depende de BE-46). Entra quando o backend entregar o PDF; abrir pelo link no navegador, como o PDF da dieta (MOB-33).
- [ ] **D-06 · Botão "i" de explicação** nos números da nutri (fórmula, referência, frase), previsto no plano 4.1 e em [padrao-ui.md](padrao-ui.md#5-padrões-de-interação). Não está nos passos de Composição e Necessidades. Resolve: executor, após o BE entregar os textos.

## Observações (sem ação agora)

- **MOB-55 · metadados da lista publicada.** O snapshot traz alimentos, medidas e gramas, mas não traz o nome e a energia-base de todos os grupos. O app usa o nome dos itens das refeições do mesmo snapshot e só deriva energia-base de itens por alimento (totais/porções). Onde faltar referência, exibe "nome não informado" ou "Energia por porção não informada", conforme RN-CAL-04; não substitui por referência atual ou de outra nutri. Para preencher todos os cabeçalhos dos artboards será necessário um campo aditivo congelado no backend.
- **MOB-55 · quantidade do substituto em item por grupo.** Sem a energia-base congelada do grupo, o app não consegue converter a opção escolhida na dieta para as medidas de todos os alimentos da lista. A troca de item por grupo mostra o alimento e pede confirmação da medida com a nutri, em vez de exibir gramas equivalentes incorretas. O registro da troca continua disponível; itens por alimento mantêm o cálculo anterior. A resolução completa depende dos metadados aditivos mencionados acima.
- **MOB-55 · legenda e fonte ampliada.** Gramas e percentuais ficam em duas linhas por macro para preservar leitura em 390 dp. Com fonte ampliada, as kcal do dia saem do centro da rosca e aparecem acima, evitando sobrepor o anel. O diâmetro permanece 128 dp no dia e 96 dp na refeição. Não há anel de meta. A lista mantém o nome da nutri e a data da publicação do snapshot, em vez de sugerir que uma lista imutável foi atualizada depois.

- **Meus protocolos** está implementado como cartão dentro da Conta da nutri (`ProtocolosNutriCard`). O artboard `N-Protocolos` mostra o conteúdo em tela cheia, que é como o cartão deve se comportar no celular ao ser aberto. Mesma estrutura de chips e interruptores; sem ação.
- A **rosca de metas** do passo Necessidades usa o ponto médio das faixas, como no artboard; o anel de meta (`anelMeta`) só passa a ser usado no planejador por porções (MOB-54), onde compara o montado com a meta.
- Telas do paciente e da nutri anteriores a MOB-47 (Hoje, Dieta, Compras, Pacientes, Painel, Dieta da nutri) não foram reauditadas aqui; seguem a validação de 24/10 ([validacao.md](validacao.md)).
