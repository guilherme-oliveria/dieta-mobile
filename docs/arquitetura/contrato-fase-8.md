# Contrato da fase 8 — dieta por porções, metas e modelos (BE-C8)

Cópia oficial no backend. Parte do plano das planilhas ([plano-melhorias-planilhas.md](plano-melhorias-planilhas.md), onda 20 em diante). Erros seguem o `ProblemDetail` de [integracao.md](integracao.md). Vetores de cálculo em `src/test/resources/contrato/calculos/dieta-porcoes.json` (conferidos por `DietaPorcoesVetoresTest` e, no mobile, pelo mesmo arquivo).

Tudo aqui é **aditivo**: campos novos são opcionais, o contrato da fase 3 (dieta por alimento e porções do grupo) continua aceito e o app ignora o que não conhece. Nada muda de versão (`/api/v1`).

## Status por tarefa

| Parte | Contrato | Servidor |
| --- | --- | --- |
| Tipos desta página e vetores | BE-C8 (pronto) | — |
| Porções por grupo: `GET /referencias/porcoes` | BE-C8 | BE-47b (pronto) |
| Produtos e suplementos | BE-C8 | BE-48 |
| Dieta ligada às metas, alternativas, "à vontade", rodapé (salvar, totais de macros, saldo das metas) | BE-C8 | BE-49a (pronto) |
| Totais de micronutrientes, `incompleto`, adequação à DRI | BE-C8 | BE-49b (pronto) |
| Peso ajustado, metas a partir da avaliação, gestação e lactação na adequação (`GET /pacientes/{id}/dietas/referencia-avaliacao`) | BE-C8 | BE-49d (pronto) |
| Snapshot e sync com os campos novos | BE-C8 | BE-49c |
| Marcadores de restrição nos alimentos | BE-C8 | BE-51 (pronto) |
| PDF da dieta e das listas de substituição | — | BE-50 |
| Modelos de dieta: CRUD, duplicar, salvar dieta como modelo, aplicar com escala (sync `modelosDieta`) | BE-C8 | BE-52a (pronto) |
| Restrição do modelo na dieta publicada e no snapshot (RN-MOD-03) | BE-C8 | BE-52b (pronto) |
| Modelos iniciais do sistema (seed) | BE-C8 | BE-53 (pronto) |

## Porções por grupo (pronto, BE-47b)

### PDF da lista de substituição (BE-50b)

`GET /api/v1/dietas/{id}/pdf-lista` (nutri dona) e `GET /api/v1/eu/dietas/{id}/pdf-lista` (paciente destinatário, inclusive vínculo encerrado) devolvem `PdfResposta { url, expiraEm }`, com link assinado por 10 minutos e `Cache-Control: no-store`. Dieta nunca publicada ou de outro usuário: `404 RECURSO_NAO_ENCONTRADO`.

Parâmetro opcional `tamanho`: `PEQUENA`, `MEDIA` (padrão), `GRANDE`, `CARBOIDRATOS`; valor desconhecido: `400 FORMATO_INVALIDO`. As opções do grupo são ordenadas por kcal crescente; pequena usa a menor, grande a maior e média a posição `quantidade / 2` (inteira, central superior se houver número par). Uma opção repete nos três tamanhos. Carboidratos usa a opção média e acrescenta a coluna `Carb. (g)` e exemplo de soma por refeição.

A fonte de alimentos é somente `listaSubstituicao` do snapshot publicado (RN-DIE-03, já filtrada por restrições). Nome, gramas e medida caseira são os congelados. As APIs internas consultam pela nutri do snapshot o nome do grupo e os valores de energia e carboidratos por porção-base, além das opções do catálogo de porções. Fator = kcal da opção / kcal por porção-base. Gramas arredondam para inteiro; a quantidade inicial da medida caseira escala e arredonda para ¼. Aceita frações ¼, ½, ¾ e decimais; sem número inicial, prefixa `N × `. A coluna de carboidratos usa a média do grupo multiplicada pelo fator, nunca o valor individual do alimento, e informa a fonte provisória `PorcoesGrupos.FONTE`.

Sem opção do grupo ou referência válida de energia, omite os alimentos afetados e avisa ao final. Carboidrato ausente aparece como "Não informado", nunca zero (RN-CAL-04). As referências nutricionais são atuais; os dados da lista continuam congelados. Layouts: `PDF-Lista-Pequena`, `PDF-Lista-Media`, `PDF-Lista-Grande` e `PDF-Lista-Carboidratos` no mobile. Medidas também escalam, conforme o índice por tarefa, apesar dos exemplos estáticos dos artboards.

`GET /api/v1/referencias/porcoes` devolve `{ fonte, unidades, grupos: [{ grupoId, nome, opcoes: [{ opcao, kcal, quantidade, unidade, medidaCaseira, guiaAlimentar, nutrientes }] }] }`. `nutrientes` é um mapa código → valor **por porção** (34 códigos, unidades em `unidades`); nutriente que nenhum alimento do grupo informa não aparece (RN-CAL-04). Os valores são as médias da planilha da Nut. Luciana e são **provisórios**; o app e o PDF mostram `fonte`. "Não usar o grupo" e "à vontade" são escolhas do cliente.

## Mudanças em `SalvarDietaRequisicao` e na resposta da dieta

Campos novos, todos opcionais.

| Onde | Campo | Tipo | Observação |
| --- | --- | --- | --- |
| dieta | `porcoes` | `[{ grupoId, opcao }]` | porção escolhida por grupo, uma vez por dieta (RN-DIE-10). Grupo ausente = não usado |
| dieta | `metas` | `{ vetKcal, carboidratoPct, proteinaPct, lipidioPct }` | alvo da dieta; vem da avaliação (`energiaResultado`) ou digitado. A soma dos percentuais não precisa fechar 100: o backend devolve o saldo |
| dieta | `pesoReferenciaKg` | decimal | peso para os "por kg" (RN-CAL-05: peso ajustado quando IMC ≥ 30). O servidor calcula e grava quando há avaliação; o cliente só mostra |
| dieta | `rodape` | texto até 2000 | "Além das refeições acima, você também poderá ingerir…" (RN-DIE-12) |
| refeição | `tipo` | `PRINCIPAL` \| `ALTERNATIVA` | padrão `PRINCIPAL` |
| refeição | `alternativaDe` | `uuid` da refeição | só com `tipo=ALTERNATIVA` (ex.: lanche 2ª opção, jantar sopa) |
| item | `grupoId` | `uuid` | item por **porção do grupo** (usa a opção escolhida em `porcoes`); `porcoes` é a quantidade |
| item | `produtoId` + `gramas` | `uuid`, decimal | produto ou suplemento (BE-48), nutrientes por 100 g × `gramas` ÷ 100 |
| item | `aVontade` | boolean | item sem quantidade e **sem kcal** (ex.: vegetal cru) |
| item | `textoLivre` | texto até 200 | alimento fora do catálogo, só como texto; não entra nos totais |

Cada item traz exatamente um de `alimentoId`, `grupoId` ou `produtoId` (ou só `textoLivre` com `aVontade`). Violação: `422 ITEM_INVALIDO`. Grupo ou opção que não existem, ou item por grupo sem a porção escolhida: `422 PORCAO_INVALIDA`.

Na resposta, cada item, refeição e a dieta ganham `totais` ampliados (a forma antiga com `kcal`, `carboidratos`, `proteinas`, `gorduras` continua):

- `micronutrientes`: mapa código → valor (mesmos códigos e unidades de `/referencias/porcoes`);
- `incompleto`: lista dos códigos em que algum item não informa o nutriente (a soma só conta quem informa; nunca zero por falta de dado);
- `saldoMetas`: `{ kcal, carboidrato, proteina, lipidio }` quando há `metas` (meta − total do dia);
- `adequacao` (só no dia): `[{ nutriente, total, meta, percentual, classe }]` contra as metas DRI do paciente (`GET /referencias/dri`), `classe` = `ABAIXO` \| `ADEQUADO` \| `ACIMA`.

O total do dia soma só refeições `PRINCIPAL`; cada `ALTERNATIVA` mostra o próprio total e a diferença para a refeição que substitui (RN-DIE-11).

## Como o servidor devolve (BE-49a)

- `ItemRefeicaoResposta`: `alimentoId`, `nomeAlimento`, `medidaCaseira`, `gramas` e `porcoes` passam a ser **opcionais** (item por grupo, à vontade e texto livre não têm todos). Item por grupo traz `grupoId`, `opcao` (a escolhida na dieta), `nomeAlimento` = nome do grupo, `medidaCaseira` da porção e `gramas` = quantidade × porções quando a unidade é g. Item à vontade traz `aVontade=true`, sem `porcoes`, com totais zero. Texto livre traz `textoLivre` (também em `nomeAlimento`). O app ainda não foi lançado, então a mudança não quebra ninguém; os decodificadores do mobile tratam `null` (MOB-54).
- `RefeicaoResposta`: `tipo` (`PRINCIPAL` quando ausente em snapshot antigo), `alternativaDe` e, na alternativa, `diferencaParaPrincipal` (total dela menos o da principal, pode ser negativo).
- `totais` do dia: `saldoMetas { kcal, carboidrato, proteina, lipidio }` = meta menos total, com carboidrato e proteína a 4 kcal/g e lipídio a 9 kcal/g sobre o VET.
- Cálculo: item por grupo soma o perfil da opção sem arredondar; a refeição e o dia arredondam o total a 2 casas (RN-CAL-07). Item por alimento continua arredondando por item (RN-DIE-05).
- `refeicao.alternativaDe` precisa ser o `id`, informado na mesma requisição, de uma refeição `PRINCIPAL`; senão `400 FORMATO_INVALIDO` (RN-DIE-11). Item à vontade não aceita `porcoes`; item com origem e `textoLivre` juntos dá `ITEM_INVALIDO`.
- `pesoReferenciaKg` é só leitura (BE-49d): o servidor devolve o peso de referência da avaliação mais recente da nutri para o paciente (RN-CAL-05, peso ajustado com IMC 30 ou mais em adulto) e o congela no snapshot ao publicar; sem avaliação fica ausente. `avaliacao.energiaResultado` ganha o campo opcional `pesoReferenciaKg`, o peso que os "por kg" usaram. Vetores em `contrato/calculos/peso-referencia.json`.
- `GET /pacientes/{id}/dietas/referencia-avaliacao` → `{ avaliacaoId, dataHora, pesoKg, pesoReferenciaKg, metas?, gestante, lactante }`. `metas` (`vetKcal`, `carboidratoPct`, `proteinaPct`, `lipidioPct`) vem do VET da avaliação e do meio da faixa de gramas de carboidrato e proteína; o lipídio fecha 100% (planilha: 100 − %gli − %ptn). É sugestão para o planejador: a nutri confirma e o servidor só grava o que ela enviar. `404 RECURSO_NAO_ENCONTRADO` sem avaliação dela (ou ficha de outra nutri).
- `micronutrientes` (BE-49b): códigos de `GET /referencias/porcoes` menos `energia`, `gli`, `pro` e `lip` (que têm campo próprio), com 2 casas; ausente quando nenhum item informa micronutriente (dieta só de alimentos do catálogo, que trazem só os 4 macros). `incompleto` lista os códigos de `micronutrientes` em que algum item que conta (não à vontade, não texto livre) não informa o nutriente; um item por alimento do catálogo faz todos os códigos ficarem incompletos.
- `adequacao` (só no dia): usa sexo e idade em meses da ficha e, para mulher, gestante ou lactante da avaliação mais recente (`energia.gestante` / `energia.lactacao`; lactante tem prioridade, BE-49d); sem sexo ou nascimento a lista não vem. Proteína usa `proteinas` do total; os demais códigos mapeiam assim: `fib`→`fibras`, `na`→`sodio`, `k`→`potassio`, `ca`→`calcio`, `cu`→`cobre`, `i`→`iodo`, `mg`→`magnesio`, `mn`→`manganes`, `p`→`fosforo`, `se`→`selenio`, `zn`→`zinco`, `fe`→`ferro`, `fol`→`vitamina_b9`, `nia`→`vitamina_b3`, `pan`→`vitamina_b5`, `b2`→`vitamina_b2`, `b1`→`vitamina_b1`, `ret`→`vitamina_a`, `b6`→`vitamina_b6`, `b12`→`vitamina_b12`, `c`→`vitamina_c`, `d`→`vitamina_d`, `e`→`vitamina_e`, `agua` (g)→`agua` (l). `percentual` = total ÷ meta × 100; `classe` ABAIXO abaixo de 90, ACIMA acima de 110 (limites provisórios).

## Marcadores de restrição (BE-51)

- `alimento.marcadores`: lista (sem repetição) de `CONTEM_LACTOSE`, `CONTEM_GLUTEN`, `ALTO_SODIO`, `ACUCAR_ADICIONADO`, `ALTO_POTASSIO`. Vem em `GET /alimentos`, nas respostas de criar e editar e no `/sync` da coleção `ALIMENTOS`. Cliente que não conhece o campo ignora (campo novo e opcional); valor de marcador desconhecido numa resposta futura deve ser ignorado pelo app.
- `POST /alimentos` e `PATCH /alimentos/{id}` aceitam `marcadores`: criar sem o campo grava lista vazia; editar sem o campo **mantém** os atuais; lista vazia limpa. Valor fora do enum dá `400 FORMATO_INVALIDO`.
- `GET /alimentos?semMarcador=CONTEM_LACTOSE&semMarcador=ALTO_SODIO`: só alimentos que não têm nenhum dos marcadores.
- A cópia de cada nutri nasce do catálogo com a sugestão inicial; ela ajusta só na própria cópia (outra nutri não vê). A sugestão inicial foi gerada por palavras do nome do alimento (`dados/alimento-marcadores.csv`, 205 pares) e é **[SUPOSIÇÃO G-15]**: uma nutri precisa validar. O marcador não altera a lista do paciente sozinho; o filtro entra com os modelos de dieta (BE-52, RN-MOD-03).

## Códigos de erro novos

| Código | HTTP | Quando |
| --- | --- | --- |
| `ITEM_INVALIDO` | 422 | item sem ou com mais de uma origem (RN-DIE-10) |
| `PORCAO_INVALIDA` | 422 | grupo ou opção inexistente, ou item por grupo sem porção escolhida |

Já estão em [integracao.md](integracao.md) (BE-49a).

## Modelos de dieta (BE-51 a BE-53)

Um modelo é uma dieta (refeições, porções por grupo, alternativas, "à vontade", rodapé, orientação) mais `restricoes`. Os modelos do sistema são somente leitura; a nutri trabalha com cópias dela, isoladas por `nutricionista_id` (RN-MOD-01). Aplicar a um paciente cria um rascunho (RN-MOD-02) com as porções escaladas para o VET dele (arredondando para 0,5). Endpoints (BE-52a):

- `GET /eu/nutri/modelos-dieta` → `{ itens: [ModeloDietaResposta] }` (os do sistema primeiro, depois os dela); `GET /eu/nutri/modelos-dieta/{id}` (sistema ou dela; de outra nutri = 404).
- `POST /eu/nutri/modelos-dieta` cria (corpo `SalvarModeloDietaRequisicao`: `titulo`, `descricao`, `condicao`, `orientacao`, `refeicoes`, `porcoes`, `metas`, `rodape`, `restricoes` e `kcalReferencia`, todos validados como numa dieta). `POST /eu/nutri/modelos-dieta/{id}/duplicar` copia um modelo do sistema ou dela. `POST /eu/nutri/modelos-dieta/de-dieta/{dietaId}` salva uma dieta dela como modelo (`titulo`, `descricao`, `condicao`, `restricoes`; a energia total da dieta vira `kcalReferencia`).
- `PUT /eu/nutri/modelos-dieta/{id}` e `DELETE` com `If-Match`, só nos dela: do sistema = `403 ACESSO_NEGADO` (RN-MOD-01); de outra nutri = `404`; versão velha = `409 CONFLITO_VERSAO` com o atual. Todas devolvem `ETag`.
- `POST /pacientes/{id}/dietas/de-modelo/{modeloId}` (corpo opcional `{ id?, vetKcal? }`) cria o **rascunho** (RN-MOD-02) e devolve `{ dieta, fatorEscala, avisos }`. Com `vetKcal`, as porções (de grupo e de alimento, menos "à vontade") são multiplicadas por `vetKcal / kcalReferencia` (sem ela, `metas.vetKcal`), arredondadas para 0,5 e nunca abaixo de 0,5, e `metas.vetKcal` da dieta vira o alvo. Fator fora de 0,25 a 4 = `400 FORMATO_INVALIDO`. `avisos` traz `{ codigo, mensagem }`: `ITEM_REMOVIDO_RESTRICAO` (alimento com marcador que o modelo evita, RN-MOD-03), `ITEM_INDISPONIVEL` (alimento que não está mais ativo na lista dela) e `SEM_ESCALA` (modelo sem energia de referência).
- Sync: os modelos **dela** entram em `GET /sync` na coleção `modelosDieta` (exclusão = tombstone); os do sistema não, vêm só do `GET` (o app guarda quando online). Até 200 modelos por nutri.
- `restricoes` guarda marcadores de alimento (`CONTEM_LACTOSE`, `CONTEM_GLUTEN`, `ALTO_SODIO`, `ACUCAR_ADICIONADO`, `ALTO_POTASSIO`). Ao aplicar, tiram os alimentos marcados do rascunho (com aviso) e viram `restricoes` da dieta (RN-MOD-03, BE-52b): `DietaResposta` e `DietaSnapshotResposta` ganham `restricoes` (ausente = nenhuma), `SalvarDietaRequisicao` aceita `restricoes` (ausente mantém as atuais, lista vazia limpa) e, na publicação, a `listaSubstituicao` do snapshot sai sem os alimentos que carregam algum desses marcadores. Changeset 044.

## Regras de negócio

RN-DIE-10 a RN-DIE-13 e RN-CAL-07 (ver [regras-de-negocio.md](regras-de-negocio.md)). Fonte dos valores por porção: ver `visao-geral.md` (suposição sobre as médias da planilha) e `src/main/resources/db/changelog/dados/README.md`.
