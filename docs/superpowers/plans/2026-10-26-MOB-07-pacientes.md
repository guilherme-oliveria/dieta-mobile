# MOB-07 — pacientes da nutri

**Objetivo:** lista/busca/filtro, ficha com abas vazias, criação, convite/compartilhamento e aviso de limite.

**Fontes:** AGENTS.md, plano-de-acao.md MOB-07, contratos oficiais BE-06/BE-07 aprovados, N-Pacientes/N-Paciente e componentes/telas/validacao. BE-07 foi mergeada durante a execução (develop `2d9e231`); documentos sincronizados, 50 objetos de exemplos/manifesto preservados. MockEngine somente nos testes, sem dados falsos na aplicação.

**Arquitetura:** novo cache SQLDelight versionado e filtrado por sessão, `PacientesRepositorio` para REST/cache, `PacientesViewModel` para interação e telas sem estado. Refresh carrega páginas reais, publica cache numa transação após verificar a mesma sessão. Logout/troca de usuário apaga cache clínico. Criação e convite são online nesta fase; escrita offline da nutri permanece na MOB-29. Android/iOS abrem share nativo; desktop usa área de transferência do sistema e informa esse resultado, pois não há share picker Java portátil.

**Design:** manter shell e cinco abas atuais, sem reproduzir navegação antiga de quatro abas dos artboards. Lista mostra status reais; não inventar filtros de atenção/retorno nem adesão ainda ausente. Ficha mostra dados disponíveis e abas Resumo/Avaliações/Dieta/Anamnese; métricas futuras ficam vazias. DES-02 não publicou folha convite/limite/lista-detalhe: seguir componentes e registrar limitação.

## Etapas

- [x] Testes RED de repositório/cache: isolamento, logout, resposta atrasada, paginação completa, falha parcial preserva cache, criação/idempotência, convite oficial e ausência de payload artificial.
- [x] Migração nova/cache e limpeza de sessão; repositório REST/Flow local e DI.
- [x] Testes RED de ViewModel: busca/status offline, criação/validação/retentativa, seleção/cancelamento, limite/plano, erro por código, convite/compartilhamento explícito.
- [x] Telas lista/ficha/formulário/convite/limite e compartilhamento de plataforma; integração com grafo existente.
- [x] UI compacta/média/expandida e fonte 200%, 16 capturas, preview, testes/build locais (104 JVM/24 Android host, migração/metadados/APK/JAR).
- [ ] CI e revisão independente no PR develop; iOS nativo depende de Mac.

## Foco da revisão

- Nenhum dado clínico de sessão anterior pode reaparecer por resposta REST ou Flow atrasado.
- Refresh parcial/queda de rede não elimina cache válido; telas continuam lendo SQLite.
- Criar/convidar repetidamente mantém a mesma chave; erro de limite não cadastra item local fictício.
- Ficha encerrada é só leitura e convite indisponível. A ficha usa o DTO completo do cache; refresh completo reconcilia os vínculos visíveis, sem GET individual nem seletor não contratado.
- Link/código só são compartilhados por ação explícita, sem dado clínico no texto e sem logs.
