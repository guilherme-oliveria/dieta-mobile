# MOB-61 — Anamnese estruturada

Referências: A-Anamnese-Estruturada e P-Anamnese. O assistente continua usando os blocos/perguntas congelados recebidos do servidor, com progresso, ações de navegação e salvamento parcial. A nutri consulta respostas e resultados na ficha; o backend permite somente ao paciente responder.

Campos codificados de PA, sal e óleo ficam em cartões, com dois campos por linha e uma coluna com fonte ampliada. Bebidas e dias de atividade ficam em grupos expansíveis, abertos automaticamente quando há erro. Medidas de bebida e perguntas com muitas alternativas usam seletor compacto, preservando os rótulos e opções do servidor. Códigos não aparecem na interface; modelos antigos e perguntas futuras mantêm o formulário genérico.

Resultados aparecem como rótulo e valor, nunca só cor. A pressão é escrita por extenso, classes futuras têm mensagem legível e valores ausentes não viram zero. Óleo mostra volume e energia separados; álcool mostra volume, etanol e energia semanais recebidos, sem fazer contas no cliente. Até 820 px o cartão de resultados fica no fluxo; em janela expandida ele fica em uma coluna lateral de 360 dp. A fonte ampliada usa uma coluna e ações que podem quebrar em mais de uma linha.

Diferenças deliberadas em relação aos números e à edição do artboard:

- A medida de latinha do contrato é 355 ml; o exemplo de 3 medidas em 3 dias resulta em 127,8 g de etanol por semana, em vez dos 126 g ilustrativos com 350 ml.
- Sal e óleo usam os resultados inteiros do BE-57a. Para 650 g/3 pessoas, sal = 7 g por pessoa/dia; para 2000 ml/4 pessoas, óleo = 16 ml e 144 kcal. Nenhum valor é recalculado ou arredondado de outra forma no app.
- Os valores são confirmados pelo servidor. Ao digitar outra resposta ou alterar o cache local, os resultados anteriores desaparecem até a confirmação; offline é possível salvar parcial e atualizar depois. A ação “Atualizar resultados” não conclui o questionário.
- O modelo padrão tem oito bebidas e atividades nos sete dias. Os cartões expansíveis evitam exibir simultaneamente todas as linhas da planilha. A frequência contém os 20 grupos e sete alternativas encontrados nos arquivos fornecidos, conforme o contrato oficial.
- A tela da nutri é de leitura. Os campos editáveis do artboard são apresentados no assistente do paciente, respeitando as permissões da API.

Foram conferidas capturas do paciente e da nutri em 390×844, 820×1180 e 1180×820, além de 390×844 com fonte 200%. Campos e resultados podem rolar; as ações do paciente permanecem disponíveis e quebram em linhas sem cortar texto na fonte ampliada. Os rótulos integrais do servidor podem ter quantidades diferentes de linhas nos campos pareados. A ficha da nutri fornece as margens externas ao conteúdo de leitura.

Previews de resultados em três tamanhos usam somente valores sintéticos. As 16 capturas automatizadas ficam em `shared/build/reports/MOB-61/` e não entram no Git. Resultados finais de testes ficam no corpo do PR.
