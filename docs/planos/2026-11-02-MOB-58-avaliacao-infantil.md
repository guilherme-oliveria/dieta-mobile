# MOB-58 — avaliação infantil e adolescente

Branch `feature/MOB-58-avaliacao-infantil`, criada de origin/develop após QA aprovar e mesclar backend #74 e espelho mobile #67. Os checks das duas cabeças estavam verdes. MOB-59 continua separado e pendente.

## Escopo autorizado

Artboard `A-Avaliacao-Infantil`, contrato aprovado em `docs/arquitetura/contrato-avaliacao-infantil.md` e seção 3.3 do plano de planilhas. Entrada por faixa etária na avaliação da nutri, edição e armazenamento offline-first, resultados pediátricos do servidor e compartilhamento existente. Não criar curvas históricas de MOB-59 neste PR, nem editar .github/plataformas ou iniciar tarefas excluídas.

## Decisões de contrato e apresentação

- Backend é a fonte dos derivados; nenhuma classificação pediátrica usa o campo legado de IMC adulto.
- Campos `infantil` e `resultadosInfantis` são opcionais; strings/enums desconhecidos e campos novos não quebram o app. Motivos e resultados ausentes não viram zero.
- O artboard usa comprimento em cm e peso ao nascer em kg. Converter exatamente para estatura em m (três casas) e inteiro em gramas, com feedback de precisão e limite; braço e dobras mantêm cm/mm.
- Divergência necessária do artboard: ele mostra percentil/Z exatos ao vivo, mas a fonte aprovada só permite intervalos. Mostrar intervalos confirmados pelo servidor e estado pendente após alteração local; não interpolar nem manter derivados antigos como atuais.
- G-16 continua explícita, sem corrigir referência no cliente. Ausência de sexo, referência ou etnia quando exigida deve ser legível.
- Preservar dados novos na edição e na fila de sync; escrita local e mutação na mesma transação. Retorno do servidor não deve sobrescrever edição pendente.
- Usar campos/opções adequados à idade da ficha e data da avaliação. Manter o fluxo adulto existente.

## Passos e verificações

1. Testes de serialização: resposta antiga/nova, opcionais, valores desconhecidos, payload sem derivados. Implementar DTOs aditivos.
2. Testes de domínio/formulário e ViewModel: unidades, idade completa, faixa, limites pediátricos, validação, edição, estado pendente e entradas preservadas. Integrar os campos à requisição e aos repositórios nativo/web.
3. Testes de fila/local: criação e edição offline guardam entradas, invalidam resultados atuais e recebem o snapshot pelo sync sem perder alterações locais.
4. Formulário e leitura dos resultados pediátricos com strings em resources; esconder protocolos adultos inadequados. Layout nos três tamanhos e fonte 200%, mantendo controles acessíveis.
5. JVM completo, compilações pertinentes e capturas sintéticas. Atualizar CHANGELOG, plano técnico/UI e divergências do artboard. Separar MOB-58 de MOB-59 no plano sem marcar a curva como concluída.
6. Commit/PR develop, CI da cabeça atual, QA independente. Não fazer review/merge do próprio PR.

## Progresso

- Lidos AGENTS, DTOs, repositório nativo, estado do formulário, handoff e artboard local aprovado.
- Armazenamento usa registro JSON no SQLDelight, portanto conferir se basta DTO aditivo antes de inventar migração.
- O repositório nativo hoje guarda apenas o IMC otimista na criação e mantém derivados antigos na edição; será necessário tratar os resultados pediátricos pendentes.
- Lidos também domínio/validação, ViewModel, prévia adulta, estrutura da tela e repositório online. Validação ainda só aceita adultos; edição formata estatura em duas casas; prévia usa anos por 365,25 dias. Essas ligações precisam ser ajustadas para o contrato pediátrico, preservando adultos.
- Passo 1: DTOs opcionais e três testes de contrato (resposta antiga, snapshot/intervalos/valor desconhecido e requisição sem derivados). RED observado em `shared/build/mob-58-dto-red.log` por campos/classes ausentes (56s); GREEN passou em `shared/build/mob-58-dto-green.log` (4m56s).
- Passo 2 iniciado: `FormularioInfantil` e auxiliares de idade/unidades/limites. RED observado em `shared/build/mob-58-formulario-red.log` (33s). Implementados meses completos em São Paulo, peso ao nascer kg→gramas, fator opcional e validação pediátrica, com comprimento/estatura em cm (uma casa) no formulário, convertido para m (três casas) ao montar payload. Adultos mantêm metros e limites anteriores. Ainda não ligados à tela/VM.
- GREEN do formulário e regressões básicas passou em `shared/build/mob-58-formulario-green.log` (4m31s).
- Estado/VM em integração: formulário novo congela a data, identifica menor de 18 anos e recebe dados pediátricos; edição converte estatura m→cm e preserva peso ao nascer com três casas/etnia desconhecida/fator. Salvar converte unidades e usa o bloco infantil sem protocolos/metas adultos. Alterações de medidas/entradas removem o snapshot do formulário, parecer não remove. Prévia de classificação adulta do VM fica ausente em menores.
- Quatro testes do VM foram vistos falhar por símbolos ausentes (`shared/build/mob-58-vm-red.log`, 32s). GREEN com regressões do VM adulto e testes anteriores passou em `shared/build/mob-58-vm-green.log` (4m34s).
- Persistência offline: três testes executados com SQLite em memória viram falhas reais (entradas omitidas na criação otimista, snapshot antigo retido na edição e refresh sobrescrevendo peso pendente), em `shared/build/mob-58-offline-red.log` (44s). O repositório agora guarda as entradas, preserva bloco omitido na edição, remove resultados antigos e protege refresh dentro de transação se houver mutação aberta. O pull de sync já tinha essa proteção genérica. Classificação adulta não é calculada para o registro pediátrico otimista (usa OUTRO).
- GREEN de offline e regressões VM em execução: `shared/build/mob-58-offline-green.log`, sessão 17442. Aguardar antes de novas alterações durante compilação. Teste acompanha retorno de snapshot só após confirmação da fila.
- GREEN offline passou em `shared/build/mob-58-offline-green.log` (4m). Sem migração local necessária: os campos ficam no registro JSON existente.
- UI em integração: campos pediátricos em grade adaptativa, comprimento em cm, peso ao nascer em kg, modo percentil/Z, categoria de Slaughter somente quando aplicável e fator de atividade por idade. Resultados são os snapshots confirmados, com intervalos e motivos; não há régua com ponto exato inventado. Energia/composição e compartilhamento respeitam as seções. Histórico e Meus resultados deixam de usar classificação adulta para menores, inclusive quando o snapshot ainda não existe. MOB-59 não foi iniciado.
- RED de UI/sexo da ficha: quatro falhas comportamentais observadas entre oito testes (`shared/build/mob-58-ui-red.log`, 1m45s). Mudança de sexo/nascimento invalida o snapshot e, se mudar de faixa, converte a unidade da estatura e avisa para revisar a ficha.
- Primeiro compile da UI falhou por duas referências a um rótulo inexistente; referências corrigidas. Não considerar esse run como validação. Novo run com UI/regressões e teste adicional de precisão da comparação em execução: `shared/build/mob-58-ui-integracao.log`, sessão 13226. O teste novo espera comparação pediátrica em cm, pois arredondar o delta em metros hoje apaga diferenças de milímetros; corrigir somente após ler a falha esperada.
- Próximos ajustes após o run: comparação de estatura em cm e rótulo correspondente; guardar idade de referência no formulário para não esconder escolhas do adolescente sem nascimento local após invalidar snapshot; distinguir texto de resultado ausente em avaliação antiga do estado de edição pendente. Depois testes completos, web, capturas 390/820/1180 e fonte 200%, docs/CHANGELOG e PR para QA.
- Integração UI passou nos testes dirigidos; único erro em 26 testes foi o novo caso de comparação, observado em `shared/build/mob-58-ui-integracao.log` (4m40s): esperava 81,1 cm e recebia 0,811 m. Corrigidos domínio e rótulo da comparação para cm em avaliações pediátricas, preservando delta de milímetros.
- Idade em meses agora é guardada no formulário como fallback independente dos resultados. Avaliações antigas sem derivados recebem mensagem de ausência, sem prometer cálculo apenas por sincronizar; rascunho editado continua com estado pendente. Acrescentada faixa de peso teórico quando confirmada pelo servidor, somente à nutri.
- JVM completo em execução: `shared/build/mob-58-jvm-completo.log`, sessão 20945. Não alterar fontes compiladas até concluir. Depois adicionar evidências visuais (usar padrão de captura de AnamneseEstruturadaTelaTest, somente dados sintéticos) e teste MockEngine da porta AvaliacoesOnline. Cadastrar capturas de criança nova, G-16, adolescente e paciente nos tamanhos 390/820/1180 e fonte 200%; conferir as imagens. Compilar também web/desktop antes de PR.
- JVM completo passou: **608 testes, 118 classes, zero falhas**, `shared/build/mob-58-jvm-completo.log` (6m19s).
- Acrescentados teste MockEngine da porta web (payload de edição, If-Match, precisão e snapshot confirmado) e geração de capturas sintéticas de criança nova, G-16, adolescente e paciente em 390/820/1180 e fonte 200%, com ações e energia acessíveis por rolagem. Fixtures visuais são de apresentação, não vetores de cálculo clínico.
- Validação final em execução: `:shared:jvmTest :webApp:compileKotlinWasmJs :desktopApp:compileKotlin --max-workers=2`, log `shared/build/mob-58-validacao-final.log`, sessão 80728. Capturas em `shared/build/reports/MOB-58/` quando o teste concluir. Ler resultado e conferir imagens antes de docs finais, commit e PR.
- Validação passou: **610 testes, 120 classes, zero falhas**, JVM/web/desktop, `shared/build/mob-58-validacao-final.log` (2m19s). Foram geradas e vistas as 23 capturas dos cenários e tamanhos, incluindo ações e energia em fonte 200%.
- Conferência visual encontrou `100%%` no percentual de adequação e espaço antes de `%` na gordura. Novo teste reproduziu o erro (`shared/build/mob-58-percentual-red.log`, 5 testes/1 falha, 56s). Corrigida formatação com `%` literal do Compose; rótulos de comprimento/estatura passam a refletir a idade quando conhecida, para evitar desalinhamento da grade. Leitura pediátrica do paciente limitada à largura de leitura e centralizada no tablet/desktop, com margens.
- Revalidação após correções visuais em execução: `shared/build/mob-58-validacao-visual-final.log`, sessão 53883, mesmos alvos JVM/web/desktop. Aguardar resultado e conferir capturas regeneradas (principalmente percentuais, grade e largura do paciente). Docs técnicos/UI/CHANGELOG e PR continuam pendentes; CHANGELOG já ganhou a entrada e o plano técnico separa MOB-58 de MOB-59, ambos ainda desmarcados até concluir validação.
- Revalidação final passou em 5m28s: **611 testes, 120 classes, zero falhas/erros/ignorados**, com compilação web e desktop. Capturas regeneradas conferidas: adequação `100%`, gordura `17,6%`, grade alinhada com `Estatura (cm)` e leitura do paciente centralizada em largura limitada. As demais capturas já tinham sido conferidas na rodada anterior.
- Plano técnico marca apenas MOB-58 concluída; MOB-59 permanece pendente. Mapa e plano de UI, CHANGELOG e divergências atualizados. Android/iOS dependem dos checks de CI da cabeça publicada. Próximo: commit, PR para develop e QA independente; não iniciar MOB-59 até aprovação e merge.
