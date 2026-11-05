# Divergências entre o app e o padrão de design

Revisão do Design e Usabilidade sobre a `develop` do `dieta-mobile` (até MOB-53), em 01/11/2026, contra [padrao-ui.md](padrao-ui.md), [graficos.md](graficos.md) e os artboards novos. Cada item diz onde está, o que o padrão pede e quem resolve. Marque `[x]` quando corrigido e cite o PR.

## Para corrigir (acessibilidade)

- [x] **D-01 · Paleta de gráficos** (`ui/comum/graficos/Graficos.kt`, `PaletaGraficos.serie`). O âmbar `#D9A21B` tem 2,3:1 sobre branco (mínimo 3:1 para gráfico) e o turquesa `#3C9C9A` e o cinza `#7A8B80` ficam em 3,3 e 3,6, no limite. Trocar para a série de [padrao-ui.md](padrao-ui.md#3-cores): `#B07A00`, `#7B5BA6`, `#2A7F7D`, `#66736A`. Mover as cores para `ui/theme` (hoje há `Color(0x…)` solto no arquivo, contra a regra de tokens). Resolve: executor, junto da próxima tarefa que mexer em gráfico (MOB-54). *(corrigido: séries em `ui/theme/Color.kt` (`SerieAmbar` `#B07A00` etc.) e `PaletaGraficos` usa os tokens (MOB-51b).)*

## Para alinhar com os artboards

- [x] **D-02 · Meus resultados sem régua e sem medidor** (`ui/paciente/evolucao/MeusResultadosTela.kt`). O artboard `P-Resultados` e o plano (seção 4.4) pedem o IMC numa **régua colorida** com a faixa marcada e a % de gordura num **medidor**. O app mostra só o chip e o texto; `ReguaFaixas` e `Medidor` existem e ainda não são usados fora dos previews. Resolve: executor (MOB-51b, pequena). *(corrigido: IMC na `ReguaFaixas` e % de gordura no `Medidor` em Meus resultados (MOB-51b).)*
- [ ] **D-03 · Meus resultados: série de gordura** O artboard `P-Resultados-Evolucao` mostra a linha de % de gordura abaixo da de peso. O app desenha só o peso (`seriePeso`). Depende de o backend devolver a série (verificar BE-41). Resolve: executor, sem urgência.
- [x] **D-04 · Régua de IMC no preview** (`PreviewsGraficos.kt`) usa o laranja de alerta para sobrepeso e obesidade. Pelo padrão, a régua usa as cores de faixa (`#9DB7D1`, `#7DB896`, `#E0B15C`, `#D98A5B`) e o laranja fica para o semáforo e o texto de alerta. Criar `PaletaFaixas` em `ui/theme`. Resolve: executor, junto do D-02. *(corrigido: `PaletaFaixas` em `ui/theme/Color.kt` (`FaixaAbaixo` a `FaixaObesidade`) e `trechosReguaImc` (MOB-51b).)*
- [ ] **D-05 · "Gerar PDF" no passo Resumo.** O artboard `A-Avaliacao-Resumo` (hoje `A-Avaliacao-Graficos-2`, ART-P1-10) tem o botão ao lado de "Salvar avaliação"; o app ainda não tem (depende de BE-46). Entra quando o backend entregar o PDF; abrir pelo link no navegador, como o PDF da dieta (MOB-33).
- [ ] **D-06 · Botão "i" de explicação** nos números da nutri (fórmula, referência, frase), previsto no plano 4.1 e em [padrao-ui.md](padrao-ui.md#5-padrões-de-interação). Não está nos passos de Composição e Necessidades. Resolve: executor, após o BE entregar os textos.

- [x] **D-07 · Lista de substituição do paciente não é acordeão.** O artboard `P-Lista-Substituicao` mostra os grupos recolhidos com "1 porção = N kcal · M itens" e a medida caseira embaixo do nome de cada alimento; o app mostra o cabeçalho do grupo como botão de texto (ver `evidencias/AUD-01/lista-substituicao.png`). Resolve: executor.
- [x] **D-08 · Chip de estado da refeição na dieta.** O artboard `P-Dieta-Macros` mostra Feita, Agora ou Depois no fim de cada linha; o app mostra só horário e kcal (`DietaPacienteTela`). Resolve: executor.
- [x] **D-09 · Avaliação infantil sem "Resultados ao vivo".** `A-Avaliacao-Infantil` tem o cartão com a régua Z; o app mostra "Resultados por idade" só depois de sincronizar (limite do contrato BE-55b, ver observações abaixo). Resolvida em MOB-AUD-09: prévia online pelo `POST /pacientes/{id}/avaliacoes/previa` (BE-55c), com pausa na digitação; sem rede o painel segue em "disponível após salvar". Mostra intervalos, não valor exato (sem LMS).
- [x] **D-10 · Telas sem barra superior própria.** Meus exames, Meus resultados, Protocolos e Necessidades ficam dentro do cartão ou da aba do host e não têm a barra do padrão. Resolvida em MOB-AUD-10: Meus exames e Meus resultados ganharam a barra (#81); Necessidades é um passo do assistente de avaliação e já herda a barra e o rodapé fixo (#73); Protocolos é cartão da Conta da nutri e o artboard `N-Protocolos` mostra o conteúdo aberto em tela cheia, o que o cartão faz no celular ao ser aberto (ver observações abaixo). Sem ação restante.

## Observações (sem ação agora)

- **MOB-59 · cobertura histórica das referências.** O gráfico lê os cortes congelados dos snapshots disponíveis, agrupados por paciente, vínculo, sexo, fonte e versão. Liga somente meses consecutivos publicados e mostra marcas isoladas quando faltam vizinhos, sem inventar referências nos meses ausentes; a tabela explicita os oito cortes e as medidas na idade real. Não troca a referência histórica pelo catálogo atual de `/referencias/oms` (consulta de uma linha, sem versão). O artboard de curva contínua e percentil exato exige adequação para essa cobertura; o texto usa o intervalo confirmado. Não é uma tabela OMS completa para todas as idades.

- **MOB-58 · percentil e escore Z.** `A-Avaliacao-Infantil` apresenta valores exatos ao vivo; o contrato aprovado de BE-55b fornece intervalos entre cortes publicados, sem LMS para interpolação. A tela mostra os intervalos confirmados pelo servidor, sem marcador exato, e remove derivados após alterar entradas. O artboard precisa refletir esses limites da fonte. A curva histórica de MOB-59 continua pendente.
- **MOB-58 · grade e ações.** Campos usam os componentes comuns, com unidade no rótulo: duas colunas no compacto, três em largura maior e uma com fonte 200%. O painel de resultados ocupa 360 dp quando há espaço; a leitura do paciente respeita a largura de leitura. As ações ficam ao final da rolagem do assistente existente, em vez de rodapé fixo do artboard; capturas/testes conferem acesso por rolagem com fonte 200%. D-06 permanece pendente.
- **MOB-58 · referência G-16.** A inconsistência aprovada da tabela Frisancho é apresentada como resultado indisponível com motivo legível; o cliente não reordena cortes nem inventa classificação.

- **MOB-55 · metadados da lista publicada.** O snapshot traz alimentos, medidas e gramas, mas não traz o nome e a energia-base de todos os grupos. O app usa o nome dos itens das refeições do mesmo snapshot e só deriva energia-base de itens por alimento (totais/porções). Onde faltar referência, exibe "nome não informado" ou "Energia por porção não informada", conforme RN-CAL-04; não substitui por referência atual ou de outra nutri. Para preencher todos os cabeçalhos dos artboards será necessário um campo aditivo congelado no backend.
- **MOB-55 · quantidade do substituto em item por grupo.** Sem a energia-base congelada do grupo, o app não consegue converter a opção escolhida na dieta para as medidas de todos os alimentos da lista. A troca de item por grupo mostra o alimento e pede confirmação da medida com a nutri, em vez de exibir gramas equivalentes incorretas. O registro da troca continua disponível; itens por alimento mantêm o cálculo anterior. A resolução completa depende dos metadados aditivos mencionados acima.
- **MOB-55 · legenda e fonte ampliada.** Gramas e percentuais ficam em duas linhas por macro para preservar leitura em 390 dp. Com fonte ampliada, as kcal do dia saem do centro da rosca e aparecem acima, evitando sobrepor o anel. O diâmetro permanece 128 dp no dia e 96 dp na refeição. Não há anel de meta. A lista mantém o nome da nutri e a data da publicação do snapshot, em vez de sugerir que uma lista imutável foi atualizada depois.

- **Meus protocolos** está implementado como cartão dentro da Conta da nutri (`ProtocolosNutriCard`). O artboard `N-Protocolos` mostra o conteúdo em tela cheia, que é como o cartão deve se comportar no celular ao ser aberto. Mesma estrutura de chips e interruptores; sem ação.
- A **rosca de metas** do passo Necessidades usa o ponto médio das faixas, como no artboard; o anel de meta (`anelMeta`) só passa a ser usado no planejador por porções (MOB-54), onde compara o montado com a meta.
- Telas do paciente e da nutri anteriores a MOB-47 (Hoje, Dieta, Compras, Pacientes, Painel, Dieta da nutri) não foram reauditadas aqui; seguem a validação de 24/10 ([validacao.md](validacao.md)).

## MOB-64a (ART-P1-02 a 08), pendências no MOB-64c
- Hub em tela própria por seção (ART-P1-03 a 08): no app há só o resumo das seções no passo Medidas. Fica para o MOB-64c.
- Cartão "Última avaliação" da ficha (ART-P1-02): não feito. Os atalhos levam às abas Avaliações e Evolução. Fica para o MOB-64c.
- Barras verticais com siglas (ART-P1-09 e ART-P1-10): resolvido no MOB-64b.

## MOB-67a (ART-P1-01), pendências no MOB-67b
- Filtros de atenção (Retorno hoje, Avaliação vencida, Sem retorno) e o resumo "M precisam de atenção": a lista do backend não traz agenda nem data da última avaliação por paciente. O app mantém os filtros de status (Todos, Convidados, Ativos, Encerrados).
- "Fixados e recentes": sem fixar nem histórico de abertura no backend.
- Índice A-Z: aparece como linha de letras sob os filtros (não como régua lateral), só com as iniciais que existem e a partir de 12 pacientes.

## MOB-79 · Financeiro da consulta (05/11/2026)

- **Sem artboard próprio.** A decisão G-20 veio depois dos artboards da Parte 4; a tela segue `componentes.md` e `padrao-ui.md` (diálogo em tela cheia como o formulário da consulta, chips de escolha, campo com unidade "R$", chave "Pago"). Pedir o artboard ao agente de Design e Usabilidade.
- O resumo do mês aparece no fim da Agenda como um cartão de texto (consultas, compareceram, faltaram, pendentes, recebido, a receber e recebido por forma). Gráfico e filtro por período ficam para depois.
- Os valores usam `Double` no app (como os demais números do contrato) e só entram na tela formatados em reais; a conta de verdade é do servidor.

## MOB-76 · Lista modelável (05/11/2026)

- A decisão G-19 trocou o "modelo de lista" por **copiar a lista de outro paciente da nutri** (RN-ALI-06). O artboard `C-Listas-Tipos` (ART-P3-06) mostra os 5 tipos de lista e não cobre a cópia: o app só ganhou, na lista do paciente, o botão "Copiar a lista de outro paciente", a escolha da origem e uma confirmação (a troca é da lista inteira e descarta edições não salvas). Pedir ao agente de Design e Usabilidade o artboard da cópia.

