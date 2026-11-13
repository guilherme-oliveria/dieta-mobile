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
- Filtros de atenção (Retorno hoje, Avaliação vencida, Sem retorno) e o resumo "M precisam de atenção": entregues no MOB-67b com o BE-66. Os dados vêm de `GET /pacientes` e não entram no sync; offline os chips somem e a lista fica como antes. "Avaliação vencida" = mais de 90 dias ou nenhuma (valor inicial, a confirmar com a nutri); "precisam de atenção" soma vencida e sem retorno (Retorno hoje é só um atalho).
- "Fixados e recentes": sem fixar nem histórico de abertura no backend.
- Índice A-Z: aparece como linha de letras sob os filtros (não como régua lateral), só com as iniciais que existem e a partir de 12 pacientes.

## MOB-79 · Financeiro da consulta (05/11/2026)

- **Sem artboard próprio.** A decisão G-20 veio depois dos artboards da Parte 4; a tela segue `componentes.md` e `padrao-ui.md` (diálogo em tela cheia como o formulário da consulta, chips de escolha, campo com unidade "R$", chave "Pago"). Pedir o artboard ao agente de Design e Usabilidade.
- O resumo do mês aparece no fim da Agenda como um cartão de texto (consultas, compareceram, faltaram, pendentes, recebido, a receber e recebido por forma). Gráfico e filtro por período ficam para depois.
- Os valores usam `Double` no app (como os demais números do contrato) e só entram na tela formatados em reais; a conta de verdade é do servidor.

## MOB-76 · Lista modelável (05/11/2026)

- A decisão G-19 trocou o "modelo de lista" por **copiar a lista de outro paciente da nutri** (RN-ALI-06). O artboard `C-Listas-Tipos` (ART-P3-06) mostra os 5 tipos de lista e não cobre a cópia: o app só ganhou, na lista do paciente, o botão "Copiar a lista de outro paciente", a escolha da origem e uma confirmação (a troca é da lista inteira e descarta edições não salvas). Pedir ao agente de Design e Usabilidade o artboard da cópia.

## MOB-78 · Cadastro ampliado (05/11/2026)

- O artboard `D-Cadastro-Paciente` (ART-P4-03) agrupa os dados em seções. O app entrega os campos como uma seção recolhível ("Mais dados do cadastro") no formulário de novo paciente e como linhas na ficha; a organização em seções com cartões e a edição da ficha ficam para depois (ainda não há tela de edição do paciente). A conferir com o artboard pelo agente de Design e Usabilidade.
- Escolaridade e estado civil têm chips de sugestão e aceitam texto livre (RN-VIN-08: o servidor não fixa os valores). `uf` vira maiúsculas e `cep` vai só com os 8 dígitos, como o servidor normaliza.

## MOB-66 · Evolução em barras (05/11/2026)

- O artboard `D-Evolucao` (ART-P4-05) mostra só as barras de Peso, % gordura e Cintura por mês. No app, as barras são uma alternativa ao gráfico de linha já existente (mesmo seletor de séries e intervalo), não uma tela nova; o rótulo é "mês/ano" curto (ex.: 08/26) e mostra as últimas 6 medidas. Sem variação entre barras nem destaque da última por enquanto.

## MOB-65 · Micronutrientes DRI (05/11/2026)

- O artboard `D-Micronutrientes` (ART-P4-04) coloca a lista na tela de Necessidades da avaliação, com valor e unidade ("8 mg · adequado"). O servidor só entrega a adequação do dia da **dieta** (total, meta, percentual e classe, sem unidade), então o cartão ficou na dieta do paciente da ficha, mostrando "% da meta" e a classe em texto. Para mostrar valor com unidade e para ir à tela de Necessidades, o backend precisaria devolver a unidade (e uma meta DRI fora de uma dieta): a decidir com o Design.
- Nomes legíveis ficam numa tabela do app; código desconhecido vira texto com a primeira letra maiúscula.


## MOB-73 · PDFs da lista de substituição (05/11/2026)

- Não há artboard para estes botões (ART-P3 ainda não foi desenhado). O app mostra quatro botões de texto ao lado de "Baixar PDF" na dieta publicada, na ficha da nutri e na tela da dieta do paciente. O Design pode trocá-los por um menu único.
- Os botões ficam desabilitados sem conexão, como o PDF da dieta: o link é gerado na hora pelo servidor.

## MOB-72 · Micronutrientes no planejador (05/11/2026)

- Não há artboard para este cartão (ART-P3). O app reaproveita o cartão do MOB-65 abaixo do "Resultado ao vivo". Micronutrientes e adequação só o servidor calcula (o planejador soma só energia e macros), então o cartão mostra o último valor salvo e, com alterações pendentes, pede para salvar de novo.

## MOB-77 · Anamnese por temas (05/11/2026)

- O artboard `D-Anamnese-Secoes` (ART-P4-02) mostra um cartão por aba da planilha, cada um com um subtítulo ("Histórico, medicamentos, sintomas"). O backend só entrega o `titulo` do bloco, sem subtítulo, então o cartão mostra o título e "n de m respondidas". Nenhuma mudança de backend foi necessária (BE-67 dispensado); se o Design quiser o subtítulo, é um campo opcional novo no bloco.
- Os cartões aparecem na leitura das respostas pela nutri (primeiro aberto, o resto recolhido). O preenchimento pelo paciente segue em passos ("Passo n de m", `A-Anamnese-Estruturada`), que já usa os mesmos blocos.

## MOB-65b · Unidade nos micronutrientes (05/11/2026)

- O artboard `D-Micronutrientes` (ART-P4-04) mostra "8 mg · adequado". Com o BE-67 o app mostra o total na unidade da meta ("8 mg") ao lado da % e da classe em texto. A tela de Necessidades da avaliação continua sem a lista: o servidor só calcula a adequação sobre a dieta (RN-DIE-13), então ela segue na dieta do paciente e na ficha da nutri. Levar a lista à avaliação exige um cálculo de adequação sem dieta, que não existe.

## MOB-56b · Editor de modelo de dieta (05/11/2026)

- Não há artboard do editor de modelo (ART-P3). O editor reaproveita o planejador por porções (`A/T-Planejador-Porcoes`): o botão "Salvar" e os textos falam de dieta, e o título é o único campo de cabeçalho. Descrição, condição e restrições do modelo seguem as do modelo aberto (modelo novo nasce sem elas); a energia de referência (RN-MOD-02) é o VET das metas editadas ou, sem metas, a do modelo aberto, para a aplicação ao paciente continuar escalando.
- Só modelo da nutri sem alimento do catálogo abre para editar; o do sistema continua só leitura (duplicar primeiro, RN-MOD-01). A leitura offline fica para a MOB-56c.

## MOB-70 · Produtos e suplementos (05/11/2026)

- Não há artboard do cadastro de produto (ART-P3 pendente). A tela segue o padrão de Alimentos (`padrao-ui.md`): escolha Alimentos | Produtos no topo da aba, lista de cartões, formulário em tela cheia com os 4 macros por 100 g (energia, carboidrato, proteína, lipídeos).
- O formulário só edita nome, marca, classificação, medida caseira e os 4 macros; apresentação, embalagem, sabores, observação e os micronutrientes são mantidos como vieram do servidor. A busca por alimentos e produtos no planejador (MOB-71) vem em outro PR.

## MOB-71 · Produto como item do planejador (05/11/2026)

- Sem artboard (ART-P3 pendente). O item por produto usa o mesmo cartão do item por porção, com a quantidade em gramas (− e + de 10 g) e a energia do item; o seletor é uma busca por nome ou marca dentro da refeição, ao lado de "Adicionar grupo".
- O total do dia e as metas ao vivo continuam só com os itens por grupo; o produto entra no total mostrado depois de salvar, calculado pelo servidor.

## MOB-67c · Fixados e recentes (05/11/2026)

- O artboard ART-P1-01/ART-P1-12 mostra a seção "Fixados e recentes" no topo da lista. Como o backend não guarda fixados nem histórico, os fixados e os 5 últimos abertos ficam só no aparelho (não sincronizam entre aparelhos; na web, só enquanto a página estiver aberta). Fixar é um botão de texto no cartão do paciente, não o ícone de alfinete do desenho.
- Os atalhos aparecem como chips em duas linhas (Fixados, Recentes) e somem quando há busca ou filtro ativo.

## MOB-64c · Cartão Última avaliação (05/11/2026)

- O cartão na aba Resumo da ficha mostra data, peso, IMC e classificação em texto, com o botão "Ver avaliações". Vem do resumo da lista de pacientes (`ultimaAvaliacao`), então aparece offline só se o resumo já foi carregado. O hub por seção (ART-P1-03 a 08) ficou para o MOB-64d.

## MOB-74 · Dieta e Refeição do paciente (05/11/2026)

- Auditoria contra P-Dieta-Macros e P-Refeicao-Macros: o subtítulo agora é "Dieta publicada em dd/mm/aaaa", o botão da lista de substituição vai depois das refeições e a refeição ganhou a nota "Cada porção é a mesma energia".
- Fica diferente do desenho: o link "Ver como tabela" (sem tela de tabela no MVP) e os gramas por item na refeição (o backend não manda gramas por porção). A Lista de substituição não foi alterada: busca e chips por grupo já existem.

## MOB-68 · Avaliação infantil (05/11/2026)

- Auditoria contra A-Avaliacao-Infantil e P-Curva-Crescimento: faltava só a régua do escore Z nos resultados ao vivo da nutri; ela agora marca o ponto médio do intervalo devolvido pelo backend, com a faixa −2 a +2 destacada. O paciente não vê Z (regra já existente).
- Segue diferente do desenho: os rótulos da régua são um texto único, sem posição por número.

## MOB-69 · Gráficos de 10 a 17 anos (05/11/2026)

- Os 4 gráficos de barras aparecem também para 10 a 17 anos (idade de `resultadosInfantis`, ou faixa ADOLESCENTE se faltar). Menor de 10 anos segue só com a curva de crescimento. O peso mínimo e máximo vem do cálculo infantil.

## MOB-64d · Hub das seções da avaliação (05/11/2026)

- O passo Medidas (adulto) abre no hub ART-P1-03 com as seções Básicas, Perímetros, Dobras e Complementares; tocar numa linha abre só os campos daquela seção, com "Voltar às seções". O estado de cada seção fica em texto ("2 de 5 medidas") e uma seção com erro avisa.
- Diferente do desenho: Cadastro (ART-P1-04) e Peso × estatura (ART-P1-05) continuam juntos na seção Básicas, a Composição segue como passo próprio e Gráficos e Resumo ficam no último passo. Cada seção é uma tela do mesmo fluxo, não uma rota separada, e a seção aberta não sobrevive a girar a tela.
- Avaliação infantil não muda.

## MOB-FIX · Troca de alimento no Hoje (07/11/2026)

- Artboards ART-P3-10 (`P-Troca-Alimento`) e ART-P3-11 (`P-Troca-Alimento-Vazio`). A tela implementa topo com voltar, busca, lista paginada só da lista do paciente, Confirmar fixo e menu inferior visível.
- Diferente do desenho: o desenho usa linhas com círculo e o bloco "Você vai trocar"; o app usa botões de texto com ✓ no escolhido, sem o bloco.
- O desenho tem "Carregar mais"; o app carrega mais 30 ao rolar.
- Sem resultado, o desenho diz "Nenhum substituto encontrado"; o app mostra "Nenhum alimento encontrado".

## MOB-UX-04 · Desfazer o registro de refeição (12/11/2026)

- O canvas `Main.dc.html` (Hoje) e o detalhe da refeição não têm o estado "Refeição registrada · Desfazer". O app mostra, por 5 s, uma faixa escura com o texto e o botão "Desfazer" no topo do conteúdo. Divergência até o Design atualizar o canvas.

## MOB-UX-05 · Água no Hoje (12/11/2026)

- O cartão de água ganhou "Outro valor" (campo de ml de 10 a 2.000, inline) e "Desfazer último". Não estão no canvas `Main.dc.html`; divergência até o Design atualizar.
- "Desfazer último" só alcança lançamentos ainda não enviados (5 s depois do toque), porque o backend só inclui água e não remove. Depois disso o botão fica desabilitado.

## MOB-UX-06 · Seletor de dia do Hoje (12/11/2026)

- Os dias viram `ChipEscolha` (48 dp, check no escolhido) em linha com rolagem horizontal, com "Hoje", "Ontem" e "seg 09/11"; a descrição de acessibilidade traz a data por extenso. O canvas `Main.dc.html` ainda mostra os botões de texto; divergência até o Design atualizar.

## MOB-UX-01 · Busca fixa e filtros recolhidos (12/11/2026)

- ART-P1-01 atualizado: a busca fica fixa no topo ao rolar e os chips de situação e de atenção ficam atrás do chip "Filtros (n)", que abre uma folha ("Situação" e "Atenção", com "Concluir"). No tablet e no desktop (largura grande) os chips continuam à vista, em linha, abaixo da busca.
- O índice A-Z passa a ser uma única linha com rolagem horizontal (antes quebrava em várias linhas e empurrava o primeiro paciente para fora da tela). O resumo "N ativos · M precisam de atenção" continua visível acima da lista.
- Corrigido junto: o salto do índice A-Z errava a posição quando os atalhos (fixados e recentes) estavam visíveis.

## MOB-UX-02 · Contagem e vazio da lista de pacientes (13/11/2026)

- Acima da lista aparece "N pacientes" (ou "N de M pacientes" com busca ou filtro) em região ao vivo, e "Limpar filtros" ao lado quando há busca ou filtro ligado. "Limpar filtros" desliga busca, situação e atenção de uma vez.
- Sem resultado, a lista diz "Nenhum paciente com “texto”." ou "Nenhum paciente neste filtro." com o botão "Limpar filtros"; o vazio de quem ainda não tem paciente fica como estava. Estados ainda não desenhados no canvas (ART-P1-01).

## MOB-UX-03 · Ordem da lista de pacientes (13/11/2026)

- A folha "Filtros" (e, em tela larga, a linha de filtros) ganhou a seção "Ordem" com "Nome (A-Z)" e "Próximo retorno". Em "Próximo retorno" a lista fica plana: quem tem retorno mais próximo primeiro, quem não tem consulta marcada no fim, sem letras de grupo e sem o índice A-Z.
- A escolha fica guardada no aparelho, por conta (`registro_sync`, coleção `ordem-pacientes-local`; na web só vale com a página aberta). "Próximo retorno" depende dos resumos da lista: sem eles (offline ou ainda carregando) o chip fica desabilitado com o texto do motivo e a lista segue A-Z. Seção ainda não desenhada no canvas (ART-P1-01).

## MOB-UX-07 · Lista de compras lembra o que foi marcado (13/11/2026)

- Já persistia no app (Android, iOS, desktop): `ComprasRepositorio` guarda os marcados em `registro_sync` (coleção `compras-marcados`), por conta; na web continua só em memória. Acrescentado: "X de Y itens" abaixo do título (região ao vivo), item marcado riscado e em cinza, e "Desmarcar tudo" passou a "Limpar marcados".
- Divergência do artboard `P-Compras`: o artboard põe o "X de Y itens" na linha do subtítulo ("Da dieta de 13/10 · 3 de 10 itens"); no app ele fica logo acima da lista, e o subtítulo com a data da dieta não foi implementado. Os marcados não vão para o fim da lista (só riscados, como no artboard), para o item não pular sob o dedo.
- A marca é por alimento e por conta, não por lista e semana: ao trocar o período (3, 7, 14 dias) ou a dieta, quem continua na lista continua marcado, e a contagem só considera itens da lista atual.
