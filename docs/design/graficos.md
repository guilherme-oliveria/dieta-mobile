# Gráficos

Referência: artboard `G-Graficos` (catálogo) e o código em `shared/src/commonMain/kotlin/org/br/dietamais/ui/comum/graficos/`. Regras gerais em [padrao-ui.md](padrao-ui.md).

## Qual gráfico usar

| Quero mostrar | Gráfico | Composable | Onde aparece |
| --- | --- | --- | --- |
| Partes de um todo (composição corporal, macros) | **Rosca** (pizza com furo), 2 a 4 fatias | `GraficoRosca` | Avaliação passos 2 e 3, Meus resultados, planejador, dieta do paciente |
| O que está montado contra a meta | **Rosca com anel de meta** | `GraficoRosca(anelMeta = …)` | Planejador (tablet e celular) |
| Um valor em faixas (IMC, exame, escore Z) | **Régua de faixas** com marcador | `ReguaFaixas` | Meus resultados, exames, avaliação infantil |
| Um valor numa escala com faixa ideal (% de gordura, água) | **Medidor** (meia-lua) | `Medidor` | Meus resultados, Hoje (água) |
| Evolução no tempo | **Linha**, com faixa de referência sombreada | `GraficoLinhas` | Evolução, Meus resultados, PDF |
| Comparar itens (kcal por refeição) | **Barras** com marca de meta | `GraficoBarras` | Planejador, ficha |
| Crescimento da criança | **Curva de crescimento** (P3, P15, P50, P85, P97) com o ponto | `CurvaCrescimento` | Avaliação infantil, curva do responsável |

Pizza cheia **não** é usada: a rosca é a pizza do Dieta+. Mais de 4 fatias vira barras. Dado que muda muito no tempo vira linha, não rosca.

## Regras

1. **Rótulo e valor em texto** ao lado de cada fatia, ponto ou barra (legenda com bolinha, rótulo e valor). A cor sozinha nunca basta.
2. **Cores** da série na ordem fixa da paleta (verde, azul, âmbar, roxo, turquesa, cinza); laranja só em alerta. Cores e contraste em [padrao-ui.md](padrao-ui.md#3-cores).
3. **Fatias da rosca** com 2 dp de espaço entre elas; espessura de 16% do diâmetro (20% no celular pequeno); o centro mostra o número principal (`26,5%` com "gordura"; `1.920` com "de 2.889").
4. **Anel de meta:** no paciente não aparece anel de meta (padrao-ui.md seção 8b); ele vê somente o planejado. No planejador da nutri: anel de dentro = montado; anel de fora, a 35% de opacidade = meta. A legenda mostra "55% · meta 57%".
5. **Régua:** segmentos com rótulo embaixo e marcador (triângulo) com o valor em cima; o valor também vem por extenso no cartão ("27,5 · sobrepeso"). Faixas de IMC: abaixo, normal, sobrepeso, obesidade.
6. **Linha:** eixo Y com 4 marcas, rótulos do eixo X abreviados (Ago, Set), pontos de 5 dp com miolo branco, faixa de referência sombreada com borda tracejada e legenda ("Faixa saudável (68,9 a 92,8 kg)"). Mínimo 2 pontos; com menos, mostra a frase "Ainda não há medidas suficientes".
7. **Curva:** mediana (P50) em verde contínuo, demais percentis em cinza tracejado, rótulos P3, P50 e P97 na ponta; ponto da criança em azul com borda branca. A frase em palavras fica logo abaixo ("percentil 62: pesa mais que 62 de cada 100 crianças").
8. **Tamanhos:** celular 150 a 180 dp de diâmetro (rosca) e largura total do cartão; tablet até 220 dp. Altura de linha 150 a 190 dp; curva 190 a 230 dp.
9. **"Ver como tabela"** em todo gráfico de dados (`GraficoComTabela`); alvo de 48 dp; a tabela é a alternativa para leitor de tela e para quem prefere números.
10. **Acessibilidade:** `contentDescription` com os valores por extenso e a ordem de leitura das fatias; sem animação quando o sistema reduz movimento.
11. **Estados:** sem dados = anel/eixo cinza e a frase do que falta; carregando = esqueleto; erro = frase curta + "Tentar de novo".
12. **PDF:** os mesmos gráficos, versão estática (sem animação, sem "ver como tabela", com a tabela impressa ao lado) e a mesma paleta.

## Exemplos de dados para previews

Rosca de composição: gordura 27,1 kg, massa magra 75,4 kg (26,5% de gordura). Com protocolo muscular: gordura 27,1; músculo 38,0; osso 12,7; residual 24,7. Macros do VET 2.889 kcal: carboidrato 55 a 60%, proteína 15 a 20%, lipídio 25 a 30%. Peso: 108,0 / 106,2 / 104,1 / 102,5 kg com faixa saudável de 68,9 a 92,8 kg. Dados de criança são ilustrativos até o backend entregar as tabelas da OMS (BE-54).
