# Manter a minha versão — identidade da nova tentativa

O resultado de `POST /sync` é idempotente por `idMutacao`: reenviar a identidade antiga reproduz inclusive seu `CONFLITO`, mesmo que o cliente altere `versaoBase`. A escolha “manter a minha” em MOB-30 gera um UUIDv7 novo e usa `atual.versao` como base. O cache de resultados do servidor não é invalidado nem alterado.

A fila troca identidade, versão base e estado numa transação local. Entidade, operação, ID do registro e dados da edição permanecem iguais; as tentativas da nova mutação começam em zero. O conflito antigo sai da lista local, a projeção otimista passa à versão seguinte e o snapshot anterior permanece até o fluxo normal concluir a fila. Outras pendências, IDs e escopos não são reescritos. Repetir o clique com a identidade antiga já consumida não cria outra tentativa.

Versão ausente, não numérica, negativa ou sem sucessora representável mantém o conflito. Falha durante a transação restaura identidade/base/conflito/projeção e não dispara o callback de sync. Apenas o commit bem-sucedido solicita envio.

`ManterMinhaIdempotenciaTest` usa SQLite real, ApiDieta/FonteEnvioHttp e MockEngine com cache por identidade: reproduz o conflito com o ID antigo, aplica o novo ID uma vez e confirma que replays preservam os resultados. Também verifica outra pendência/conta e rollback por falha SQL controlada.

O bugfix parte de develop `328f97b`; não modifica o backend, o contrato de sync ou schema/migrações. A projeção de alimentos do pull e a preservação de atualizações remotas enquanto há pendências são apontamentos separados da auditoria; este ajuste de identidade não os resolve. Resultados de testes/build e limites de CI/iOS/DB ficam no PR.
