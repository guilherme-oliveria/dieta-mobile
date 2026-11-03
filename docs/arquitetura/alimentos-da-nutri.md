# Alimentos da nutri — MOB-10, recorte independente

O plano de execução permite desenvolver telas da fase contra MockEngine após o contrato BE-C2. Este recorte foi autorizado pelo coordenador após MOB-09 aprovada; a tarefa principal continua pendente de integração BE-10/BE-14 e catálogo BE-09. Produção usa a API configurada; não há catálogo fictício, seed ou motor mock no app.

## Fonte e limites de contrato

Documentos comuns e `contrato-fase-2.md` copiados do backend aprovado `c35541dfbdf5142e2226d572e40adf0bea0c0030`. Os 41 objetos em `shared/src/commonTest/resources/contrato/fase-2/` (39 exemplos, manifesto e 501) são byte a byte iguais aos objetos Git da fonte; os 50 objetos da fase1 ficam intactos. Somente os tipos de grupos/alimentos necessários à MOB-10 foram implementados no cliente; a cópia de outros exemplos não implementa suas funcionalidades.

Sucessos C2 descrevem o contrato futuro, e os endpoints da lista ainda retornam `501 NAO_IMPLEMENTADO` até BE-09/10. Esse código apresenta indisponibilidade, conservando o cache; nunca vira sucesso/lista vazia. O exemplo PATCH oficial contém `ativo=false` na requisição e `ativo=true` na resposta: a cópia foi preservada, a inconsistência comunicada ao backend, e a resposta continua sendo a autoridade. Testes de desativação derivam da fixture oficial alterando somente ativo/versão; não inventam grupos ou valores nutricionais. Macros do catálogo real dependem de decisão/fonte do Guilherme.

## Leitura e escrita

Grupos e alimentos são persistidos no SQLite por usuário/perfil. O refresh lê os grupos e pagina as listas ativas e inativas, com tamanho20, para depois substituir o cache inteiro numa transação. Busca por nome, grupo e ativo filtram essa cópia local completa sem nova requisição. Falha parcial, paginação inconsistente, duplicidade ou resposta de época anterior não substituem o cache. A migração 3→4 cria apenas o cache novo e preserva sessão/fila/projeções/cursor. Logout apaga o cache físico; consultas dependem da sessão nutri ativa.

Criar, editar e desativar exigem rede. IDs UUIDv7 e Idempotency-Key da criação permanecem na retentativa do mesmo formulário; alterações do formulário trocam a chave. PATCH envia todos os campos editáveis e `If-Match` forte com versão clínica do alimento. O cliente só grava resposta confirmada, sem editar snapshots publicados ou o catálogo base. Conflito 409 mantém o formulário e bloqueia novo envio até descarte/atualização explícitos; nenhuma sobrescrita automática. Guardas de geração impedem resposta tardia de fechar outro formulário. Reentrada da mesma conta limpa o estado transitório.

BE-14 ainda não publicou/implementou coleções de alimentos no sync. Este recorte não acrescenta coleções hipotéticas ao aplicador MOB-09. Enquanto isso, atualização da aba é por GET completo/manual; integrar a fonte incremental é gate posterior e explícito.

## UI e verificação

`AlimentosScreen` ainda consta como faltando em `docs/design/telas.md`/DES-04. A implementação segue a descrição e os componentes/tokens existentes: busca, chips de grupo/estado, itens com nome/medida/gramas, formulário rolável e confirmação de desativação explicando que dietas publicadas permanecem intactas. Lista e formulário funcionam com fonte200%; escrita fica desabilitada offline e a leitura mantém o cache.

Testes usam somente contrato oficial e derivações controladas para falhas/paginação/concorrência. Capturas nos três tamanhos, fonte200% e shell completo ficam em `docs/design/evidencias/MOB-10/`; resultado final dos testes/build/CI está no PR. iOS/Swift/dispositivos, leitor de tela e integração HTTP real com backend/DB continuam não executados neste Windows. CI pode estar bloqueado por Billing & plans; sempre conferir o run real.

## Gate para concluir MOB-10

Após BE-09/10/14 aprovadas e mergeadas: sincronizar documentação/fixtures finais, resolver a inconsistência de desativação se ainda existir, verificar catálogo e autorização/limites reais, integrar coleções sync oficiais e testar contra backend implementado. Só então marcar a tarefa principal concluída. Este PR pode ser revisado e integrado como parte independente; não libera dependências que exijam a integração final.
