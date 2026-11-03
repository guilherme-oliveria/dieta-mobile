# MOB-59 — curva de crescimento

Branch `feature/MOB-59-curva-crescimento`, criada de origin/develop após QA aprovar e mesclar mobile #68 (MOB-58), cabeça bc2d858a2ac1c5f2ba96e2a26062abca73d5154e, com Android/desktop, web, iOS e E2E verdes. Nenhum PR aberto nos dois repositórios ao iniciar.

## Escopo e decisões

- Curva para nutri e responsável, artboard aprovado `P-Curva-Crescimento`, sem alterações de plataforma, `.github`, tarefas excluídas ou classificação pediátrica local.
- Usar avaliações já lidas do banco local/porta web e seus snapshots BE-55b: idade, sexo, medidas e cortes congelados. A leitura não muda o histórico ao atualizar ficha/catálogo. A API `/referencias/oms` retorna apenas uma linha por consulta e não identifica versão; não substituir silenciosamente os cortes históricos por catálogo atual.
- Mostrar Peso, Estatura e IMC por idade; adolescente só tem IMC. Sem snapshot/referência, indicar ausência em vez de usar IMC adulto ou inventar dados. Pontos são medidas confirmadas na idade real da avaliação; a referência anual adolescente conserva a idade da linha, que não é a idade do ponto.
- Não juntar referências de sexos, fontes ou versões diferentes. Selecionar avaliação e indicador, mostrar os cortes disponíveis no grupo compatível, origem/versão e limites da série; sem fabricar meses intermediários ou percentil 62 do artboard. Texto em palavras usa somente intervalo/classificação confirmados.
- Paleta e desenho: P50 verde contínuo, demais cortes cinza tracejado, medida azul com borda branca, eixos/unidades/legenda e alternativa em tabela. Rótulos devem refletir os percentis realmente publicados, sem supor que índices são P3/P15/P85/P97.
- Integrar à leitura da avaliação da nutri e à Evolução do paciente, respeitando os registros compartilhados que o servidor entrega e separando vínculos/pacientes. Não criar cadastro de responsável.
- Adaptar os componentes existentes em Compose Canvas; sem biblioteca nova. Fonte 200% e três larguras com rolagem e controles acessíveis. Documentar divergências do artboard e cobertura limitada aos snapshots disponíveis.

## Plano de execução

1. Domínio puro com testes RED/GREEN: seleção de indicadores/avaliações, agrupamento por sexo/fonte/versão, ordenação/duplicatas, idade real versus referência anual, cortes ausentes/inconsistentes e isolamento de pacientes. Nenhuma classificação nova.
2. Componente de curva: preservar chamadas existentes, acrescentar pontos históricos/seleção sem falsa classificação calculada pelo gráfico; paleta, eixos, tabela e semântica. Testes de UI e casos vazios/parciais.
3. Tela e integração nas rotas existentes da nutri e paciente. Leitura offline dos snapshots, sem pedidos clínicos novos. Testes de compartilhamento/estado e navegação.
4. Capturas sintéticas em 390/820/1180 e fonte 200%, conferidas visualmente. JVM completo e compilação web/desktop; Android/iOS pelo CI da cabeça publicada.
5. Atualizar CHANGELOG, plano técnico/UI e divergências; commit/PR develop e `QA: PRONTO PARA REVISAR` após checks verdes. QA independente aprova e mescla; DEV nunca revisa/mescla o próprio PR.

## Progresso

- Lidos artboard, padrões de gráficos, componente atual, rotas de avaliações/Evolução, DTOs BE-55b e controller de referências. O gráfico existente só aceita um ponto, usa cores e inferência de posição inadequadas; precisa ser adaptado antes de entrar em tela de produto.
- Quatro testes de domínio escritos: idade real versus referência anual, isolamento por paciente/vínculo/sexo/versão, cortes inválidos/ausentes e duplicatas de mês com cortes divergentes. RED observado por `crescimento` ausente em `shared/build/mob-59-dominio-red.log` (57s).
- Implementada projeção pura `Crescimento.kt`, sem classificação local nem catálogo atual: pontos confirmados, referências congeladas validadas e grupos compatíveis. Conferido no backend que são oito cortes (P0,1/P3/P10/P15/P50/P85/P97/P99), corrigida a fixture inicial de nove valores antes do GREEN.
- GREEN dirigido em execução: `shared/build/mob-59-dominio-green.log`, sessão 46171. Não iniciar outra compilação nem alterar fontes durante o run. Depois integrar gráfico/tabela e UI nas leituras da nutri/paciente, com testes antes da implementação. Nenhum commit/PR de MOB-59 ainda.
- GREEN do domínio passou em 4m51s. Dois testes de UI acrescentados para intervalo/tabela sem classificação adulta e ausência de snapshot. RED de UI em execução em `shared/build/mob-59-ui-red.log`; ler resultado antes de implementar `CrescimentoTela`. Sem PR aberto nos dois repositórios em 03/11 às 11:22 UTC.
- RED da UI observado por `CrescimentoTela` ausente (32s). Implementados tela com escolha de indicador/data, gráfico de medidas/cortes congelados, tabela, fonte/versão e intervalo confirmado. Primeiro GREEN executou seis testes, com uma falha exclusivamente no seletor ambíguo de texto `18 meses`; corrigido para correspondência exata. Os outros cinco passaram.
- Acesso integrado ao histórico da nutri/paciente e aos resultados do paciente. Acrescentado teste de abertura/volta pelo histórico offline. Run de integração em `shared/build/mob-59-integracao.log`, sessão 46571. Após ler resultado, gerar/conferir capturas e rodar JVM completo/web/desktop. Ainda sem commit/PR.
- Integração passou (4m26s), seguida de suíte completa e compilações web/desktop bem-sucedidas em `shared/build/mob-59-final.log`. Capturas do gráfico/tabela/intervalo geradas em três larguras e fonte 200%. Primeira conferência visual levou a duas melhorias: tabela com um corte por linha e linhas de referência apenas entre meses consecutivos publicados, mantendo lacunas vazias. Nova suíte completa e compilações em execução em `shared/build/mob-59-visual-final.log`, sessão 73574; aguardar e reconferir capturas antes de commit/PR.
- Revalidação final passou em 5m46s: **619 testes, zero falhas/erros/ignorados**, compilações JVM/web/desktop. Capturas regeneradas conferidas; tabela por corte mantém leitura e rolagem com fonte 200%, sem concatenar todos os cortes numa célula. Plano técnico/UI, mapa e CHANGELOG atualizados. Próximo: PR develop e CI/QA independente; Android/iOS não executados localmente.
- Conferência adicional da captura com fonte 200% revelou corte no texto longo do botão da data. Separada a data em texto e o botão curto `Escolher avaliação`. Nova validação completa em `shared/build/mob-59-data-final.log`, sessão 79166; conferir a captura 390/fonte 200% antes do commit. Corpo do PR preparado em `shared/build/mob-59-pr.md` (ignorado).
- Última revalidação concluída com sucesso: 619 testes sem falhas/erros/ignorados e web/desktop compilados. Captura regenerada de fonte 200% confirma a data separada e botão legível. Evidência final em `shared/build/mob-59-data-final.log`.
