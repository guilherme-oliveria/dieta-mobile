# Contrato da fase 5 — escrita offline da nutri (BE-C5, BE-25)

Cópia oficial no backend. Não há endpoint novo: a escrita offline usa o `POST /api/v1/sync` já descrito em [integracao.md](integracao.md) (envelope, resultados, idempotência por `idMutacao`). Esta fase só acrescenta **entidades** da nutri. Em vez de controllers `501`, o contrato é a tabela abaixo: o `dados` de cada mutação é o **mesmo corpo do endpoint REST** da entidade, e as mesmas regras valem (RN-SYN-04).

## Entidades

| `entidade` | `CRIAR` (`id` da mutação = id da entidade) | `ATUALIZAR` | Corpo (`dados`) | Versão inicial |
| --- | --- | --- | --- | --- |
| `pacientes` (ficha) | sim | sim | `CriarPacienteRequisicao` / `AtualizarPacienteRequisicao` | 0 |
| `alimentos` | sim | sim | `CriarAlimentoRequisicao` / `AtualizarAlimentoRequisicao` | 0 |
| `listas` (lista de substituição do paciente) | não: nasce com a ficha | sim; `id` = id da ficha | `AtualizarListaSubstituicaoRequisicao` (`adicionados`, `removidos`) | 0 |
| `dietas` (só rascunho) | sim | sim | `SalvarDietaRequisicao`; no `CRIAR`, `pacienteId` (ficha) vai dentro de `dados` | 1 |
| `avaliacoes` | sim | sim | `SalvarAvaliacaoRequisicao`; no `CRIAR`, `pacienteId` (ficha) dentro de `dados`; `justificativa` se passou de 30 dias (RN-AVA-04) | 1 |
| `modelos` (modelo de questionário) | sim | sim | `SalvarModeloRequisicao` | 1 |
| `consultas` | sim | sim (inclui cancelar: `{"status":"CANCELADA"}`) | `CriarConsultaRequisicao` / `AtualizarConsultaRequisicao` | 1 |

## Regras

- `ATUALIZAR` exige `versaoBase` (sem ela: `REJEITADA` `FORMATO_INVALIDO`). Se a versão no servidor for outra, o resultado é `CONFLITO` com `atual` (o registro como o REST devolveria) e nada é gravado (RN-SYN-02). "Manter o meu" reenvia com `versaoBase` = `atual.versao`.
- `EXCLUIR` não existe nestas entidades (desativar alimento é `ATUALIZAR` com `ativo=false`): `REJEITADA` `FORMATO_INVALIDO`, regra `RN-SYN-04`.
- Corpo inválido, entidade desconhecida ou `pacienteId` ausente/ilegível: `REJEITADA` `FORMATO_INVALIDO`. Violação de regra (limite de plano, vínculo encerrado, dieta já publicada, data de consulta fora da janela etc.): `REJEITADA` com o código da regra, sem derrubar o resto do lote.
- Isolamento: tudo é filtrado pela nutri da sessão. Entidade ou ficha de outra nutri: `REJEITADA` `RECURSO_NAO_ENCONTRADO`. Paciente enviando estas entidades: `REJEITADA` `PERFIL_INVALIDO`.
- Exigem estar online e **não** passam pelo sync: publicar dieta, convidar paciente, enviar questionário, gerar PDF, ações de assinatura, concluir questionário.
- Reenviar a mesma `idMutacao` com usuário, perfil e conteúdo idênticos devolve o mesmo resultado. Reuso com outro perfil/usuário ou alteração de entidade/operação/id/versaoBase/dados retorna `REJEITADA` `CHAVE_IDEMPOTENCIA_REUTILIZADA`, sem expor o resultado anterior. A comparação usa JSON canônico autenticado (propriedades ordenadas, números equivalentes normalizados, arrays em ordem), conforme [integração](integracao.md). Registros anteriores à migração 029 sem metadados falham fechados: o cliente reconcilia o estado antes de uma nova intenção; não há reaplicação automática do legado. Reenviar um `CRIAR` com outra `idMutacao` mas o mesmo `id` dá `CONFLITO`.
- Ordem: o app envia na ordem de criação, e uma mutação que depende de outra (dieta que usa o alimento criado offline) só vai depois da dela; se a anterior for rejeitada ou conflitar, a dependente volta com o código da regra.
