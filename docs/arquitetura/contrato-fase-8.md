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
| PDF da dieta e das listas de substituição | — | BE-50 |
| Modelos de dieta por condição | BE-C8 | BE-51 a BE-53 |

## Porções por grupo (pronto, BE-47b)

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

## Códigos de erro novos

| Código | HTTP | Quando |
| --- | --- | --- |
| `ITEM_INVALIDO` | 422 | item sem ou com mais de uma origem (RN-DIE-10) |
| `PORCAO_INVALIDA` | 422 | grupo ou opção inexistente, ou item por grupo sem porção escolhida |

Já estão em [integracao.md](integracao.md) (BE-49a).

## Modelos de dieta (BE-51 a BE-53)

Um modelo é uma dieta (refeições, porções por grupo, alternativas, "à vontade", rodapé, orientação) mais `restricoes`. Os modelos do sistema são somente leitura; a nutri trabalha com cópias dela, isoladas por `nutricionista_id` (RN-MOD-01). Aplicar a um paciente cria um rascunho (RN-MOD-02) com as porções escaladas para o VET dele (arredondando para 0,5). Endpoints previstos, detalhados no BE-51:

- `GET /eu/nutri/modelos-dieta` (sistema e dela), `POST` (criar, copiar ou salvar uma dieta como modelo), `PUT/DELETE /eu/nutri/modelos-dieta/{id}` (só as dela), `POST /pacientes/{id}/dietas/de-modelo/{modeloId}` (cria o rascunho).

## Regras de negócio

RN-DIE-10 a RN-DIE-13 e RN-CAL-07 (ver [regras-de-negocio.md](regras-de-negocio.md)). Fonte dos valores por porção: ver `visao-geral.md` (suposição sobre as médias da planilha) e `src/main/resources/db/changelog/dados/README.md`.
