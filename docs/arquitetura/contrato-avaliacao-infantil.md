# Avaliação infantil e adolescente — BE-55b

Extensão aditiva de `/api/v1` aos mesmos endpoints de avaliação e à coleção `avaliacoes` do sync. A ficha autenticada e a data local da avaliação determinam a idade, nunca um indicador enviado no corpo. Anos e meses completos são calculados em America/Sao_Paulo; no aniversário de 10 anos passa a adolescente e no de 18 passa a adulto. As calculadoras puras do adulto continuam com suas fórmulas anteriores.

## Entradas opcionais

`infantil` acrescenta `pesoNascerGramas` (inteiro positivo, até 20.000 g como limite técnico), `modoCurva` (`PERCENTIL`, padrão, ou `ESCORE_Z`), `etniaSlaughter` (`BRANCA` ou `NEGRA`, categorias da fórmula de origem, sem inferência) e `fatorAtividade` (positivo, até um algarismo inteiro e três decimais). Valores ausentes ficam sem cálculo dependente, com motivo. Perímetro do braço em `perimetros.braco` continua em cm; tricipital e subescapular em `dobras` continuam em mm. Não enviar resultados derivados como entrada.

Na edição, `infantil` ausente ou nulo preserva o bloco anterior. Um objeto presente substitui o bloco, permitindo limpar entradas opcionais com `{}`. Os derivados são recalculados somente na escrita. Ler uma avaliação antiga não muda idade, cortes ou resultados após atualização da ficha ou do catálogo.

Para menores de 18 anos os limites técnicos permitem peso 0,1–400 kg, estatura 0,2–2,5 m, perímetros 1–250 cm e dobras 0,1–80 mm. Estatura aceita até três decimais para preservar milímetros; as demais medidas mantêm duas casas. Os limites anteriores continuam para adultos e fichas sem nascimento válido. Não são faixas de normalidade clínica.

## Resultado congelado

`resultadosInfantis` contém idade em meses, texto da idade, sexo da referência, modo da curva, classificação do peso ao nascer, peso teórico mínimo/máximo pelas referências de IMC, `curvas`, `frisancho`, `composicao`, `energia` e `motivosNulos`. O bloco é ausente para adultos e avaliações anteriores à integração.

Cada curva guarda indicador, valor, unidade, idade da linha, modo, classificação, posições em percentil/Z, ambas as listas de cortes e fonte/versão. Estatura é convertida de m para cm apenas para a curva. Crianças recebem peso/idade, estatura/idade e IMC/idade; adolescentes recebem IMC/idade. A tabela de 10 a 17 anos é anual: a idade da linha usada pode diferir da idade em meses da avaliação.

Percentil e escore Z são **intervalos**, sem interpolação. `{inferior:50, superior:85}` significa entre P50 e P85, sem incluir os cortes. Limites iguais indicam coincidência com um corte publicado; um limite nulo representa extremo aberto. Não há dados LMS para fornecer um percentil ou Z exato entre cortes. Referência incompleta, ausente ou não crescente não produz posição nem classificação.

Classes seguem BE-55a: peso/estatura `MB/B/A/AC`; IMC infantil `MA/M/A/S/O/OG`; IMC adolescente `BP/NO/SP/OB`. `classificacaoImc` no nível raiz permanece por compatibilidade com clientes anteriores e representa os cortes fixos adultos: para menores de 18 anos, os clientes devem usar a curva `IMC_IDADE` de `resultadosInfantis`, jamais aquele campo legado. `resultados.classificacaoImc` fica ausente e seu motivo orienta usar a curva infantil.

Frisancho guarda CB, DCT, DCSE, AMBc e AGB com medida, unidade, cortes, adequação ou classe de percentil e fonte/versão. CB usa a mediana de **perímetro do braço**, não CMB. AMBc/AGB conservam os números das fórmulas BE-55a e das tabelas, em mm²; o rótulo cm² da planilha não deve ocasionar divisão silenciosa por 100. Referências existem somente de 2 a 9 anos. G-16 permanece: AMBc masculino de 4 anos tem P5 maior que P10; o resultado guarda os cortes originais e o motivo `REFERENCIA_INCONSISTENTE_G16`, sem classificação ou posição.

Composição de adolescentes usa Slaughter e classificação Deurenberg. Sem dobras, sexo ou etnia quando exigida pela fórmula, conserva motivo e não inventa valores. Percentuais fora de 0–100 não são apresentados como composição válida. Energia apresenta os resultados separados `gebIomKcal`, `getIomKcal` e `getRdiKcal`: GET IOM exige 3–17 anos e fator de atividade; RDI até 2 anos independe desse fator. As metas e protocolos adultos não são aplicados automaticamente. Se o cliente solicitar o bloco adulto de energia, seu motivo `USE_ENERGIA_INFANTIL` aponta para o resultado apropriado.

## Compartilhamento

O paciente só recebe avaliações compartilhadas. As entradas `infantil` ficam na visão da nutri. As curvas de peso, estatura e IMC acompanham os dados básicos já compartilhados. `medidas` controla Frisancho e peso ao nascer; `composicao` controla Slaughter/Deurenberg; `necessidades` controla energia. Motivos internos não acompanham a visão do paciente. REST e sync aplicam a mesma filtragem. Outra nutri recebe 404.

## Verificação

Testes de cortes e orquestração usam somente valores sintéticos e os vetores já publicados. Testes de API verificam precisão, snapshot, edição de cliente anterior, G-16, isolamento e seções ocultas. Os arquivos originais permanecem fora do Git, sem copiar respostas ou dados de saúde para exemplos.

## Prévia sem gravar — BE-55c

`POST /api/v1/pacientes/{id}/avaliacoes/previa` recebe o mesmo corpo de `criarAvaliacao` e devolve `{imc, classificacaoImc, faixaEtaria, resultados, energiaResultado, resultadosInfantis}` calculados pelas mesmas regras da gravação (idade pela data local e pela ficha, limites técnicos, G-16, intervalos de percentil/Z). Não grava, não audita escrita e não usa `Idempotency-Key`. Só a nutri dona do vínculo; paciente de outra nutri é `404`. Os percentis continuam sendo **intervalos**: não há LMS, então o painel "Resultados ao vivo" mostra o intervalo confirmado, nunca um valor exato. A prévia não é um resultado congelado: só a gravação congela.
