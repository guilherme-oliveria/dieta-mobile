# Sync de leitura — MOB-09

O app usa `GET /api/v1/sync` autenticado como fonte incremental das projeções locais. Contrato oficial BE-08 aprovado: backend `014ef6f93c189fdce3d7f81bbb000b00fb11fd39`. Os 49 exemplos JSON e seu manifesto permanecem idênticos à fonte oficial. Não há mock no runtime de produção.

## Persistência e isolamento

`Sincronizador.puxar()` mantém uma única leitura em andamento. Cursor é uma string opaca: a primeira chamada omite o parâmetro; cada página confirmada grava seu cursor junto das mudanças numa transação SQLDelight. Falha mantém as páginas já confirmadas; repetição/ciclo de cursor com `temMais` falha sem loop. O limite solicitado é 500, contando as três coleções conjuntamente.

`registro_sync` guarda pacientes, vínculos e nutris por conta/perfil, com revisão de projeção separada de `dados.versao` usada para edição clínica. Uma revisão nova pode conter uma versão clínica menor. Tombstones removem a projeção visível; `ENCERRADO` mantém o histórico autorizado. Paciente não aceita projeções privadas da coleção pacientes. Respostas são validadas antes do commit; uma página inválida inteira é revertida.

Login cria uma época de operação, inclusive na reentrada da mesma conta. Logout, mudança de perfil e reentrada invalidam respostas em voo; rotação de refresh preserva a época. As consultas clínicas e as projeções dependem da sessão ativa. Logout limpa projeções, cache, cursor, fila e conflitos conforme a política existente.

Cada instância de `App` possui contexto Koin isolado e seu próprio `ViewModelStore` de autenticação/convites, limpos ao sair da composição. Um segundo shell não reutiliza dependências/ViewModels ligados ao banco/API de outra instância; o teste integrado do shell e a navegação de autenticação exercitam as duas instâncias na mesma suíte. O runtime de dados fornecido por plataforma continua sendo o único dono de API/driver/rede.

A migração 2→3 acrescenta as projeções e o marcador de bootstrap; preserva sessão, cache e fila, e reinicia o cursor para buscar as novas coleções. O bootstrap marca o cache anterior e só remove suas sobras ao concluir a última página. Falha parcial mantém dados anteriores e páginas novas. Criações online ocorridas durante a carga não recebem o marcador antigo.

## Gatilhos e dono dos recursos

`AmbienteDieta` possui API, banco, conectividade e coordenador compartilhados por UI e trabalho de fundo. Isso evita dois clientes disputando a família de refresh. Abertura, retorno ao primeiro plano, reconexão e intervalo de 15 minutos solicitam leitura; requisições simultâneas aguardam a leitura existente. A UI apresenta estado de rede, andamento, erro por código e atualização manual; a lista continua lendo SQLite.

No Android, `ConnectivityManager` acompanha rede validada e o WorkManager agenda trabalho periódico único com rede conectada, backoff e intervalo mínimo de 15 minutos. No desktop, interfaces disponíveis são observadas periodicamente; isso não garante acesso à API, cuja falha continua sendo tratada. No iOS, `NWPathMonitor` informa conectividade e `BGAppRefreshTask` solicita execução de melhor esforço; expiração cancela a operação. O sistema operacional decide quando executar, sem garantia de periodicidade exata.

Referências: [conectividade Android](https://developer.android.com/develop/connectivity/network-ops/reading-network-state), [WorkManager](https://developer.android.com/jetpack/androidx/releases/work), [NWPathMonitor](https://developer.apple.com/documentation/network/nwpathmonitor/), [tarefas de fundo Apple](https://developer.apple.com/documentation/uikit/using-background-tasks-to-update-your-app).

## Verificação e limites

Testes cobrem paginação, retomada, concorrência, cancelamento, isolamento, refresh, rollback, migração, revisão independente, tombstones, histórico encerrado, bootstrap, gatilhos e UI com fonte ampliada. Capturas e contagens finais são registradas no PR.

O ambiente Windows não executa Swift, simulador iOS, `NWPathMonitor` ou `BGTaskScheduler`. Compilação/execução nativa iOS e agendamento real permanecem verificação necessária no Mac. WorkManager e callbacks de rede compilam no Android, mas o agendamento real requer dispositivo/emulador. A situação real do GitHub Actions é registrada separadamente no PR; falha de quota não equivale a teste executado.
