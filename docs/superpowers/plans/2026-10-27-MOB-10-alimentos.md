# MOB-10 — alimentos da nutri, partes independentes

**Objetivo:** lista por grupo/busca e formulário criar/editar/desativar contra contrato BE-C2 aprovado `c35541dfbdf5142e2226d572e40adf0bea0c0030`. Autorização do coordenador permite MockEngine em testes e PR revisável antes dos endpoints reais. Não declarar integração concluída: BE-09/macros, BE-10 e BE-14 continuam pendentes.

**Arquitetura:** DTOs tipados, repositório HTTP com cache SQLite por conta/perfil, ViewModel e telas comuns. A UI observa o banco; refresh completo online aplica grupos/lista somente após todas as páginas válidas, com guarda da época da sessão. Criação/edição/desativação exigem rede, Idempotency-Key/If-Match e atualizam cache após sucesso; sem fila offline nesta fase. Não acrescentar coleções sync hipotéticas antes do contrato BE-14 implementado.

**Design:** `AlimentosScreen` segue faltando em `docs/design/telas.md`; usar descrição/componente `ItemListaAlimento`, campos/botões/estados/tokens existentes. Formulário responsivo rolável, confirmação explícita de desativação preservando snapshots. Nenhum grupo/macronutriente inventado; exemplos fictícios oficiais apenas em testes.

- [x] Copiar docs/JSONs C2 por objeto Git do commit aprovado, registrar origem e preservar fase1.
- [x] RED contrato/HTTP/cache: tipos oficiais, parâmetros pagina/tamanho/grupo/busca/ativo, Bearer, PATCH If-Match, criação idempotente, 501 como indisponibilidade, falha parcial sem substituir cache, logout/reentrada sem repopular.
- [x] Implementar DTOs/repositório e nova migração 3→4 preservando snapshots publicados; nenhuma alteração no contrato backend.
- [x] RED ViewModel: filtros/busca, validação de feedback, envio online, chave estável na retentativa, conflito sem sobrescrita automática, sessão/rede invalidam resultado/ações.
- [x] Implementar lista/formulário, DI e aba Alimentos; testes/capturas três tamanhos e fonte200%, ações offline desabilitadas.
- [ ] Verificar suíte/migração/metadados/APK/JAR, PR develop com recorte/gates/CI real explícitos e QA: PRONTO PARA REVISAR.

**Gate final pendente:** integrar backend BE-10 real e fontes BE-14 aprovadas quando chegarem; validar catálogo/macros BE-09 com fonte definida pelo Guilherme. A tarefa MOB-10 principal permanece incompleta até essa integração; este PR entrega somente o recorte independente autorizado.

## Verificação local

183 testes JVM e 64 Android host sem falhas/erros; migração 3→4, metadados comuns, APK debug e JAR desktop verdes em 1m37s/94 tarefas (`.gradle/mob10-final.log`). Nove capturas verificam três tamanhos, fonte em 200% e shell real. Snapshots1/2/3 e exemplos fase1 intactos. iOS/Swift/dispositivo e integração HTTP real não executados; CI real e revisão independente serão registrados no PR.
