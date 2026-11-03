# Contrato da fase 6 — assinatura, PDFs, LGPD e admin (BE-C6)

Cópia oficial no backend. As seções entram junto com a implementação de cada tarefa (BE-26 a BE-30). Erros seguem o `ProblemDetail` de [integracao.md](integracao.md).

## Assinatura e checkout (BE-26)

O app **não vende nada** (RN-ASS-06): ele só pede o link do checkout hospedado do gateway (Asaas `[SUPOSIÇÃO S3]`) e abre no navegador externo. O status da assinatura só muda por webhook validado, job diário ou ação do admin (RN-ASS-07).

### `GET /api/v1/assinatura` (nutri)

Funciona mesmo com a assinatura inativa (é só leitura).

```json
{
  "organizacaoId": "…",
  "plano": { "codigo": "TESTE", "nome": "Teste", "precoMensal": 0.00, "limitePacientes": 5 },
  "status": "TESTE",
  "pacientesOcupados": 2,
  "fimPeriodoEm": "2026-11-27T12:00:00Z",
  "carenciaAte": null,
  "pagamentoDisponivel": true,
  "planosDisponiveis": [ { "codigo": "BASICO_20", "nome": "Básico", "precoMensal": 29.90, "limitePacientes": 20 } ]
}
```

- `carenciaAte` só vem em `EM_ATRASO` (`fimPeriodoEm` + 7 dias): depois dela a nutri fica só leitura (RN-ASS-04).
- `pagamentoDisponivel=false` quando o servidor está sem chave do gateway; o app esconde o botão de contratar.

### `POST /api/v1/assinatura/checkout` (nutri)

Corpo `{ "plano": "BASICO_20" }`; resposta `{ "url": "https://…" }`. Chamar de novo para o mesmo plano devolve o link da cobrança em aberto.

| Situação | Resposta |
| --- | --- |
| Plano inexistente, inativo | `404 RECURSO_NAO_ENCONTRADO` |
| Plano gratuito (`TESTE`) | `400 FORMATO_INVALIDO` |
| Assinatura já `ATIVA` | `409 ASSINATURA_JA_ATIVA` (RN-ASS-06) |
| Gateway sem chave ou fora do ar | `503 PAGAMENTO_INDISPONIVEL` (RN-ASS-06) |
| Paciente | `403 PERFIL_INVALIDO` |

### `POST /api/v1/webhooks/pagamento` (público, só o gateway)

Exige o cabeçalho `asaas-access-token` igual a `ASAAS_WEBHOOK_TOKEN` (comparação em tempo constante); sem ele, ou com servidor sem token configurado, `401`. Corpo inválido: `400 FORMATO_INVALIDO`. O evento é gravado em `evento_pagamento` com `evento_id` único: repetido responde `200` sem efeito.

| Evento do gateway | Efeito |
| --- | --- |
| `PAYMENT_CONFIRMED`, `PAYMENT_RECEIVED` | `ATIVA`; troca para o plano contratado; `fimPeriodoEm` = vencimento + 1 mês |
| `PAYMENT_OVERDUE` | `ATIVA` → `EM_ATRASO`; `fimPeriodoEm` = vencimento em atraso (carência de 7 dias, RN-ASS-04) |
| `SUBSCRIPTION_DELETED`, `SUBSCRIPTION_INACTIVATED` | `ATIVA`/`EM_ATRASO` → `CANCELADA`; em teste só solta o vínculo com o gateway |
| recusa de cartão / análise de risco | só avisa |
| outros | ignorados (`200`) |

Cada mudança relevante gera um aviso `ASSINATURA` (genérico, RN-NOT-02) para as nutris da organização.

Configuração (somente variáveis de ambiente): `ASAAS_API_KEY`, `ASAAS_WEBHOOK_TOKEN`, `ASAAS_URL_BASE` (padrão: sandbox).

## Ciclo de vida da assinatura (BE-27)

Sem endpoint novo. Um job de hora em hora (`dieta.assinatura.intervalo-ciclo`) mantém os status coerentes e avisa a nutri (aviso `ASSINATURA`, genérico, push e central; e-mail fica para depois):

| Situação | Efeito |
| --- | --- |
| `TESTE` com `fimPeriodoEm` vencido | `EXPIRADA` (RN-ASS-02) + aviso |
| `TESTE` a 3 dias do fim (`dieta.assinatura.aviso-fim-teste`) | aviso, uma vez |
| `ATIVA` sem renovação por mais de 3 dias (`dieta.assinatura.tolerancia-webhook`, webhook perdido) | `EM_ATRASO` + aviso |
| `EM_ATRASO` além da carência de 7 dias (RN-ASS-04) | aviso de bloqueio, uma vez por vencimento; escrita já recusa com `ASSINATURA_INATIVA` |

Downgrade (RN-ASS-08): com plano de limite menor que os pacientes ativos, novos convites ficam bloqueados (`LIMITE_PACIENTES_ATINGIDO`) e nenhum vínculo é encerrado. O paciente nunca é afetado (RN-ASS-05).

## PDFs e logo da nutri (BE-28)

Os PDFs são gerados na hora e entregues por **link assinado de 10 minutos**; o arquivo é apagado em seguida (job a cada 5 min remove o que tem mais de 30 min). Nada de dado de saúde fica guardado. O cabeçalho traz nome de exibição, CRN e a logo da nutri (se enviada) e o nome do paciente.

| Endpoint | Perfil | Observação |
| --- | --- | --- |
| `GET /api/v1/dietas/{id}/pdf` | Nutri | PDF do **snapshot publicado**; dieta nunca publicada: `404` |
| `GET /api/v1/avaliacoes/{id}/pdf` | Nutri | Avaliação da nutri |
| `GET /api/v1/eu/dietas/{id}/pdf` | Paciente | Snapshot que recebeu (vínculo ativo ou encerrado) |
| `GET /api/v1/eu/avaliacoes/{id}/pdf` | Paciente | Só as compartilhadas (RN-AVA-05); não compartilhada: `404` |
| `PUT /api/v1/eu/nutri/logo` | Nutri | Corpo é a imagem (`Content-Type: image/png` ou `image/jpeg`), até 512 KB, conferida pelos bytes; inválida: `400 FORMATO_INVALIDO`. `204` |
| `DELETE /api/v1/eu/nutri/logo` | Nutri | `204` |

Resposta dos `GET`: `{ "url": "https://.../api/v1/arquivos/<token>?exp=...&sig=...", "expiraEm": "2026-10-28T12:10:00Z" }`. O cliente abre a `url` direto (navegador/visualizador do sistema); ela não leva `Authorization`. Link adulterado ou vencido: `403`.

`GET /eu/nutri` ganha o campo opcional `temLogo` (boolean). Leitura de PDF pela nutri entra na auditoria de acesso clínico (RN-LGPD).

Configuração do armazenamento: sem `S3_ENDPOINT` grava em disco (`ARQUIVOS_DIR`, `ARQUIVOS_SEGREDO`), próprio para dev/beta; com `S3_ENDPOINT`, `S3_BUCKET`, `S3_REGIAO`, `S3_CHAVE_ACESSO`, `S3_CHAVE_SECRETA` usa S3/MinIO com URL pré-assinada. `URL_PUBLICA` é a base dos links do modo local.

## Exportação de dados do paciente (BE-29a, RN-LGPD-02)

Só o paciente. O pedido é aceito na hora e gerado em segundo plano (job a cada 30 s); quando pronto, um e-mail leva ao arquivo. O ZIP contém `dados.json` (tudo que o sync entrega ao paciente desde o início: perfil, vínculos, nutris, dietas recebidas, rotina, avaliações compartilhadas, questionários, consultas, avisos), `dietas/*.pdf`, `avaliacoes/*.pdf` e `LEIAME.txt`. Fica disponível por 24 h e é apagado.

| Endpoint | Resposta |
| --- | --- |
| `POST /api/v1/eu/exportacao` | `202` `{ id, status: "PENDENTE", criadoEm }`. Com pedido pendente, ou feito há menos de 1 h: `409 EXPORTACAO_EM_ANDAMENTO` (RN-LGPD-02) |
| `GET /api/v1/eu/exportacao` | `200` pedido mais recente: `status` `PENDENTE`, `PRONTA`, `FALHOU` (após 3 tentativas) ou `EXPIRADA`. Com `PRONTA` traz `url` (vale 15 min, consulte de novo para renovar), `prontaEm`, `expiraEm`. `404` se nunca pediu |
| `GET /api/v1/exportacoes/{token}` | Público, só o link do e-mail: `302` para o arquivo assinado (5 min); token desconhecido ou vencido: `404` |

Nutri recebe `403 PERFIL_INVALIDO`. O e-mail (tipo `EXPORTACAO_PRONTA`) usa `dieta.publico.url-base` (`URL_PUBLICA`) para montar o link. Nada do conteúdo vai para log.

## Exclusão de conta e de prontuário (BE-29b, RN-LGPD-03/04)

### `POST /api/v1/eu/excluir-conta` (paciente)

Corpo `{ "senha": "..." }` (reautenticação). `204` e **irreversível**. Senha errada: `401 CREDENCIAIS_INVALIDAS`; nutri: `403 PERFIL_INVALIDO`.

O que acontece (RN-LGPD-03):

| Dado | Destino |
| --- | --- |
| Conta (`nome`, `e-mail`, senha), perfil pessoal (nascimento, sexo) | anonimizados; login e tokens deixam de funcionar na hora |
| Sessões, aparelhos, preferências de notificação, avisos, feed `.ics`, exportações (e arquivos) | apagados |
| Vínculos abertos | `ENCERRADO`; a nutri vê o histórico só leitura (RN-VIN-05) |
| Prontuário de cada nutri (fichas, dietas, avaliações, consultas, respostas, registros de rotina) | **retido** pelo prazo legal, sem a conta que o originou |
| Consentimento e log de acesso | retidos (prova legal, RN-LGPD-01) |

Se o mesmo login também for de nutri, só o lado paciente sai (conta, sessões do perfil nutri e dados dela permanecem).

### `DELETE /api/v1/pacientes/{id}` (nutri)

Exclusão **lógica** da ficha (RN-LGPD-04), com `If-Match`. `204`: a ficha some da lista e do sync da nutri (`excluido: true`), mas o registro continua no banco para a retenção legal `[SUPOSIÇÃO S6]`. Vínculo `ATIVO`: `422 VINCULO_ATIVO` (encerre antes). Vínculo `CONVIDADO`: convite invalidado, vínculo encerrado e vaga do plano liberada. O paciente que já aceitou mantém o próprio histórico. Restauração só por suporte, direto no banco.

## Admin da plataforma (BE-30, RN-LGPD-05)

Perfil `ADMIN` (login normal em `/auth/login`; a conta é criada na subida por `ADMIN_EMAIL` + `ADMIN_SENHA`, mínimo 12 caracteres, só variáveis de ambiente). **Nenhuma rota de dado clínico ou de usuário** aceita o admin: ele leva `403 PERFIL_INVALIDO` em tudo que não seja `/api/v1/admin/**`, `/api/v1/eu` (a própria identidade), `/api/v1/auth/**` e os termos de uso. As respostas de admin trazem só dados de conta (nome da conta, plano, status, CRN) e uma **contagem** de pacientes. Nutri e paciente recebem `403 PERFIL_INVALIDO` nas rotas de admin.

Toda alteração exige `motivo` (até 500 caracteres) e grava em `log_admin` (quem, o quê, antes, depois, motivo, quando, IP). A tabela é somente inserção (trigger bloqueia `UPDATE` e `DELETE`).

| Endpoint | Descrição |
| --- | --- |
| `GET /admin/planos` | Todos os planos, inclusive inativos |
| `POST /admin/planos` | Cria plano: `codigo` (maiúsculas, imutável), `nome`, `precoMensal`, `limitePacientes`, `duracaoDias` (só plano de preço zero), `motivo`. Código repetido: `409 VINCULO_JA_EXISTE` |
| `PUT /admin/planos/{id}` | Atualiza `nome`, `precoMensal`, `limitePacientes`, `duracaoDias`, `ativo`, `motivo`. Preço novo vale para novos checkouts. O plano `TESTE` não desativa e mantém a duração |
| `GET /admin/assinaturas?status=&busca=&pagina=&tamanho=` | Página de assinaturas: conta, plano, status, fim do período, `pacientesOcupados` (contagem), `gatewayVinculado` |
| `GET /admin/assinaturas/{id}` | Uma assinatura |
| `PATCH /admin/assinaturas/{id}` | Ajuste manual de `planoCodigo`, `status` e/ou `fimPeriodoEm` (ao menos um) + `motivo`. O webhook do gateway continua valendo depois |
| `GET /admin/nutricionistas?busca=&verificado=` | Nome, e-mail, CRN e `crnVerificado` |
| `PUT /admin/nutricionistas/{id}/crn-verificado` | `{ verificado, motivo }`. Editar o CRN pela nutri volta a verificação para falso |
