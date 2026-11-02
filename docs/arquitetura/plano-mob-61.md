# MOB-61 — Anamnese estruturada

Dependências aprovadas e mescladas pelo QA: backend #73 e espelho mobile #65. Artboards A-Anamnese-Estruturada e P-Anamnese.

- [x] DTOs opcionais de código e resultados, compatíveis com JSON antigo e valores futuros.
- [x] Preservar códigos no editor dos modelos, SQLDelight e sync; não oferecer edição técnica de código ao usuário.
- [x] Resultados sempre do servidor, com PA, álcool, sal, óleo e duração semanal. Não calcular conduta ou classificar sal no cliente.
- [x] Ao digitar/gravar/mesclar resposta local, invalidar derivados anteriores; mostrar pendência até a confirmação do servidor. Web incorpora a resposta do PUT e protege edições em voo.
- [x] Campos conhecidos em cartões por assunto, medidas de bebida em seletor compacto, pares de campos adaptativos; genéricos e códigos futuros continuam no formulário existente.
- [x] Paciente: parcial offline, salvamento de digitação válida, conclusão online e feedback de falha. Nutri: leitura dos derivados e respostas, mantendo o backend como autoridade de quem responde.
- [x] Testes de contrato/editor, validação e estado, SQLite/sync, MockEngine com resposta em voo, UI e evidências em 390, 820 e 1180 px, fonte 200%.
- [ ] Testes JVM, docs, tarefa marcada e PR para develop; QA independente antes da próxima tarefa.

Dados da fonte: 20 grupos e sete frequências, oito bebidas e horários nos sete dias, conforme contrato-anamnese.md. Divergências deliberadas do artboard: volumes e truncamentos do contrato BE-57a, resultados após confirmação do servidor (sem cálculo local), nutri lê as respostas porque o contrato permite somente ao paciente respondê-las. Nenhum dado real nos exemplos.

Apontamento documental não bloqueante do QA #73: corrigir a posição do parágrafo de anamnese após a tabela de módulos na próxima alteração oficial do backend e espelhar. Não divergir a cópia mobile isoladamente neste PR.

Integração: respostas parciais válidas são salvas com debounce de 400 ms (escolhas, imediatamente), e o repositório nativo solicita envio da fila após a transação. “Atualizar resultados” confirma a fila e reconsulta o servidor sem concluir o questionário. A consulta mantém registros com mutações abertas. Na web, o PUT atualiza os derivados, preservando novas edições enquanto a resposta estava em voo; atualizar permite repetir o envio após falha. Falha transitória não desabilita definitivamente as ações quando o observador de conectividade permanece online.

Validação final: 588 testes JVM em 113 classes, zero falhas/erros; 16 capturas sintéticas conferidas. Log `build/mob-61-validacao-consolidada.log`. Aguardar CI na cabeça enviada e QA independente.
