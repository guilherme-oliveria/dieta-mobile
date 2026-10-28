# Contrato da fase 3 — rotina do paciente e envio do sync (BE-C3 + BE-15)

Cópia oficial no backend. Mobile e frontend acompanham este contrato. A partir da BE-15 os contratos "BE-Cn" deixam de ter stubs 501 separados: cada tarefa publica o DTO, o OpenAPI e a implementação juntos, e o contrato é validado pelos testes de API da própria tarefa.

Todos os caminhos começam em `/api/v1`. Bearer obrigatório, perfil PACIENTE.

## Endpoints (BE-15)

| Método/caminho | Requisição | Sucesso |
| --- | --- | --- |
| POST /eu/checkins | `{id?, dietaId, refeicaoId, data, status, trocas?, registradoEm}` | 201 `CheckinResposta` |
| POST /eu/agua | `{id?, ml, registradoEm}` (1 a 5000 ml) | 201 `AguaResposta` |
| POST /eu/pesos | `{id?, pesoKg, registradoEm}` (20 a 400 kg, até 2 casas) | 201 `PesoResposta` |
| POST /sync | `{mutacoes: [...]}` (1 a 200) | 200 `{resultados: [...]}` |

`status` do check-in: `FEITO`, `TROCADO`, `PULADO`. `trocas` (`[{itemId, alimentoId}]`) é obrigatório em `TROCADO` e proibido nos outros.

## Regras aplicadas

- `id` é UUIDv7 gerado no app; se ausente, o backend gera. Repetir o mesmo `id` do mesmo paciente devolve o registro já gravado (idempotente); `id` de outro paciente dá 409.
- Vínculo precisa estar `ATIVO` (RN-VIN-05: ENCERRADO só lê, escrita dá 403 `ACESSO_NEGADO`).
- Check-in: `data` não pode ser futura nem anterior a 3 dias (fuso do usuário) — RN-ROT. A refeição precisa existir no snapshot da dieta publicada; cada troca precisa ser de item da refeição, com alimento da lista congelada no snapshot e do mesmo grupo (RN-SUB-01).
- Peso fora de 20–400 kg: 422 `VALOR_FORA_DO_INTERVALO` (RN-AVA-03).
- RN-ASS-05: assinatura vencida da nutri não bloqueia a escrita do paciente.

## POST /sync

Entidades aceitas nesta fase, só com operação `CRIAR`: `checkins`, `agua`, `pesos`. O campo `dados` tem o mesmo formato do corpo do endpoint REST correspondente (o `id` da mutação vale como `id` do registro). Cada mutação roda em transação própria; o resultado vem na mesma ordem:

- `APLICADA` com `versao` da entidade (1 para entidades só de inclusão);
- `CONFLITO` com `atual` (quando houver registro de outro dono/estado);
- `REJEITADA` com `codigo` e `regra` (ex.: `FORMATO_INVALIDO` para entidade/operação desconhecida ou dados inválidos, `ACESSO_NEGADO`, `VALOR_FORA_DO_INTERVALO`).

Reenviar o mesmo `idMutacao` devolve o resultado guardado. `idMutacao` já usado por outro usuário: `REJEITADA` / `CHAVE_IDEMPOTENCIA_REUTILIZADA`.

## GET /sync (novas coleções)

`checkins` (paciente e nutri do vínculo), `agua` (só o paciente) e `pesos` (paciente e nutris com vínculo ativo). Opcionais no JSON: ignore ausentes (`ignoreUnknownKeys`).

## Adesão (BE-16)

`GET /pacientes/{id}/adesao` (perfil NUTRICIONISTA, vínculo próprio; outra nutri = 404) devolve `{pacienteId, dias7, dias30}`, cada janela com `{dias, esperadas, feitas, trocadas, puladas, percentual}`.

- Esperadas = refeições da dieta hoje publicada × dias da janela, contando a partir da primeira publicação do vínculo (um paciente novo não é penalizado por dias anteriores). Fuso da nutri.
- Por refeição/dia vale o último check-in. `percentual` = (feitas + trocadas) / esperadas × 100, uma casa decimal; `null` sem dieta publicada.
- `adesao7d` na listagem e na ficha (`GET /pacientes`, `GET /pacientes/{id}`) traz o mesmo valor de `dias7.percentual`. Não é enviado no `/sync` (campo derivado, continua `null` lá).

## Feed .ics (BE-17)

| Método/caminho | Auth | Resposta |
| --- | --- | --- |
| POST /api/v1/eu/calendario/token | Bearer PACIENTE | 201 `{token, url, urlWebcal}`; gerar outro revoga o anterior |
| DELETE /api/v1/eu/calendario/token | Bearer PACIENTE | 204 (idempotente) |
| GET /cal/{token}.ics | pública (o token é a credencial) | 200 `text/calendar`; token inexistente/revogado = 404 |

- O token (32 bytes aleatórios, base64url, 43 caracteres) só aparece na criação; o banco guarda o SHA-256. O app guarda o link. Base da URL: propriedade `dieta.publico.url-base` (padrão `https://api.dietamais.com.br`).
- O feed é calculado na leitura a partir da dieta em uso (S9): um `VEVENT` diário (`RRULE:FREQ=DAILY`) por refeição, `UID` = `refeicao-<id>@dietamais.com.br`, início na data da publicação, 30 min, fuso do paciente com `VTIMEZONE`. Título genérico `Dieta+: <nome da refeição>`, sem alimentos nem dado de saúde. Publicar nova dieta troca os eventos; sem dieta ativa o calendário vem vazio. Consultas entram na BE-23.
- RN-ASS-05: assinatura vencida da nutri não bloqueia o feed.

## Dispositivos e preferências de push (BE-18)

Qualquer perfil autenticado; não depende de assinatura (não é dado clínico).

| Método/caminho | Requisição | Resposta |
| --- | --- | --- |
| POST /eu/dispositivos | `{token, plataforma: ANDROID\|IOS\|DESKTOP, nomeAparelho?}` | 201 `{id, plataforma, nomeAparelho}` (nunca devolve o token) |
| DELETE /eu/dispositivos/{id} | — | 204; aparelho de outra conta = 404 |
| GET /eu/preferencias-notificacao | — | 200 `{tipos:[{tipo,ativo}], silencio:{ativo,inicio,fim}, versao}` + `ETag` |
| PUT /eu/preferencias-notificacao | `{tipos, silencio}` + `If-Match` | 200 (igual ao GET); versão velha = 409 `CONFLITO_VERSAO` com `atual` |

- O token é único: registrar um token que era de outra conta (troca de usuário no aparelho) move o aparelho para quem registrou. Registrar duas vezes o mesmo token devolve o mesmo `id`. No máximo 10 aparelhos por conta (saem os mais antigos). Chame `DELETE` no logout.
- Tipos por perfil (RN-NOT-01): PACIENTE = `NOVA_DIETA`, `QUESTIONARIO_ENVIADO`, `CONSULTA_AMANHA`; NUTRICIONISTA = `QUESTIONARIO_RESPONDIDO`, `PACIENTE_SEM_CHECKIN`, `ASSINATURA`. GET lista só os do perfil ativo. Tipo do outro perfil ou repetido = 400 `FORMATO_INVALIDO`; tipos ausentes no PUT ficam ligados.
- Padrão sem personalização: versão `0` (`If-Match: "0"` no primeiro PUT), tudo ligado, silêncio ativo 22:00–07:00 no fuso do usuário (pode cruzar a meia-noite; início = fim é inválido).
