# Reconciliação de alimentos

Correção MOB-09 sobre develop `78715d5`, separada do editor de macros `1ad8773` e da auditoria integral dos commits externos. Não altera o contrato da API, o schema ou migrações publicadas.

Publicação atualizada incrementalmente sobre develop `7785b63`, sem conflitos. Verificação local: 411 JVM e 231 Android host, zero falhas/erros; migração SQLDelight, APK debug e JAR desktop verdes em 2m41s/94 tarefas, log `.gradle/reconciliacao-atual-estavel.log`. Oito regressões novas, seis delas confirmadas RED antes da implementação. SHA da árvore conferido antes/depois do build; a tentativa anterior interrompida por checkout externo foi descartada como evidência. Sem mudança visual.

`registro_sync` mantém a base conhecida do servidor na coleção interna `base-remota/alimentos`, isolada por usuário/perfil/id. `alimentos` guarda a projeção lida pela aba através de `alimento_cache`. A revisão da base é a do envelope do feed; `dados.versao` continua sendo a versão da entidade usada como base das mutações. Nunca são comparadas entre si.

Ao consumir uma página, o app conserva a base mesmo com edições abertas, sobrepõe os campos editáveis das mutações PENDENTE/CONFLITO na ordem já existente da fila e projeta o cache na mesma transação do cursor. A data otimista é preservada enquanto há edições locais. Um tombstone remove a visibilidade, mas conserva a fila para resolução. Página inválida reverte base, projeção e cursor.

Uma confirmação APLICADA incorpora os campos enviados e a versão confirmada na base conhecida, sem substituir uma versão remota mais nova. Uma rejeição deixa de participar da projeção; as demais edições abertas continuam visíveis. Quando a última é resolvida, a base remota reaparece sem outro pull. Rejeições continuam disponíveis na lista de pendências. O refresh GET completo também conserva a base e reaplica a fila; não inventa revisões de feed ou tombstones para ausências em páginas GET.

O código preserva as revisões e a projeção existentes ao iniciar novas edições. Dados remotos já descartados por uma versão anterior, depois de seu cursor ter avançado, não podem ser reconstruídos localmente: o refresh completo do catálogo permite recuperar a base pela API. Este recorte não reinicia cursores opacos nem declara recuperação retroativa sem nova leitura.

Os testes usam SQLite real em memória e ApiDieta/FonteLeituraHttp/FonteEnvioHttp com MockEngine, DTOs e exemplos oficiais fase 2 existentes. Valores derivados apenas variam versão, ativo, ids de isolamento e data para exercitar ordenação/rollback; nenhum catálogo nutricional novo é criado. Servidor/Postgres reais e execução iOS não são evidenciados por esses testes.

Permanece separada a cadeia de múltiplas edições do mesmo registro quando `manterMinha` gera outro UUID e muda sua posição na fila. A reconciliação não reordena, renumera ou reenvia mutações por conta própria e não declara essa cadeia integralmente validada. Outras coleções mantêm seu comportamento anterior.
