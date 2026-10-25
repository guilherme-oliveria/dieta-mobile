# Banco local e fila — MOB-04

Base SQLDelight em `shared/src/commonMain/sqldelight/org/br/dietamais/data/local/`. Não contém entidades clínicas futuras, cliente HTTP, envio da fila ou regras de permissão/plano. É infraestrutura para os repositórios offline-first das próximas tarefas.

| Tabela | Conteúdo |
| --- | --- |
| `sessao` | Um registro de metadados da sessão ativa: `usuario_id` e `perfil`, vindos da autenticação. Não armazena credenciais ou tokens. |
| `cursor_sync` | Cursor opaco em texto, separado por usuário/perfil. Ausência de cursor representa carga inicial. |
| `mutacao_pendente` | `id_mutacao`, entidade, operação, ID do registro, versão base opcional, JSON, tentativas e status. Usuário/perfil são metadados locais de isolamento, derivados da sessão, sem acrescentar campos ao corpo da API. |
| `conflito` | ID da mutação, contexto usuário/perfil e o `atual` completo do servidor em JSON, sem perder campos desconhecidos. |

Operações usam os valores definidos em `integracao.md`: CRIAR, ATUALIZAR, EXCLUIR. O estado inicial interno da fila é PENDENTE, com zero tentativas. Atualização de estados, repetição e resolução de conflitos pertencem a MOB-16/MOB-30. Nenhum registro é enviado nesta entrega.

`FilaMutacoes.gravarEEnfileirar` lê a sessão e executa o callback de escrita com o mesmo `BancoDieta`/driver/transação do enfileiramento. Uma exceção em qualquer etapa desfaz as duas partes. Sem sessão, a escrita é recusada antes do callback. `pendentes()` também lê sessão e fila em uma transação, filtrando pelo contexto; mudar usuário ou perfil não expõe a fila anterior. O chamador deve usar as queries desse banco no callback e executar I/O fora da thread de UI.

IDs novos são gerados por `novoIdLocal()`, que usa `Uuid.generateV7()` do Kotlin 2.4. A fila gera uma chave de mutação uma única vez e a preserva para futuras tentativas; IDs do registro são gerados pelo repositório ao criar a entidade. Os dados são `JsonObject` opaco: não há DTO ou schema de endpoint inventado. Dois exemplos oficiais BE-C1 em `commonTest/resources/contrato/` provam a preservação de payload/conflito nos testes de armazenamento; não há MockEngine ou chamada de API aqui.

## Drivers e ciclo de vida

- Android: `AndroidSqliteDriver` com `applicationContext` e schema gerado.
- iOS: `NativeSqliteDriver` com o schema gerado.
- JVM: `JdbcSqliteDriver` em `~/.dietamais/dietamais.db`; diretório pode ser passado à fábrica. Inicialização/migração e `user_version` são atômicos, reabrir preserva dados e uma versão mais nova é recusada sem apagar o arquivo.

`moduloBancoLocal` disponibiliza `BancoLocal`, `BancoDieta` e `FilaMutacoes` como instâncias únicas. Os três pontos de entrada fornecem a fábrica ao `App`; o banco só abre na primeira resolução e o driver fecha ao encerrar o módulo. Previews podem omitir a fábrica. MOB-05 conectará sessão/autenticação e limpeza no logout; tokens ficam exclusivamente em `ArmazenamentoSeguro`. As futuras tabelas clínicas também deverão participar dessa limpeza.

## Schema e migrações

Schema inicial: versão 1. O snapshot `schemas/1.db` contém somente as quatro estruturas vazias e é validado por teste para impedir dados de aplicação no Git. Bancos de uso, journals/WAL e arquivos SQLite são ignorados; a única exceção é o diretório de snapshots do schema.

Antes de alterar uma estrutura publicada, atualize os `.sq` para a estrutura atual e acrescente a migração numerada (`1.sqm` leva de 1 a 2). Preserve migrações e o snapshot inicial publicados. `verifySqlDelightMigration` aplica as migrações ao snapshot e compara o resultado com o schema atual. O snapshot inicial foi produzido por `:shared:generateCommonMainBancoDietaSchema`, conforme o mecanismo do SQLDelight; não é necessário gerar outro snapshot a cada mudança.

## Verificação

`./gradlew :shared:jvmTest :shared:testAndroidHostTest :shared:verifySqlDelightMigration`. Os testes JVM usam SQLite real em memória e arquivos temporários em `build/test-db`: commit/rollback nos dois sentidos, JSON oficial, UUIDv7, contexto usuário/perfil, cursor sem perda de precisão, conflitos, reabertura, módulo Koin, recusa de downgrade e snapshot vazio. Não há testes de domínio/ViewModel porque essa tarefa não cria essas camadas.

Drivers Android/iOS são compilados pelos alvos; a lógica transacional comum foi executada no JVM. Execução nativa iOS precisa de macOS e não foi feita no Windows. Não há novas telas ou mudanças visuais; os layouts aprovados de MOB-03 permanecem.

No Windows desta execução, o worker do SQLDelight precisou de `java.io.tmpdir` junto de `jdk.net.unixdomain.tmpdir`, ambos apontando para `.gradle/socket-tmp`, em `JAVA_TOOL_OPTIONS` somente no processo, com `--no-daemon`. Nenhuma configuração global de Java/SDK foi alterada.
