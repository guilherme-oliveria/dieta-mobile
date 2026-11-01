# Contrato da fase 7 — avaliação ampliada, resultados e protocolos (BE-C7)

Cópia oficial no backend. Parte do plano das planilhas ([plano-melhorias-planilhas.md](plano-melhorias-planilhas.md), ondas 17 a 23). Erros seguem o `ProblemDetail` de [integracao.md](integracao.md). Exemplos JSON em `src/test/resources/contrato/fase-7/` (tipados e conferidos por `ContratoFase7Test`); vetores de cálculo em `src/test/resources/contrato/calculos/`.

Tudo aqui é **aditivo**: campos novos são opcionais, o contrato antigo (fase 4) continua aceito e o app ignora o que não conhece. Nada muda de versão (`/api/v1`).

## Status por tarefa

| Parte | Contrato | Servidor |
| --- | --- | --- |
| Tipos e JSONs desta página | BE-C7 (pronto) | — |
| Referências (tabelas `referencia_faixa`) | — | BE-39 |
| Funções de cálculo (domínio) | vetores em `calculos/adulto.json` | BE-40 |
| `AvaliacaoResposta` com `resultados`, `protocolo`, `complementares`, `compartilhamento`; gravação e `/sync` | BE-C7 | BE-41 (pronto) |
| `GET/PUT /eu/nutri/protocolos` | BE-C7 | BE-42 (pronto) |
| `energia` e `energiaResultado` na avaliação; `GET /referencias/atividades-met` e `/referencias/dri` | BE-C7 | BE-43b (pronto) |

Desde o BE-41 o servidor grava a entrada e calcula `resultados` ao salvar (REST e `POST /sync` usam o mesmo caso de uso). Detalhes:

- A idade vem de `dataNascimento` da ficha na data da avaliação (fuso de São Paulo) e o sexo de `sexo` da ficha. Sem nascimento: `resultados.motivosNulos.resultados = "FALTA_NASCIMENTO"`; sem sexo (ou `OUTRO`/`NAO_INFORMADO`): `"FALTA_SEXO"`; abaixo de 18 anos: `"FAIXA_ETARIA_NAO_SUPORTADA"` até o BE-55. Nesses casos só `idadeAnos` (quando sabida) e o IMC antigo vêm preenchidos; a avaliação é salva normalmente.
- Protocolo ausente na criação = padrão da nutri (`GET /eu/nutri/protocolos`; `SETE`/`MAGRO` se nunca salvou). Na edição, ausente = o protocolo que a avaliação já tinha (RN-CAL-03: o resultado não muda sozinho). `versaoReferencia` é sempre a vigente no servidor.
- O paciente recebe só as seções compartilhadas (RN-RES-01): `medidas` (perímetros, dobras, compleição, cintura, somatório), `composicao` (composição e % de gordura), `resumo` (parecer e `textoResumo`), `necessidades` (`complementares`). Peso, estatura, IMC, classificação do IMC, peso teórico e `faixaEtaria` acompanham a avaliação compartilhada. `motivosNulos` não vai ao paciente.
- A evolução ganha as séries `SOMATORIO_DOBRAS` e `DOBRA_*` (mm).

## `AvaliacaoResposta` ampliada

Campos novos no fim do registro, todos opcionais (`NON_NULL`):

| Campo | Tipo | Observação |
| --- | --- | --- |
| `faixaEtaria` | `CRIANCA` \| `ADOLESCENTE` \| `ADULTO` \| `IDOSO` | RN-CAL-02, pela idade na data da avaliação |
| `protocolo` | `{ dobras: TRES\|SETE, massa: MAGRO\|MUSCULAR, versaoReferencia }` | RN-CAL-03; o que foi usado no cálculo |
| `complementares` | `pesoUsualKg`, `pesoDesejadoKg`, `punhoCm`, `diametroPunhoCm`, `diametroJoelhoCm`, `percentualGorduraDesejado`, `nivelAtividade`, `objetivo`, `objetivoKgPorMes`, `menstruada` | entrada da nutri; todos opcionais |
| `resultados` | ver abaixo | calculado e gravado pelo servidor (RN-CAL-01) |
| `compartilhamento` | `{ medidas, composicao, necessidades, resumo, exames }` | RN-RES-01; `null` na chave = padrão (tudo `true`, `exames` `false`); a chave antiga `compartilhada=false` esconde tudo |

`classificacaoImc` (campo antigo, 6 valores) continua igual. A classificação detalhada da planilha vem em `resultados.classificacaoImc` (`MG_III`, `MG_II`, `MG_I`, `MG`, `NO`, `SP`, `OB_I`, `OB_II`, `OB_III`; idoso usa `MG`, `NO`, `SP`). O cliente deve tratar valor de enum desconhecido.

### `resultados`

`idadeAnos`, `classificacaoImc`, `riscoComorbidades` (`B`, `ME`, `A`, `MO`, `S`, `MS`), `pesoTeoricoMinKg`, `pesoTeoricoMaxKg`, `compleicao` (`PEQUENA`, `MEDIA`, `GRANDE`), `classificacaoCintura` (`B`, `A`, `MA`), `somatorioDobrasMm`, `densidadeCorporal` (4 casas), `percentualGordura`, `classificacaoPercentualGordura` (`MT_BXO`, `EXC`, `MT_BOM`, `BOM`, `ADEQ`, `MOD_ALTO`, `ALTO`, `MT_ALTO`), `percentualMagro`, `pesoGordoKg`, `pesoOsseoKg`, `pesoResidualKg`, `pesoMagroKg` (ou muscular, conforme o protocolo), `pesoParaPercentualDesejadoKg`, `textoResumo` e `motivosNulos`.

- Kg e % com 2 casas, HALF_UP. Resultado sem dado fica **ausente**, nunca zero, e o motivo vai em `motivosNulos` (RN-CAL-04): `FALTA_PUNHO`, `FALTA_CINTURA`, `FALTA_DOBRAS`, `FALTA_DIAMETRO_PUNHO`, `FALTA_DIAMETRO_JOELHO`, `FALTA_PERCENTUAL_DESEJADO`.
- Onda 19 acrescenta `necessidades` (GEB, GET, VET, metas); onda 22 as classificações infantis. Sempre campos novos opcionais.
- O `textoResumo` é para a nutri revisar (pode usar siglas já expandidas em frases); o que o paciente vê passa por RN-RES-02 no app.

## `SalvarAvaliacaoRequisicao` ampliada

Mesmos campos novos de entrada: `protocolo` (a nutri escolhe; `versaoReferencia` é ignorada e o servidor grava a vigente), `complementares` e `compartilhamento`. `resultados` nunca é aceito na entrada: o servidor sempre recalcula ao salvar e no `/sync` (RN-CAL-01). Protocolo ausente = padrão da nutri (`GET /eu/nutri/protocolos`), ou `SETE`/`MAGRO` se ela nunca definiu.

Limites de sanidade (RN-AVA-03) dos campos novos: pesos 20–400 kg, punho 5–40 cm, diâmetros 1–30 cm, % desejado 3–60, kg/mês 0–10. Fora: `422 VALOR_FORA_DO_INTERVALO`.

## "Meus protocolos" (BE-42)

| Endpoint | Perfil | Corpo | Resposta |
| --- | --- | --- | --- |
| `GET /api/v1/eu/nutri/protocolos` | nutri | — | `200 ProtocolosNutriResposta` + `ETag` (padrão `SETE`/`MAGRO` se nunca salvo) |
| `PUT /api/v1/eu/nutri/protocolos` | nutri | `SalvarProtocolosNutriRequisicao` + `If-Match` | `200 ProtocolosNutriResposta` |

Valor desconhecido: `422 PROTOCOLO_INVALIDO` (RN-CAL-03). Paciente: `403 PERFIL_INVALIDO`. Vale para as próximas avaliações; as já gravadas guardam o que usaram. Entra no `/sync` na coleção `protocolos` (`mudancas.protocolos`), só para a nutri dona. A escrita é pelo `PUT` (online, com `If-Match`; versão `"0"` na primeira gravação). Valor desconhecido de protocolo também vale como `422 PROTOCOLO_INVALIDO` ao criar ou editar avaliação.

## Energia e metas (BE-43b)

Entrada opcional `energia` em `SalvarAvaliacaoRequisicao`: `{ metodo, gestante, lactacao, carboidrato, proteina, lipidio, dislipidemico, colesterolTotal, rotinaMet }`.

- `metodo`: `FAO_1985`, `FAO_2004` (padrão), `IOM_2005`, `RDI_2005` ou `MET`. `lactacao`: `ATE_6_MESES` ou `DE_7_A_12_MESES` (preenchido = lactante, só vale para mulher). Nível de atividade, `objetivo` e `objetivoKgPorMes` vêm de `complementares`.
- `carboidrato`: `FAIXA_DRI` (padrão), `MINIMO_RDA`, `FAIXA_ESPORTIVA`, `POR_KG`. `proteina`: `FAIXA_DRI` (padrão), `RDA`, `MINIMO_15`, `ENDURANCE`, `HIPERTROFIA`. `lipidio`: `MINIMO_20` (padrão) ou `MINIMO_25`.
- `rotinaMet` (só lida com `metodo = MET`): `{ horasSono, horasTrabalho, intensidadeTrabalho: LEVE|MODERADO|INTENSO, atividades: [{ codigo, minutos }] }`, até 5 atividades; o `codigo` vem de `GET /referencias/atividades-met`. Código desconhecido ou dia com mais de 24 h: `400 FORMATO_INVALIDO`. O MET usa a massa magra calculada da avaliação.
- O servidor só calcula quando vem `energia` ou `complementares.nivelAtividade`; sem isso a avaliação não tem o bloco (contrato antigo). Na resposta: `energia` (a escolha, ecoada) e `energiaResultado`: `{ metodo, gebKcal, getKcal, vetKcal, kcalPorKg, carboidrato, proteina, lipidio, lipidiosDislipidemico, colesterolMaximoMg, motivosNulos }`. Cada macro é `{ percentualMinimo, percentualMaximo, gramasMinimo, gramasMaximo }` (máximos ausentes quando só há mínimo). Sem dado o campo some e o motivo vai em `motivosNulos` (RN-CAL-04): `get = FALTA_NIVEL_ATIVIDADE | FALTA_ROTINA_MET | FALTA_MASSA_MAGRA`, `vet = FALTA_GET`, `energia = FAIXA_ETARIA_NAO_SUPORTADA | FALTA_SEXO | FALTA_NASCIMENTO`. `gebKcal` não existe em `RDI_2005`.
- `lipidiosDislipidemico` e `colesterolMaximoMg` (300 mg, ou 200 se `colesterolTotal` ≥ 200) só vêm com `dislipidemico = true`.
- `exames` (BE-56b, RN-EXA-01, opcional): entrada `[{ codigo, valor | texto, condicao? }]` (máx. 400; ausente mantém, `[]` limpa); resposta `[{ codigo, secao, nome, unidade, valor?, texto?, condicao?, rotulo?, motivo? }]` com `rotulo` da planilha ou `motivo` (`FALTA_SEXO`, `FALTA_CONDICAO`, `FALTA_IDADE`, `FORA_DAS_FAIXAS`) e, em exame numérico, `faixas` (BE-56c): `[{ rotulo, min?, minIncl, max?, maxIncl }]` já resolvidas para o sexo, a idade e a fase do paciente (limite ausente = aberto), para desenhar a régua. Paciente só recebe se `compartilhamento.exames` for `true`. Entra no `/sync` junto com a avaliação.
- Paciente: `energia` e `energiaResultado` seguem a seção `necessidades` do compartilhamento (RN-RES-01).
- `GET /referencias/dri` devolve `{ sexo, idadeMeses, gestante, lactante, itens: [{ nutriente, nome, unidade, valor }] }`; lactante tem prioridade sobre gestante; nutriente sem referência para a idade não aparece. Vetores: `calculos/energia.json`, `met.json` e `dri.json`.
- [SUPOSIÇÃO] Os padrões (FAO 2004, faixas DRI, lipídio mínimo 20%) são os mais usados na planilha; mudar um padrão é só trocar o valor em `CalculoEnergiaAvaliacao`.

## Vetores de teste (RN-CAL-06)

`contrato/calculos/adulto.json` é o mesmo arquivo nos dois repositórios (no mobile: `shared/src/commonTest/resources/contrato/calculos/`). Cada caso tem `id`, `descricao`, `origem` (célula ou macro da planilha), `funcao`, `entrada`, `esperado` e, quando a versão corrigida da seção 3.6 difere da planilha, `diferenteDaPlanilha: true` com `nota`. Funções: `avaliacaoAdulto`, `classificarImcAdulto`, `classificarImcIdoso`, `classificarCintura`, `classificarCompleicao`, `classificarPercentualGordura`, `calcularIdade`. Tolerâncias no próprio arquivo (kg 0,01; % 0,1; IMC 0,01; densidade 0,0001). Os valores foram gerados de forma independente por um script de referência a partir das fórmulas e das macros VBA, e o caso `V01` confere com o PDF do Guilherme (IMC 27,52; densidade 1,0389; 26,48%; 91,9 kg para 18%).

Observação sobre a planilha: a idade usa `INT((data − nascimento)/365,25)` e, no dia exato de um aniversário, pode dar 1 ano a menos (caso `idade-2026-10-31-1997-10-31`). O app mantém a fórmula da planilha para bater os números; se a Luciana preferir contar aniversário exato, é ajuste de uma linha no BE-40 e nos vetores.
