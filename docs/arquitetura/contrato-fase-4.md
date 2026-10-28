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
