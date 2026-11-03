# MOB-09 — sync de leitura

**Objetivo:** `Sincronizador.puxar()`, cursor/paginação, aplicação local transacional, conectividade, gatilhos e indicador offline; WorkManager Android conforme plano.

**Base:** develop mobile `b9d301d` após MOB-08 aprovada. Somente BE-C1 oficial (`f3aef14`, 49 JSONs + manifesto preservados) enquanto BE-08 está em implementação. O plano de execução autoriza MOB-09 com mock oficial na onda 4; integração real somente depois de BE-08 aprovada/mergeada. Nenhum contrato WIP será copiado ou tratado como oficial.

**Gate recebido:** BE-08 aprovada/mergeada em `014ef6f93c189fdce3d7f81bbb000b00fb11fd39`, 26/10 21:30:40 UTC. Prosseguir integração real com esse contrato final. Limite global 1..1000/default500; projeções atuais por conta/perfil; revisão do envelope independente de dados.versao; ENCERRADO preservado. Postgres/triggers não executados no CI por quota, exceção registrada pelo coordenador.

**Decisões de implementação:** runtime de dados único por processo evita disputa de refresh entre UI e WorkManager. Cache guarda revisão da projeção separada e aplica cada página+cursor numa transação. Pacientes passa a usar sync como refresh; bootstrap preserva cache anterior durante falha e reconcilia somente ao concluir, sem apagar criações online realizadas durante a carga. Aplicador confirma identidade/época da sessão dentro da transação. Login da mesma conta cria nova época; rotação de refresh não troca a época. Runtime possui API/driver/observador de rede e os fecha segundo o dono da plataforma.

## Etapa independente agora

- [x] RED orquestrador: cursor inicial ausente, percorre `temMais`, preserva cursor string sem conversão numérica, persiste por página concluída, retomada após falha parcial, não sobrepõe pulls concorrentes, cancela quando muda a sessão, detecta cursor repetido sem progresso e preserva cancelamento.
- [x] `Sincronizador` com portas `FonteLeituraSync` e `ArmazemLeituraSync`. O armazenamento recebe contexto opaco de operação e deve aplicar página+cursor atomicamente. Testes usam DTOs/exemplos oficiais e portas de teste; sem mock em produção/DI.
- [x] Contrato de contexto distingue a mesma conta/perfil em entradas diferentes; login/logout invalida operação anterior. Não comparar nem supor igualdade entre envelope.versao e dados.versao.
- [x] Verificar testes do núcleo e registrar checkpoint; tarefa continua desmarcada/incompleta até integração e gates.

## Após BE-08 oficial

- [x] Ler contrato final, sincronizar documentos aprovados e conferir os 50 objetos de exemplos/manifesto.
- [x] Implementar adaptadores HTTP e SQLite reais e testes transacionais/sessão/migração. Não apagar histórico apenas por status ENCERRADO; exclusão somente pelo tombstone autorizado pelo servidor.
- [x] Resolver fonte única do cache de pacientes e convivência com refresh MOB-07 segundo as projeções finais, sem corrida entre duas cargas substituindo dados.
- [x] `Conectividade` expect/actual, abrir/resume/reconexão/15 min e WorkManager Android sem criar clientes concorrentes que disputem refresh tokens. Indicador offline usa componentes existentes.
- [x] Testes de integração mock oficial, builds locais/CI real e revisão independente no único PR MOB-09 para develop.

## Foco da revisão

- Cursor é opaco; não converter bigint para Long/Double nem derivar o próximo valor.
- Falha da página não avança cursor nem desfaz páginas já confirmadas; retentativa parte do último commit local.
- Página e cursor são uma transação; logout/troca/reentrada da mesma conta invalidam respostas em voo.
- Sem loop infinito se temMais=true e cursor não avança; sem duas execuções simultâneas.
- Sem inferir exclusão por ENCERRADO, nem usar revisão de projeção como versão de edição clínica.

## Evidência final local

161 testes JVM e 55 Android host sem falhas/erros; migração, metadados comuns, APK debug e JAR desktop verdes. Cinco capturas em `docs/design/evidencias/MOB-09/`. Swift/iOS e gatilhos nativos em dispositivo não executados no Windows; resultado real de CI será registrado no PR. Revisão independente é gate posterior à abertura do PR, não execução do implementador.
