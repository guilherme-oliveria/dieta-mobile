# Contrato da fase 2 — BE-C2

Cópia oficial no backend. Mobile e frontend devem acompanhar este contrato; frontend continua pós-MVP. O manifesto `src/test/resources/contrato/fase-2/manifesto.json` publica 16 operações e exemplos fictícios tipados, separados dos 49 JSONs da fase 1, que permanecem intactos.

Esta entrega publica DTOs, OpenAPI e rotas **501 NAO_IMPLEMENTADO**, sem persistência, carga de catálogo, cálculo, publicação, push ou autorização clínica da fase 2 implementados. Sucessos abaixo são o contrato previsto para BE-09..14 e BE-33, para uso com MockEngine até cada gate QA. Não tratar 501 como lista vazia nem sucesso. Bearer continua obrigatório; validação de transporte pode retornar 400 antes do stub. O ProblemDetail 501 informa que a implementação pertence à fase 2.

## Operações

Todos os caminhos começam em `/api/v1`. Os nomes de classes abaixo ficam em `alimentos.api`, `dietas.api` ou `contas.api`.

| Método/caminho | Requisição | Sucesso previsto | Tarefa |
| --- | --- | --- | --- |
| GET /grupos-alimentos | — | 200 GruposAlimentosResposta | BE-09 |
| GET /alimentos | pagina, tamanho, grupoId?, busca?, ativo? | 200 PaginaAlimentosResposta | BE-10 |
| POST /alimentos | CriarAlimentoRequisicao | 201 AlimentoResposta | BE-10 |
| PATCH /alimentos/{id} | AtualizarAlimentoRequisicao + If-Match | 200 AlimentoResposta | BE-10 |
| GET /pacientes/{id}/lista-substituicao | — | 200 ListaSubstituicaoResposta | BE-10 |
| PUT /pacientes/{id}/lista-substituicao | AtualizarListaSubstituicaoRequisicao + If-Match | 200 ListaSubstituicaoResposta | BE-10 |
| GET /pacientes/{id}/dietas | — | 200 DietasResposta | BE-11 |
| POST /pacientes/{id}/dietas | SalvarDietaRequisicao | 201 DietaResposta | BE-11 |
| GET /dietas/{id} | — | 200 DietaResposta | BE-11 |
| PUT /dietas/{id} | SalvarDietaRequisicao + If-Match | 200 DietaResposta | BE-11 |
| POST /dietas/{id}/publicar | sem corpo + If-Match | 200 DietaSnapshotResposta | BE-12 |
| POST /dietas/{id}/duplicar | sem corpo | 201 DietaResposta | BE-12 |
| GET /eu/dietas | — | 200 DietasPacienteResposta | BE-13 |
| PUT /eu/dieta-em-uso | SelecionarDietaEmUsoRequisicao + If-Match | 200 DietaEmUsoResposta | BE-13 |
| GET /eu/nutri | — | 200 PerfilNutriResposta | BE-33 |
| PATCH /eu/nutri | AtualizarPerfilNutriRequisicao + If-Match | 200 PerfilNutriResposta | BE-33 |

## Identidade, concorrência e erros

Rotas da nutri usam o dono autenticado; `{id}` de paciente é o ID global da fase 1 e a ficha/vínculo é resolvida no contexto da nutri. `/eu/dietas` e seleção são do paciente autenticado. Nenhuma requisição recebe `nutricionistaId`, `usuarioId`, totais calculados ou status editável. Implementações devem reaplicar isolamento/assinatura/visibilidade, inclusive 404 antes de expor `atual` em conflito, e manter RN-VIN-05 para histórico ENCERRADO. A anotação de rota nutri integra a política transversal existente; os stubs não substituem os testes de isolamento das tarefas de implementação.

PUT/PATCH e publicação usam `If-Match: "<versao>"` da entidade, nunca a revisão do envelope sync. `versao` de DietaResposta, AlimentoResposta, ListaSubstituicaoResposta e PerfilNutriResposta identifica a entidade respectiva. GET de entidade e sua escrita retornam ETag forte; em coleções, a versão é a de cada item. `DietasPacienteResposta.versao` versiona a **seleção** de dieta em uso; seu ETag fundamenta PUT /eu/dieta-em-uso. Não usar versão de uma dieta ou de seu snapshot para alterar essa seleção. O PUT retorna dietaId e a nova versão da seleção. A seleção inicial segue a publicada mais recente (RN-DIE-07/S9), sem decidir isso no cliente.

POSTs declaram `Idempotency-Key` UUID opcional, conforme a política transversal de 24 horas; persistência/reprodução será conectada nas tarefas reais. O contrato 501 desta entrega não grava idempotência nem consome versões. IDs de criação podem ser UUIDv7 do cliente ou nulos para geração no servidor; no PUT da dieta, `id` deve ser nulo ou corresponder ao caminho. IDs de refeições/itens preservados representam os mesmos itens; novos podem vir nulos. A ordem dos itens é a ordem da lista; refeições têm `ordem` explícita e `horario` local `HH:mm:ss`, sem inventar offset para recorrência diária.

Erros seguem ProblemDetail/codigo. A fase reutiliza `DIETA_VAZIA` (422/RN-DIE-06), `DIETA_NAO_EDITAVEL` (422/RN-DIE-04), `CONFLITO_VERSAO` (409/RN-SYN-02), `ASSINATURA_INATIVA`, `PERFIL_INVALIDO`, `RECURSO_NAO_ENCONTRADO`, `OBRIGATORIO` e `FORMATO_INVALIDO`. Os dois códigos específicos de dieta já estavam no enum: não há código novo artificial. OpenAPI declara 400/401/403/404/409/422 e 501 com ProblemaResposta. Fixtures de conflito só contêm dados fictícios e exemplificam `atual` autorizado.

## Alimentos e lista

Grupo publica kcal e carboidratos/proteínas/gorduras **por porção** (macros em gramas). Alimento pertence a um grupo e define medida caseira textual e gramas de uma porção, positivas. `catalogoId` é referência opcional à origem da cópia; o ID usado pela dieta é o alimento da lista da nutri. Os exemplos não são carga real nem inventam os grupos faltantes do PDF.

GET /alimentos: pagina zero, tamanho padrão 20 (1..100), busca por nome até 150 caracteres, grupo opcional e ativo padrão true; resposta contém itens/pagina/tamanho/total. PATCH envia todos os campos editáveis, incluindo `ativo`; desativação preserva snapshots (RN-ALI-03). Não há DELETE nem edição do catálogo base nesta fase.

ListaSubstituicaoResposta contém a lista efetiva em `alimentos` e os IDs das diferenças `adicionados`/`removidos` sobre a lista da nutri (RN-ALI-01). PUT substitui essas diferenças inteiras, não a lista de outra nutri/paciente. Arrays são obrigatórios e podem ser vazios para limpar diferenças; IDs pertencem à lista autorizada da nutri. Duplicidades, interseção e referências inválidas serão validadas pelo backend em BE-10. A lista congelada do snapshot é independente de futuras mudanças na lista efetiva.

## Dieta e snapshot

SalvarDietaRequisicao envia título, orientação opcional e refeições com itens `{alimentoId, porcoes, observacao?}`. Porções são decimais positivas; nomes/título têm até 150 caracteres, observação até 2000 e orientação até 10000. O rascunho pode ter lista vazia; RN-DIE-06 é validada ao publicar, descartando refeições vazias e recusando dieta sem itens. Totais e medidas resultantes são calculados no backend (RN-DIE-05), sem confiar em prévia do cliente.

DietaResposta traz paciente/vínculo, status RASCUNHO/PUBLICADA/SUBSTITUIDA/ARQUIVADA, versão e datas. Cada refeição/item inclui totais, e o dia inclui totais. Macros são gramas; `gramas` do item é a quantidade total para as porções e `medidaCaseira` é a descrição calculada correspondente. PUT substitui o rascunho inteiro; publicada não é editada. Duplicação cria novo rascunho (RN-DIE-04).

Publicar retorna DietaSnapshotResposta: ID do snapshot e dieta de origem, paciente/vínculo, resumo da nutri, refeições/itens, totais, orientação e lista de substituição congelados, data de publicação. É payload imutável; futuras substituições da dieta não reescrevem seu conteúdo. A implementação BE-12 aplica unicidade da publicada, congela a orientação padrão quando cabível e gera evento/push/feed conforme RN-DIE-02/03/08/09. GET /eu/dietas traz snapshots recebidos de todas as nutris e dietaEmUsoId, inclusive histórico autorizado; assinatura posterior da nutri não bloqueia essa leitura (RN-ASS-05). Troca de alimento usa a lista do snapshot e o mesmo grupo; o check-in permanece em tarefa posterior.

BE-14 acrescentará fontes/coleções opcionais de sync, preservando o contrato BE-08 e a distinção revisão da projeção/versão da entidade. Esta entrega não altera GET /sync nem publica coleções fictícias como se já estivessem implementadas.

## Perfil da nutri

GET/PATCH /eu/nutri inclui nome de exibição, CRN no formato já usado no cadastro (`CRN-1 00000` fictício), telefone opcional e orientação padrão opcional. PATCH envia todos esses campos; opcionais nulos limpam o valor. E-mail, senha, organização, assinatura e logo não são editados por esse contrato. Logo pertence a BE-28. A orientação será congelada na publicação da dieta, não aplicada retroativamente.
