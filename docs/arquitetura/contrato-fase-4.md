# Contrato da fase 4 — avaliações, evolução e anamnese (BE-C4)

Cópia oficial no backend. Mobile e frontend acompanham este contrato. Como na fase 3, cada tarefa publica DTO, OpenAPI e implementação juntos; o contrato é validado pelos testes de API da própria tarefa. Caminhos sob `/api/v1`.

## Avaliações (BE-19)

| Método/caminho | Perfil | Requisição | Sucesso |
| --- | --- | --- | --- |
| GET /pacientes/{id}/avaliacoes | nutri | — | 200 `{itens: [AvaliacaoResposta]}` (mais recente primeiro) |
| POST /pacientes/{id}/avaliacoes | nutri | `SalvarAvaliacaoRequisicao` (`Idempotency-Key` opcional) | 201 `AvaliacaoResposta` + `ETag` |
| GET /avaliacoes/{id} | nutri | — | 200 + `ETag` |
| PUT /avaliacoes/{id} | nutri | `SalvarAvaliacaoRequisicao` + `If-Match: "<versao>"` | 200 + `ETag` |
| GET /eu/avaliacoes | paciente | — | 200 `{itens: [...]}` só as `compartilhada=true` |

`SalvarAvaliacaoRequisicao`: `{id?, dataHora, pesoKg, estaturaM, perimetros?, dobras?, composicao?, parecer?, compartilhada?, justificativa?}`.

- `perimetros` (cm): `pescoco, torax, cintura, abdomen, quadril, braco, antebraco, coxa, panturrilha`.
- `dobras` (mm): `triceps, subescapular, peitoral, axilarMedia, suprailiaca, abdominal, coxa`.
- `composicao` (digitada pela nutri): `percentualGordura, pesoGordoKg, pesoMagroKg`.
- Resposta acrescenta `imc` (2 casas) e `classificacaoImc` (`ABAIXO_DO_PESO`, `NORMAL`, `SOBREPESO`, `OBESIDADE_I`, `OBESIDADE_II`, `OBESIDADE_III`), calculados pelo backend (RN-AVA-02). Valores enviados para esses campos são ignorados.

### Regras

- RN-AVA-03: peso 20–400 kg, estatura 0,5–2,5 m, dobras 1–80 mm, perímetros 10–250 cm; fora disso 422 `VALOR_FORA_DO_INTERVALO` com o campo; mais de 2 casas decimais é 400 `FORMATO_INVALIDO`. `dataHora` no futuro (tolerância de 5 min): 400.
- RN-AVA-04: edição depois de 30 dias da criação exige `justificativa`; sem ela, 422 `JUSTIFICATIVA_OBRIGATORIA`. A justificativa fica registrada (`avaliacao_justificativa`).
- RN-AVA-05: o paciente só enxerga avaliações compartilhadas. `compartilhada` assume `true` quando omitido.
- Vínculo ENCERRADO só lê (RN-VIN-05): escrita dá 403 `ACESSO_NEGADO`. `If-Match` desatualizado: 409 `CONFLITO_VERSAO` com `atual` no corpo (RN-SYN-02). Avaliação de outra nutri: 404.

### Sync

Nova coleção `avaliacoes` em `GET /sync` (opcional no JSON; ignore ausente). A nutri recebe todas as do vínculo; o paciente recebe as compartilhadas e, ao descompartilhar ou excluir, uma lápide (`excluido=true`).

## Evolução (BE-20)

| Método/caminho | Perfil | Sucesso |
| --- | --- | --- |
| GET /pacientes/{id}/evolucao | nutri | 200 `EvolucaoResposta` |
| GET /eu/evolucao | paciente | 200 `EvolucaoResposta` |

`EvolucaoResposta`: `{pacienteId, series: [{medida, unidade, pontos: [{dataHora, valor}]}]}`. Cada série traz os pontos em ordem cronológica crescente (até 500, os mais recentes); séries sem nenhum ponto não aparecem. `medida`: `PESO`, `IMC`, `PERCENTUAL_GORDURA`, `PESO_GORDO`, `PESO_MAGRO`, `PERIMETRO_PESCOCO`, `PERIMETRO_TORAX`, `PERIMETRO_CINTURA`, `PERIMETRO_ABDOMEN`, `PERIMETRO_QUADRIL`, `PERIMETRO_BRACO`, `PERIMETRO_ANTEBRACO`, `PERIMETRO_COXA`, `PERIMETRO_PANTURRILHA`, `PESO_EM_CASA`. Ignore `medida` desconhecida (o backend pode acrescentar séries).

- Avaliações: a nutri vê todas as do vínculo; o paciente só as compartilhadas (RN-AVA-05). Séries opcionais (perímetros, composição) só têm pontos das avaliações que as preencheram.
- `PESO_EM_CASA` vem dos registros `POST /eu/pesos`. A nutri só o recebe com vínculo ATIVO (igual ao sync); com vínculo encerrado vêm só as avaliações.
- Nutri sem o paciente: 404. Perfil trocado: 403 `PERFIL_INVALIDO`.

## Anamnese — modelos (BE-21)

| Método/caminho | Perfil | Requisição | Sucesso |
| --- | --- | --- | --- |
| GET /modelos-questionario | nutri | — | 200 `{itens: [ModeloQuestionarioResposta]}` |
| POST /modelos-questionario | nutri | `SalvarModeloRequisicao` | 201 + `ETag` |
| GET /modelos-questionario/{id} | nutri | — | 200 + `ETag` |
| PUT /modelos-questionario/{id} | nutri | `SalvarModeloRequisicao` + `If-Match` | 200 + `ETag` |

`SalvarModeloRequisicao`: `{id?, nome, blocos: [{titulo, perguntas: [{id?, texto, tipo, opcoes?, obrigatoria?, condicao?}]}]}`. `ModeloQuestionarioResposta` acrescenta `padrao`, `versao`, `criadoEm`, `atualizadoEm`.

- RN-ANA-01: toda nutri tem uma cópia do modelo padrão (12 blocos), criada no primeiro `GET /modelos-questionario` (ou no primeiro POST). Ela é editável como qualquer outro modelo (`padrao=true` só marca a origem). Não há exclusão de modelo no MVP.
- RN-ANA-02: `tipo` ∈ `TEXTO`, `NUMERO`, `ESCOLHA_UNICA`, `MULTIPLA_ESCOLHA`, `ESCALA_1_5`, `HORARIO`, `LISTA_POR_REFEICAO`. `opcoes` (2 a 30, sem repetição) só nos dois tipos de escolha. `condicao` = `{perguntaId, operador: IGUAL|DIFERENTE|CONTEM, valor}` e aponta para uma pergunta **anterior** do mesmo modelo; `CONTEM` só vale para múltipla escolha; `valor` deve ser uma das opções da pergunta de referência.
- `id` de pergunta é UUIDv7; ausente = o backend gera. Reenviar o `id` mantém as respostas já dadas ligadas à pergunta.
- Limites: até 30 blocos, 60 perguntas por bloco e 200 no modelo; texto até 500 caracteres. Violação: 400 `FORMATO_INVALIDO` com `campos[].campo` (ex.: `blocos[0].perguntas[1].opcoes`).
- `PUT` exige `If-Match`; desatualizado: 409 `CONFLITO_VERSAO` com `atual`. Escrita de nutri exige assinatura em dia (RN-ASS-04).

## Anamnese — envio e respostas (BE-22)

| Método/caminho | Perfil | Requisição | Sucesso |
| --- | --- | --- | --- |
| POST /pacientes/{id}/questionarios | nutri | `{id?, modeloId}` | 201 `QuestionarioResposta` |
| GET /pacientes/{id}/questionarios | nutri | — | 200 `{itens: [...]}` com as respostas |
| GET /eu/questionarios | paciente | — | 200 `{itens: [...]}` |
| PUT /eu/questionarios/{id}/respostas | paciente | `{respostas: {perguntaId: valor\|null}}` | 200 `QuestionarioResposta` |
| POST /eu/questionarios/{id}/enviar | paciente | — | 200 (status `RESPONDIDO`) |

`QuestionarioResposta`: `{id, pacienteId, vinculoId, modeloId?, nome, blocos, respostas: {perguntaId: valor}, status: PENDENTE|RESPONDIDO, enviadoEm, respondidoEm?, versao, criadoEm, atualizadoEm}`.

- RN-ANA-04: `blocos` é a cópia das perguntas feita no envio. Editar o modelo depois não muda o que o paciente recebeu.
- Envio exige vínculo `ATIVO` (senão 403 `ACESSO_NEGADO`, RN-VIN-05) e assinatura da nutri em dia. Repetir o mesmo `id`/`Idempotency-Key` devolve o mesmo questionário. Evento interno `QuestionarioEnviado` (push na BE-24).
- Formato do valor da resposta por tipo: `TEXTO` string (até 2000); `NUMERO` número; `ESCOLHA_UNICA` string de `opcoes`; `MULTIPLA_ESCOLHA` lista de strings de `opcoes`, sem repetir; `ESCALA_1_5` inteiro 1 a 5; `HORARIO` `"HH:mm"`; `LISTA_POR_REFEICAO` lista (até 12) de `{refeicao, descricao, horario?}`. Valor que não cabe no tipo, ou pergunta que não existe no questionário: 400 `FORMATO_INVALIDO` com a pergunta em `campos[].campo`.
- `PUT .../respostas` **mescla** por pergunta (valor `null` apaga; as não citadas ficam). Pode ser chamado várias vezes (salvar parcial). Sem `If-Match`: a mesclagem por pergunta não gera conflito.
- `POST .../enviar` confere as perguntas `obrigatoria` que estão visíveis (a condição é avaliada com as respostas dadas; sem resposta na pergunta de referência, `IGUAL` e `CONTEM` escondem e `DIFERENTE` mostra). Faltando: 400 `OBRIGATORIO` com as perguntas em `campos`. Sucesso: `RESPONDIDO`, respostas imutáveis (também por trigger no banco) e evento `QuestionarioRespondido`.
- RN-ANA-03: depois de `RESPONDIDO`, qualquer escrita dá 409 `QUESTIONARIO_FECHADO`. Vínculo encerrado: o paciente só lê (403 `ACESSO_NEGADO` ao escrever).
- Outro paciente ou outra nutri: 404. Perfil trocado: 403 `PERFIL_INVALIDO`.

### Sync

Coleções novas em `GET /sync`: `modelos` (só nutri) e `questionarios` (nutri dona e paciente, já com `respostas`). `POST /sync` aceita do paciente `entidade: "questionarios"`, `operacao: "ATUALIZAR"`, `id` do questionário, `dados: {respostas: {...}}` (mesma mesclagem do REST; `versaoBase` é ignorada). Resultado `APLICADA` com a nova `versao`, ou `REJEITADA` (`QUESTIONARIO_FECHADO`, `FORMATO_INVALIDO`, `ACESSO_NEGADO`). Concluir o questionário (`/enviar`) exige conexão.
