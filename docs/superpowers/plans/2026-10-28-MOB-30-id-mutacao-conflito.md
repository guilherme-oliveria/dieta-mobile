# MOB-30 — nova identidade ao manter a minha versão

Branch bugfix da develop fixa `328f97b7aa30fb43db60806e02742df947691de0`, por instrução do coordenador. Editor de macros preservado em `feature/MOB-10-integracao-macros`/`1ad8773`, fora deste PR. Nenhuma aprovação dos 18 commits externos é inferida.

O servidor guarda o resultado por idMutacao. Alterar versaoBase mantendo o mesmo ID apenas reproduz CONFLITO. A escolha explícita “manter a minha” é uma nova tentativa lógica: troca por UUIDv7 novo e usa atual.versao como base, na mesma transação local; o replay da tentativa antiga continua imutável no servidor. Preservar dados/entidade/operação/registro, snapshot anterior e todas as outras pendências. Não corrigir neste PR as lacunas de projeção/cursores ou editor.

- [x] RED: SQLite real + FonteEnvioHttp/MockEngine com cache por idMutacao; ID antigo reproduz conflito mesmo com nova base, ID novo aplica uma vez e seu replay não duplica efeito. Outras pendências/escopos permanecem intactos e clique repetido no ID antigo não cria outra tentativa.
- [x] RED: falha após troca de identidade reverte fila/conflito/projeção, sem callback; versões ausentes/inválidas e sessão alheia não reabrem.
- [x] Implementar troca de ID no UPDATE da fila e gerar novoIdLocal em manterMinha, com guarda de versão; nenhuma alteração de schema/migração/cache backend.
- [x] Testes focados, suíte/build/migração na base atual; documentar limites reais e PR develop, QA independente.

## Verificação

RED real: dois dos três testes novos falharam por reutilização da identidade e aceitação de versão negativa; rollback já se mantinha íntegro. Após a correção, focados de fila/offline e regressões verdes. Suíte atual: 388 JVM/217 Android host, zero falhas/erros; migração/metadados/APK debug/JAR desktop verdes em 3m40s/94 tarefas (76 executadas), .gradle/mob30-id-final.log. iOS/servidor/Postgres reais não executados; CI real será registrado no PR, com revisão independente posterior.
