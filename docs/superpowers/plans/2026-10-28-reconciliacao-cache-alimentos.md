# Reconciliação do cache de alimentos — MOB-09 / interação MOB-29

Base develop após PR17 `78715d5`. Branch/PR exclusivos da reconciliação; editor `1ad8773` preservado e cadeia de várias edições após manterMinha é backlog separado. Os contratos BE-14 remotos continuam em auditoria; testes usam DTOs/exemplos fase2 oficiais existentes, envolvidos no envelope de sync já implementado, sem criar fixture nutricional nova.

RED deve demonstrar: pull guarda grupos/alimentos mas não atualiza a tabela observada pela aba; uma edição local faz o pull descartar dados remotos e avançar cursor, e rejeição posterior perde atualização remota já consumida. A correção precisa manter base remota durável e projeção otimista separadas, por conta/perfil/registro, projetar cache na mesma transação do cursor e reaplicar apenas mutações abertas do mesmo alimento. Não reordenar/regravar fila ou misturar editor.

- [x] HTTP/SQLite: pull de grupos/alimentos atualiza AlimentosRepositorio.observar, atualização/desativação e tombstone; versão do envelope separada da entidade e revisão antiga ignorada.
- [x] Mesmo registro com duas edições locais: pull novo mantém a última intenção, guarda base remota; rejeição parcial mantém a outra pendência e rejeição final restaura atualização remota sem novo pull.
- [x] Tombstone com pendência, rollback de página inválida/cursor e isolamento; falha não consome base remota ou perde fila.
- [x] Implementar reconciliação incremental sem migração, mantendo demais coleções e contratos inalterados. Auditar interação com refresh GET completo e resolução explícita de pendências.
- [x] Testes focados/suíte/build na base atual e documentação de limites: 411 JVM/231 Android host sem falhas, migração/APK/JAR verdes. Oito regressões, seis RED confirmadas. PR develop preparado para QA independente; o veredito continua pendente. Cadeia/ordem UUID após manterMinha continua sem validação integral neste PR.
