# Padrão visual e de usabilidade

Para quem implementa (pessoas, Claude e Codex). Complementa [componentes.md](componentes.md) (tokens e componentes), [graficos.md](graficos.md) (os gráficos) e [plataformas.md](plataformas.md) (tamanhos de janela). Se algo aqui contradiz o código, **vale este documento** e o código é corrigido (ver [divergencias.md](divergencias.md)).

Os desenhos das telas ficam no canvas https://claude.ai/artifact/6thej5zGsTo81ZosiigGxC e, em cópia, em [artboards/](artboards/README.md). O mapa tela → artboard está em [telas.md](telas.md).

## 1. Regra de ouro: artboard antes da tela

1. **Tela nova só entra no app com artboard aprovado**, antes ou junto da implementação. O artboard é desenhado pelo agente de Design e Usabilidade (tarefas `DES-nn` do plano de execução) no canvas e copiado para `docs/design/artboards/`.
2. Antes de abrir uma tarefa `MOB-nn` que cria ou muda tela, o agente confere em [telas.md](telas.md) se a tela está **desenhada**. Se não está: pede o artboard à thread de Design e Usabilidade (via coordenador) e, para não parar, segue a descrição de telas.md e os componentes existentes, dizendo isso no PR.
3. O PR de tela cita o artboard (`Avaliacao-Necessidades`, por exemplo) e lista as **divergências** em relação a ele, cada uma com o motivo. Sem lista, vale "igual ao artboard".
4. Mudou o comportamento de uma tela desenhada (campo novo, passo novo, estado novo)? O mesmo PR pede a atualização do artboard. O Design atualiza o canvas e esta pasta.
5. O Design revisa o app contra este padrão e registra os achados em [divergencias.md](divergencias.md). Divergência com impacto de acessibilidade ou de regra de negócio vira tarefa na próxima onda; as demais, no PR da tela que for mexida.

## 2. Princípios

- **Uma ideia por passo.** Fluxos longos viram passos com "Passo 2 de 4 · Composição" e barra de progresso (avaliação, anamnese, aplicar modelo). Cada passo cabe numa tela de celular sem rolar muito.
- **Resultado ao vivo, sem surpresa.** Número calculado aparece enquanto a nutri digita, com o selo "prévia" e a nota de que o valor final vem do servidor. Dado que falta vira uma frase do que falta ("informe punho e joelho para calcular o peso ósseo"), **nunca zero** (RN-CAL-04).
- **Cor nunca é a única pista.** Todo estado tem palavra ("Alto", "Dentro", "Abaixo") e, nos gráficos, rótulo e valor em texto.
- **Laranja só para alerta.** Nada de laranja como cor de marca, de botão ou de série de gráfico.
- **O paciente lê sem siglas.** "MG", "SP", "GEB", "VET", "Z" não aparecem para o paciente; a nutri vê a sigla com o "i" de explicação. IMC é a única sigla aceita para o paciente, sempre com a classificação por extenso.
- **Mostre pouco, no momento certo.** O paciente só vê o que a nutri compartilhou (RN-AVA-05, por seção) e uma frase de contexto, não a tabela inteira.

## 3. Cores

Tokens de [componentes.md](componentes.md) sem mudança. Acrescentam-se estas regras de uso:

| Situação | Cor | Observação |
| --- | --- | --- |
| Semáforo "bom / dentro" | `VerdeDieta` + chip `feita` | Sempre com a palavra |
| Semáforo "atenção / limítrofe" | `InfoDieta` + chip `info` | Azul, não amarelo: o amarelo não passa em contraste |
| Semáforo "alerta / alto / baixo" | `AlertaDieta` no ícone e na barra, `AlertaTexto` no texto, chip `alerta` | Nunca vermelho (daltonismo) |
| Faixas de régua (abaixo, normal, sobrepeso, obesidade) | `#9DB7D1`, `#7DB896`, `#E0B15C`, `#D98A5B` | Cores só de gráfico (`PaletaFaixas`); cada faixa tem rótulo, então o contraste de 3:1 não é exigido, mas o marcador e o texto do valor são `TextoDieta` |
| Séries de gráfico | verde `#2E7D4F`, azul `#2F6FA8`, âmbar `#B07A00`, roxo `#7B5BA6`, turquesa `#2A7F7D`, cinza `#66736A` | Todas com 3:1 ou mais sobre branco e sobre `#F5F6F1`. Ordem fixa. O âmbar antigo `#D9A21B` (2,3:1) **sai** |
| Faixa de referência sombreada | `VerdeDieta` a 16% de opacidade + linha tracejada | Em linhas e curvas |
| Anel de meta (rosca) | cor da série a 35% de opacidade | Anel de fora = meta; anel de dentro = o que está montado |

## 4. Tipografia, espaçamento e forma

Valem as tabelas de [componentes.md](componentes.md). Em complemento:

- **Margem lateral:** 16 dp nas telas de rolagem com cartões; 20 dp nos formulários; 24 dp no tablet e desktop.
- **Cartões** empilhados: 10 a 12 dp entre eles; dentro do cartão, 8 a 10 dp entre linhas e 14 a 16 dp de preenchimento.
- **Formulário de medidas:** 2 campos por linha no celular (peso/estatura, idade/punho); 3 por linha no tablet. Rótulo acima do campo, unidade dentro do campo à direita (`kg`, `cm`, `mm`), valor em 16 sp / 700.
- **Barra de ações fixa** no pé do celular: botão principal à direita ocupando o resto; "Voltar" contornado à esquerda. Altura 52 dp.
- **Painel de resultados** no tablet e desktop: coluna fixa de 360 dp à direita; no celular, o mesmo conteúdo vira um cartão "Resultados ao vivo" abaixo dos campos.
- **Largura de leitura** máxima 560 dp no celular grande e tablet em retrato; a lista-detalhe usa a janela inteira.
- **Alvo de toque** 48 dp (chips, passos de quantidade, interruptores, botão "Ver como tabela").
- **Fonte a 200%:** nenhum controle com altura fixa; textos longos quebram, nunca cortam.

## 5. Padrões de interação

### Passos (wizard)
Título da tela, linha "Passo n de N · Nome" (região ao vivo, para o leitor de tela), barra de progresso, conteúdo, barra de ações. "Continuar" valida só o passo; erro aparece abaixo do campo, em `AlertaTexto`, com a faixa aceita ("Informe um valor entre 40 e 200"). Voltar mantém o que foi digitado. Fechar no meio pede confirmação se houver dado novo.

### Escolhas
2 a 4 opções curtas: chips selecionáveis com "✓" (ícone de check) e rótulo; 5 ou mais: lista com busca. Interruptor para sim/não. Opções que dependem de sexo ou faixa etária **somem**, não ficam desabilitadas.

### Quantidades (porções)
Passo de quantidade `− n +`, com frações de 0,5 e a palavra "livre" para "à vontade". Cada linha mostra o nome, a porção em kcal e a medida caseira, em duas linhas.

### Metas versus montado
Sempre o mesmo trio: carboidrato, proteína, lipídio; barra curta por macro com o valor e a palavra ("dentro", "acima", "abaixo"). Verde dentro da faixa; laranja fora, com a palavra. Total de kcal em número grande com "de {meta} kcal".

### Compartilhar com o paciente
Interruptor geral "Compartilhar esta avaliação" e, ligado, um interruptor por seção (medidas, composição, resumo e metas, exames). O texto abaixo diz o que o paciente passa a ver.

### Explicar um número
Número calculado da nutri tem um botão "i" de 48 dp com a fórmula, a referência e uma frase. Para o paciente, a explicação já está em palavras no próprio cartão.

## 6. Estados de toda tela

| Estado | Como aparece |
| --- | --- |
| Carregando | Esqueletos cinza (`SuperficieSuave`) no formato do conteúdo; sem rodinha solta |
| Vazio | Ícone em círculo `VerdeDietaClaro`, título de uma frase, uma linha de ajuda e, se houver ação, um botão |
| Erro | Frase curta dizendo o que fazer, "Tentar de novo"; o código do erro só em log |
| Sem internet | `FaixaOffline` no topo; mostra o que está salvo; desabilita só o que exige rede e diz por quê |
| Dado faltando | Frase do que falta, em `TextoSecundarioDieta`; nunca zero nem "—" sozinho |
| Gráfico sem dados | Anel ou eixo cinza (`BordaDieta`) e a frase do que falta |
| Somente leitura | Aviso `AvisoSomenteLeitura`; campos viram texto |
| Conflito | `DialogoConflito`; nunca perde dado em silêncio |

## 7. Textos

- Português do Brasil, em `strings.xml`. Frases curtas, voz ativa, sem culpa ("Informe um valor entre 40 e 200", não "Valor inválido").
- **Botões** começam com verbo no infinitivo: "Salvar avaliação", "Criar rascunho e revisar", "Gerar PDF". Evitar "OK" e "Enviar".
- **Números:** vírgula decimal (102,5 kg); milhar com ponto (2.889 kcal); unidade separada por espaço (`kg`, `cm`, `mm`, `mg/dL`, `kcal`); porcentagem colada (26,5%); intervalo "68,9 a 92,8 kg"; datas 09/10/2026 e "09/10" quando o ano está claro.
- **Classificações** por extenso e no gênero certo ("Sobrepeso", "Alto", "Baixo", "Limítrofe", "Normal", "Adequado").
- **Para o paciente:** linguagem de conversa e sem diagnóstico. Exame sempre fecha com "converse com a sua nutri". Criança: "percentil 62 significa que pesa mais que 62 de cada 100 crianças da mesma idade".
- **Prévias e estimativas** levam a nota "Prévia. O valor final vem do servidor".

## 8. Acessibilidade (conferir em todo PR de tela)

- `heading()` nos títulos de tela e de seção; região ao vivo (`liveRegion`) no resultado que muda sozinho e na linha do passo.
- Todo botão só de ícone tem `contentDescription`; todo campo tem rótulo visível.
- Gráfico: descrição de acessibilidade com os valores por extenso, e "Ver como tabela" visível (ver [graficos.md](graficos.md)).
- Contraste: texto 4,5:1; elementos gráficos 3:1 (séries e marcadores). Verificar com `ContrasteTelasTest`.
- Ordem de foco segue a ordem visual; foco visível; alvo de toque de 48 dp.
- Movimento: animações de entrada curtas (600 ms) e desligadas quando o sistema pede menos movimento.

## 9. PDF

O PDF da avaliação, da dieta e das listas é gerado no backend (BE-46, BE-50) com a **mesma paleta e os mesmos gráficos** destes padrões, em A4 retrato, margem de 30 pt, texto de corpo a partir de 10,5 pt, cabeçalho com a marca "D+", nome da nutri e CRN, e rodapé com CRN, "Gerado pelo Dieta+" e página. O artboard `PDF-Avaliacao` é a referência do layout; o app só abre o PDF (link no navegador, MOB-33).

## 10. Checklist do PR de tela

- [ ] Artboard citado; divergências listadas com motivo (ou "igual ao artboard").
- [ ] Tokens e componentes de `ui/comum`; nenhum hex, dp ou texto solto fora de `ui/theme` e `strings.xml`.
- [ ] Quatro estados (carregando, vazio, erro, sem internet) e dado faltando sem zero.
- [ ] Celular e tablet conferidos (e desktop, se a tela existe lá); fonte a 200%.
- [ ] Gráficos pela biblioteca `ui/comum/graficos`, com tabela alternativa.
- [ ] Acessibilidade da seção 8.
- [ ] `telas.md` atualizado para "implementada".
