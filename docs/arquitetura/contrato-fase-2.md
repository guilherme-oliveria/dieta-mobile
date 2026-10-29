# Contrato da fase 2 — BE-C2

Cópia oficial no backend. Mobile e frontend devem acompanhar este contrato; frontend continua pós-MVP. O manifesto `src/test/resources/contrato/fase-2/manifesto.json` publica 17 operações e exemplos tipados, separados dos 49 JSONs da fase 1, que permanecem intactos.

BE-C2 publicou DTOs/OpenAPI/stubs. BE-33 implementa perfil e BE-10 implementa as operações de grupos, alimentos e lista do paciente, incluindo a extensão de edição de macros. Quatro operações de dietas (listar, criar, consultar e salvar rascunho) foram implementadas na BE-11; publicar e duplicar entraram na BE-12; `/eu/dietas` e `/eu/dieta-em-uso` entraram na BE-13: nenhuma operação da fase 2 responde mais 501. Não tratar 501 como lista vazia nem sucesso. Bearer é obrigatório; validação de transporte pode retornar 400 antes do stub. Não foram antecipados cálculo, publicação, push ou fontes BE-14.

## Operações

Todos os caminhos começam em `/api/v1`. Os nomes de classes abaixo ficam em `alimentos.api`, `dietas.api` ou `contas.api`.

| Método/caminho | Requisição | Sucesso previsto | Tarefa |
| --- | --- | --- | --- |
| GET /grupos-alimentos | — | 200 GruposAlimentosResposta | BE-09 |
| PATCH /grupos-alimentos/{id} | AtualizarMacrosGrupoRequisicao + If-Match | 200 GrupoAlimentoResposta | BE-10 |
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

POSTs declaram `Idempotency-Key` UUID opcional, conforme a política transversal de 24 horas. POST /alimentos já persiste/reproduz sucessos; stubs de dieta não gravam idempotência nem consomem versões. IDs de criação podem ser UUIDv7 do cliente ou nulos para geração no servidor; no PUT futuro da dieta, id será nulo ou corresponderá ao caminho. IDs de refeições/itens preservados representam os mesmos itens; novos podem vir nulos. A ordem dos itens é a da lista; refeições têm ordem explícita e horário local HH:mm:ss, sem inventar offset para recorrência diária.

Erros seguem ProblemDetail/codigo. A fase reutiliza `DIETA_VAZIA` (422/RN-DIE-06), `DIETA_NAO_EDITAVEL` (422/RN-DIE-04), `CONFLITO_VERSAO` (409/RN-SYN-02), `ASSINATURA_INATIVA`, `PERFIL_INVALIDO`, `RECURSO_NAO_ENCONTRADO`, `OBRIGATORIO` e `FORMATO_INVALIDO`. Os dois códigos específicos de dieta já estavam no enum: não há código novo artificial. OpenAPI declara 400/401/403/404/409/422 e 501 com ProblemaResposta. Fixtures de conflito só contêm dados fictícios e exemplificam `atual` autorizado.

## Alimentos e lista

### Extensão BE-10 — cópias e edição isolada

BE-10 acrescenta PATCH /grupos-alimentos/{id}: corpo `{carboidratosPorPorcao, proteinasPorPorcao, gordurasPorPorcao}`, números não negativos com no máximo quatro casas e oito dígitos inteiros; If-Match obrigatório. Resposta 200 GrupoAlimentoResposta/ETag/no-store, mesma identidade pública do grupo. Campos novos opcionais de resposta: `versao` e `origemMacros` (`CATALOGO_ESTIMADO`, `CATALOGO`, `AJUSTE_NUTRI`). Os exemplos antigos sem esses campos continuam válidos. O editor salva somente a cópia da nutri autenticada e não recebe dono no corpo.

A origem inicial completa é preservada no banco. Um ajuste manual registra usuário/data/origem própria; `macrosEstimados` fica omitido quando não há declaração do método manual, pois editar não comprova análise laboratorial. Kcal e porção do catálogo não são alteradas por esse PATCH. Cópias são criadas uma única vez para nutris existentes e no cadastro; GET não recopia o catálogo e mudanças administrativas não propagam a cópias já existentes.

Lista do paciente tem baseline dos alimentos ativos da nutri ao criar o vínculo e diferenças adicionados/removidos. Adicionar alimento novo a uma lista não altera baseline/ajustes de outros pacientes. Enquanto não houver snapshot, os dados atuais dos alimentos próprios da nutri são usados; desativados saem das novas escolhas. Os ajustes pertencem ao vínculo escolhido pela mesma regra de ficha BE-06/07 (aberto ou histórico mais recente). ENCERRADO mantém leitura e retorna 403 em escrita. IDs adicionados/removidos devem ser próprios, sem duplicidade/interseção; adição exige ativo. Referência de outra nutri retorna 404.

PUT da lista substitui todas as diferenças, com If-Match da lista efetiva, nunca versão da ficha ou revisão do sync. Versões da lista podem ter saltos e mudam também após edição de alimento/grupo próprio, evitando sobrescrever uma configuração baseada em dados antigos. Baseline não é recriada em leitura. Snapshots BE-12 congelarão valores/lista/proveniência e não serão reescritos por essas edições.

POST /alimentos admite UUIDv7 opcional do cliente e Idempotency-Key; PATCH altera o conjunto de campos editáveis com If-Match, inclusive ativo. Não há DELETE. Busca é substring literal do nome sem distinção de maiúsculas; paginação zero, tamanho 1..100, grupo/ativo próprios, ordem nome/id estável. Conflito inclui apenas a projeção autorizada em atual. Assinatura inativa bloqueia escrita no filtro e serviço; paciente/admin não acessam essas rotas da nutri. Lista clínica é auditada somente por identificadores, sem conteúdo na trilha.

BE-09: GET /grupos-alimentos retorna catálogo global real para NUTRICIONISTA autenticada, no-store e ordem nome/id. São 12 grupos e 482 alimentos base carregados, sem dados pessoais. Macros iniciais são estimativas por representantes TACO/UNICAMP normalizados pelas kcal do PDF, com campo novo **opcional** `macrosEstimados` (true na carga inicial; ausente nos exemplos antigos). Nenhum campo existente/nulabilidade de macro mudou e os JSONs anteriores continuam válidos. Exemplo adicional oficial: `contrato/fase-2/listarGruposAlimentos/resposta-estimativa.json`, registrado no manifesto sem substituir exemplos anteriores. [Fonte, método, arredondamento e limites](catalogo-base.md) explicam a diferença entre porção de grupo e 100 g de alimento, inclusive proxy de sopa.

Edição funcional da API de macros está implementada na BE-10: cópia própria e PATCH com If-Match. A integração do editor no app é do mobile; não foi implementada neste repositório. `listarGruposAlimentos/resposta-copia.json` exemplifica versão/origem inicial e `atualizarMacrosGrupo/` exemplifica ajuste manual. A fixture de desativação de alimento foi corrigida para ativo=false/versão=2, coerente com a requisição; o formato e os demais exemplos anteriores permanecem válidos. Atualização do catálogo não propaga a cópias existentes. Não há API de escrita global para nutris.

Grupo publica kcal e carboidratos/proteínas/gorduras **por porção** (macros em gramas). Alimento pertence a um grupo e define medida caseira textual e gramas de uma porção, positivas. `catalogoId` é referência opcional à origem da cópia; o ID usado pela dieta é o alimento da lista da nutri. Os exemplos não são carga real nem inventam os grupos faltantes do PDF.

GET /alimentos: pagina zero, tamanho padrão 20 (1..100), busca por nome até 150 caracteres, grupo opcional e ativo padrão true; resposta contém itens/pagina/tamanho/total. PATCH envia todos os campos editáveis, incluindo `ativo`; desativação preserva snapshots (RN-ALI-03). Não há DELETE nem edição do catálogo base nesta fase.

ListaSubstituicaoResposta contém alimentos efetivos e IDs adicionados/removidos sobre a baseline do vínculo (RN-ALI-01). PUT substitui diferenças inteiras. Arrays obrigatórios/vazios limpam diferenças; duplicidade/interseção retorna 400 FORMATO_INVALIDO. Referências de outra nutri, excluídas ou inativas para adição retornam 404; remoção de item próprio inativo é permitida. A lista congelada futura do snapshot é independente de mudanças nessa projeção atual. Versões efetivas usam sequência dedicada e podem ter saltos; não são revisão do envelope sync.

## Dieta e snapshot

SalvarDietaRequisicao envia título, orientação opcional e refeições com itens `{alimentoId, porcoes, observacao?}`. Porções são decimais positivas; nomes/título têm até 150 caracteres, observação até 2000 e orientação até 10000. O rascunho pode ter lista vazia; RN-DIE-06 é validada ao publicar, descartando refeições vazias e recusando dieta sem itens. Totais e medidas resultantes são calculados no backend (RN-DIE-05), sem confiar em prévia do cliente.

DietaResposta traz paciente/vínculo, status RASCUNHO/PUBLICADA/SUBSTITUIDA/ARQUIVADA, versão e datas. Cada refeição/item inclui totais, e o dia inclui totais. Macros são gramas; `gramas` do item é a quantidade total para as porções e `medidaCaseira` é a descrição calculada correspondente. PUT substitui o rascunho inteiro; publicada não é editada. Duplicação cria novo rascunho (RN-DIE-04).

Publicar retorna DietaSnapshotResposta: ID do snapshot e dieta de origem, paciente/vínculo, resumo da nutri, refeições/itens, totais, orientação e lista de substituição congelados, data de publicação. É payload imutável; futuras substituições da dieta não reescrevem seu conteúdo. A implementação BE-12 aplica unicidade da publicada, congela a orientação padrão quando cabível e gera evento/push/feed conforme RN-DIE-02/03/08/09. GET /eu/dietas traz snapshots recebidos de todas as nutris e dietaEmUsoId, inclusive histórico autorizado; assinatura posterior da nutri não bloqueia essa leitura (RN-ASS-05). Troca de alimento usa a lista do snapshot e o mesmo grupo; o check-in permanece em tarefa posterior.

BE-14 acrescentará fontes/coleções opcionais de sync, preservando o contrato BE-08 e a distinção revisão da projeção/versão da entidade. Esta entrega não altera GET /sync nem publica coleções fictícias como se já estivessem implementadas.

## Perfil da nutri

BE-33: GET/PATCH implementados com no-store e ETag forte, versão inicial 0. PATCH bloqueia a linha própria antes de comparar If-Match; conflito retorna 409 com o perfil próprio em `atual`, sem sobrescrever. Assinatura inativa bloqueia escrita também no serviço, mantendo leitura. Strings são aparadas e opcionais vazios viram nulo. Nome obrigatório e CRN seguem os limites e formato do cadastro; telefone admite 30 caracteres e orientação 10000. Mudança de CRN remove sua marca de verificação; o sistema não presume validação profissional.

Nome é o **nome de exibição da nutri**, com fallback ao nome da conta em cadastros existentes; alterar o perfil não muda `usuario.nome`, e-mail, senha ou perfil de paciente da mesma conta. Resumos públicos em convites, vínculos e sync usam esse nome efetivo. Telefone e orientação permanecem privados no perfil e não entram no resumo público do sync. A orientação padrão será congelada em BE-12, sem alteração retroativa de snapshots. Perfil não é prontuário clínico; sua consulta não grava conteúdo na trilha clínica.

Migração 011 é nova, sem editar 009; a numeração 010 foi reservada para BE-09 em andamento. O trigger de identidade pública passa a incluir nome de exibição e preserva a revisão monotônica do diário. Não há mudança nos JSONs de exemplos ou campos do contrato C2.

GET/PATCH /eu/nutri inclui nome de exibição, CRN no formato já usado no cadastro (`CRN-1 00000` fictício), telefone opcional e orientação padrão opcional. PATCH envia todos esses campos; opcionais nulos limpam o valor. E-mail, senha, organização, assinatura e logo não são editados por esse contrato. Logo pertence a BE-28. A orientação será congelada na publicação da dieta, não aplicada retroativamente.

BE-11: rascunhos de dieta reais. `POST /pacientes/{id}/dietas` (201, `Idempotency-Key` opcional), `GET /pacientes/{id}/dietas`, `GET /dietas/{id}` e `PUT /dietas/{id}` (If-Match; substitui a dieta inteira). Totais (RN-DIE-05) são sempre calculados no backend: item = porções × macros do grupo da cópia da nutri, 2 casas HALF_UP; refeição e dia somam os itens arredondados. Itens só aceitam alimentos ativos da própria lista (outra nutri = 404; inativo = 400 `FORMATO_INVALIDO`/RN-ALI-03). Dieta publicada responde 422 `DIETA_NAO_EDITAVEL`; vínculo encerrado permite leitura e recusa escrita (403, RN-VIN-05). Limites: 20 refeições e 60 itens por refeição, porções até 4 casas. IDs de refeição/item/dieta são opcionais (UUIDv7); id já usado por outra dieta responde 409 sem `atual`. Sem campos novos nos JSONs.

BE-12: `POST /dietas/{id}/publicar` (If-Match, `Idempotency-Key` opcional, 200 `DietaSnapshotResposta`) e `POST /dietas/{id}/duplicar` (201 `DietaResposta`, novo rascunho "Título (cópia)" com ids novos). Publicar: só `RASCUNHO` (senão 422 `DIETA_NAO_EDITAVEL`); refeições sem itens ficam fora do snapshot e sem nenhuma refeição com itens responde 422 `DIETA_VAZIA` (RN-DIE-06); alimento inativo recusa com 400 `FORMATO_INVALIDO`/RN-ALI-03; orientação da dieta, ou a padrão da nutri se vazia, é congelada (RN-DIE-09); a lista de substituição efetiva do paciente também (RN-DIE-03). A publicada anterior do vínculo vira `SUBSTITUIDA` (índice único garante uma `PUBLICADA` por vínculo). O snapshot fica em `dieta_snapshot` (jsonb, imutável por trigger) e o evento interno `DietaPublicada` é emitido para push/.ics (BE-17/BE-24). Vínculo encerrado recusa publicar e duplicar (403, RN-VIN-05).

BE-13: `GET /eu/dietas` (paciente; snapshots de todos os vínculos ATIVO/ENCERRADO, mais recentes primeiro, sem exigir assinatura da nutri — RN-ASS-05) devolve `dietaEmUsoId` (id da **dieta**, não do snapshot) e `versao` da seleção (ETag; 0 enquanto o paciente nunca escolheu). `PUT /eu/dieta-em-uso` `{dietaId}` + If-Match exige uma dieta PUBLICADA de vínculo ATIVO do próprio paciente (senão 404; rascunho e dieta alheia indistinguíveis de inexistente) e responde `{dietaId, versao}`. Dieta em uso efetiva (RN-DIE-07, S9): a escolha enquanto válida; se a nutri publicou uma nova no mesmo vínculo, a sucessora; sem escolha, a publicada mais recente. Perfil NUTRICIONISTA recebe 403 `PERFIL_INVALIDO`. O `pacienteId` do snapshot devolvido é sempre o da conta atual do paciente (o aceite do convite troca o paciente provisório pelo global).

BE-14 (sync de leitura da fase 2): `GET /sync` ganha seis coleções opcionais em `mudancas` (omitidas se o servidor for antigo; sempre presentes, possivelmente vazias, nas novas): `grupos` (nutri; id = id do grupo), `alimentos` (nutri; `AlimentoResposta`, inclui inativos), `listas` (nutri; `ListaSubstituicaoResposta`; **id do envelope = id do vínculo**), `dietas` (nutri; `DietaResposta`, rascunhos e publicadas), `snapshots` (paciente; `DietaSnapshotResposta`) e `dietaEmUso` (paciente; `DietaEmUsoResposta`; id do envelope = id do paciente; tombstone quando não há dieta candidata). Remoção de visibilidade segue a regra da fase 1 (`excluido: true`, `dados: null`). O limite continua valendo para a soma de todas as coleções. A carga inicial de uma nutri nova inclui os 12 grupos e os 482 alimentos copiados do catálogo. Mobile e frontend: campos novos são opcionais, `ignoreUnknownKeys` já cobre clientes antigos.

**Campo novo opcional (A7, 29/10/2026):** `ItemRefeicaoResposta.grupoNome` traz o nome do grupo do alimento (cópia da nutri) e serve para agrupar a lista de compras. Snapshots publicados antes dessa mudança não têm o campo; o cliente trata ausente como "sem grupo". Os exemplos antigos continuam válidos.
