# Dieta+ — plano de execução das melhorias com as planilhas da nutri

> Documento comum ao `dieta-backend` e ao `dieta-mobile`, no mesmo formato do `plano-de-execucao.md` (ondas BE/MOB/DES, Gitflow com `develop`). Esta é a cópia oficial (a do `dieta-mobile` é espelho). Os anexos de fórmulas e macros ficam na pasta do projeto (`planos/anexos-planilhas/`), por serem grandes.
> Criado em 31/10/2026 por um agente PO a partir das 4 planilhas da Nut. Luciana (pasta `Documento dieta`), das macros VBA delas, do `relatorio-pendencias-e-pdfs.md` (30/10) e do código da `develop` (backend até BE-38, mobile até MOB-46).
> **Status: aprovado pelo Guilherme em 31/10/2026; em execução a partir da onda 17** (andamento em `plano-de-acao.md`, fase 8). Web Angular fora (o site da nutri e do paciente é o alvo web do KMP, que ganha tudo daqui de graça).
> Anexos (fórmulas célula por célula e macros): `planos/anexos-planilhas/`.

## 0. Resumo para o Guilherme

1. As planilhas são muito mais que IMC: são **4 avaliações completas** (cadastro, anamnese, alimentar, comportamental, bioquímica com ~150 exames, antropometria, gasto energético por 6 protocolos, metas de macro e micronutrientes) e **uma calculadora de dieta por porções** com 522 alimentos, 36 nutrientes e 411 suplementos. Hoje o app calcula só o IMC.
2. Boa notícia: o modelo do app (grupo com kcal por porção, alimento com medida caseira e gramas, lista de substituição por paciente) **é o mesmo da planilha de dieta**. Dá para crescer sem refazer nada.
3. O plano tem **7 ondas (17 a 23)** e 3 marcos: **M8** avaliação de adulto completa com gráficos (nutri e paciente), **M9** dieta calculada contra as metas + **dietas pré-prontas por doença editáveis por nutri**, **M10** crianças, adolescentes, exames e anamnese estruturada.
4. Os cálculos ficam no backend (fonte de verdade) e são repetidos no app só como prévia ao vivo e offline, com **os mesmos vetores de teste** nos dois lados, gerados a partir das fórmulas da planilha. Conferi o primeiro com os seus números do PDF: 26,5% de gordura, 27,1 kg de peso gordo, 24,7 kg de residual e 91,9 kg para 18% batem.
5. Achei **11 inconsistências nas planilhas** (seção 3.6), por exemplo uma macro de adolescente que ignora a idade e o potássio em "mg" com valor em gramas. O Guilherme mandou corrigir todas em 31/10/2026: o app implementa a versão corrigida (as planilhas da Luciana não são alteradas) e o que mudou aparece nas notas de versão para ela conferir (G-16 virou só aviso).
6. As dietas pré-prontas do SUS (hipertensão, intolerância à lactose etc.) entram como **modelos**: o sistema traz um conjunto inicial, cada nutri copia e edita os dela, e aplicar um modelo a um paciente cria um rascunho já ajustado às calorias dele. O conteúdo clínico dos modelos é suposição até a Luciana revisar (G-15).

## 1. Como o trabalho anda

Igual ao `plano-de-execucao.md`, seção 1: uma tarefa = uma branch `feature/BE-NN-…` ou `feature/MOB-NN-…` a partir de `develop`, PR para `develop`, `release/X.Y.Z` por marco aprovada pelo Guilherme, contrato primeiro (BE-Cn com JSONs em `src/test/resources/contrato/`) e o mobile contra `MockEngine` até o endpoint real chegar. Fluxo de validação atual (agente faz merge na `develop` depois de build/testes, item A1 do plano) continua valendo.

Três regras novas desta fase:

1. **Paridade com a planilha é teste, não opinião.** Cada fórmula portada tem vetores de teste em `src/test/resources/contrato/calculos/*.json` (entrada → saída esperada, com a célula de origem). O backend (`CalculadoraXxxTest`) e o mobile (`dominio/calculos`, `jvmTest`) rodam o **mesmo arquivo**. Mudou uma fórmula: muda o vetor nos dois repos no mesmo par de PRs.
2. **Tabelas de referência são dado, não código.** Curvas da OMS, Pollock e Wilmore, faixas de exames, DRIs, METs e Frisancho entram por changeset Liquibase em tabelas `referencia_*` versionadas (coluna `fonte`, `versao_referencia`). O código só lê. Assim, corrigir uma faixa não exige release do app.
3. **Protocolo é escolha da nutri.** Onde a planilha tem seletor (3 ou 7 dobras, peso magro ou muscular, percentil ou escore Z, GEB/GET por FAO, IOM, RDI, Cunningham ou MET, faixas de carboidrato/proteína/lipídio), o app tem um padrão por nutri ("Meus protocolos") que pode ser trocado em cada avaliação. A avaliação grava o protocolo usado, para o resultado nunca mudar sozinho.

## 2. O que as planilhas fazem (mapa)

| Planilha | Abas | O que entra no app |
| --- | --- | --- |
| 18 anos acima (adulto e idoso) | cadastro, capa, av. clínica, av. alimentar, av. comportamental, evolução, av. bioquímica, av.antropométrica, MET, macronutrientes e calorias, micronutrientes | Avaliação de adulto/idoso completa (ondas 17 a 19 e 23) |
| 10 a 17 anos | as mesmas + tabela I.M.C. por idade | Avaliação de adolescente (onda 22) |
| 0 a 9 anos | sem comportamental e MET; com P.I., E.I., I.M.C. (curvas OMS por mês) e "outras" (Frisancho) | Avaliação infantil com curvas de crescimento (onda 22) |
| Dieta sem restrições | Escolha as porções, Calcule a dieta, Cardápio pronto/modelável 1 e 2, Lista pequena/média/grande/modelável/com contagem, Alimentos, Produtos | Planejador por porções, metas, suplementos, listas e modelos de dieta (ondas 20 e 21) |

Todas guardam **8 avaliações lado a lado** (colunas E..L ou D..K = datas). No app isso já é a lista de avaliações + evolução; a planilha não traz nada que exija outro modelo de dado.

## 3. Fórmulas extraídas (o essencial; o célula a célula está nos anexos)

Convenções: `P` peso atual (kg), `E` estatura (m), `I` idade em anos na data da avaliação `INT((data − nascimento)/365,25)`, sexo `masculino|feminino`. Entre colchetes, a célula de origem na planilha de 18+ (aba `av.antropométrica`, coluna E = primeira data).

### 3.1 Antropometria e composição, adulto e idoso (18+)

| Resultado | Fórmula | Origem |
| --- | --- | --- |
| Idade | `INT((data − nascimento)/365,25)` | [E11] |
| IMC | `P / E²` | [E14] |
| Peso teórico mín./máx. | 18 a 59 anos: `E² × 18,5` e `E² × 24,9`; 60+: `E² × 22` e `E² × 27` | [E15], [E16] |
| Classificação do IMC, adulto | `<16 MG III`, `<17 MG II`, `<18,5 MG I`, `<25 NO`, `<30 SP`, `<35 OB I`, `<39 OB II`, senão `OB III` | macro `IMC_ADULTO` |
| Classificação do IMC, idoso | 60 a 64: `<23 MG`, `23–28 NO`, `>28 SP`; 65+: `<24 MG`, `24–29 NO`, `>29 SP` | `IMC_IDOSO` |
| Risco de comorbidades | MG* → `B`; NO → `ME`; SP → `A`; OB I → `MO`; OB II → `S`; OB III → `MS` | [E18] |
| Compleição | `r = E×100 / punho`; mulher `>11 pequena`, `10,1–11 média`, `<10,1 grande`; homem `>10,4`, `9,6–10,4`, `<9,6` | [H5] |
| Classificação da cintura | mulher `<80 B`, `80–87,9 A`, `≥88 MA`; homem `<94 B`, `94–101,9 A`, `≥102 MA` | [E34] |
| Somatório de dobras | soma das 7 dobras (tricipital, peitoral, axilar média, subescapular, abdominal, suprailíaca, coxa) | [E64] |
| Densidade e % gordura | Jackson e Pollock + Siri: `%G = (4,95 / D − 4,5) × 100` | [E67] |
| — 7 dobras, homem | `D = 1,112 − 0,00043499·S + 0,00000055·S² − 0,0002882·I` | `POL_7DC_MASC` |
| — 7 dobras, mulher | `D = 1,097 − 0,00046971·S + 0,00000056·S² − 0,00012828·I` | `POL_7DC_FEM` |
| — 3 dobras, homem (peitoral, abdominal, coxa) | `D = 1,10938 − 0,0008267·S + 0,0000016·S² − 0,0002574·I` | `POL_3DC_MASC` |
| — 3 dobras, mulher (tricipital, suprailíaca, coxa) | `D = 1,0994921 − 0,0009929·S + 0,0000023·S² − 0,0001392·I` | `POL_3DC_FEM` |
| % peso magro | `100 − %G` | [E68] |
| Peso gordo | `P × %G / 100` | [E69] |
| Peso ósseo (Von Döbeln/Rocha) | `3,02 × (400 × E² × punho_diam/100 × joelho_diam/100)^0,712` (diâmetros em cm) | [E71] |
| Peso residual | homem `24,1% × P`; mulher `20,9% × P` | [E72] |
| Peso magro ou muscular | protocolo 1: `P − peso gordo`; protocolo 2 (muscular): `P − (gordo + ósseo + residual)` | [E70] |
| Classificação do % gordura | Pollock e Wilmore (1993), 8 faixas (MT BXO, EXC, MT BOM, BOM, ADEQ, MOD ALTO, ALTO, MT ALTO) por sexo e idade 18–25, 26–35, 36–45, 46–55, >55 | `CLASS_PERC_GORD_ADULTO_IDOSO` |
| Peso para % de gordura desejado | protocolo 1: `magro / (1 − %desejado/100)`; protocolo 2: `(muscular + ósseo + residual) / (1 − %desejado/100)` | [E75] |
| Texto-resumo | "O avaliado apresenta" + frase do IMC + frase do risco + frase da cintura + frase do % gordura, da avaliação mais recente | [B96], macros `DIAG_*` |

**Vetor de teste nº 1 (seus dados do PDF):** homem, 29 anos, 102,5 kg, 1,93 m, soma de 7 dobras 199 → IMC 27,52 (SP, risco A), peso teórico 68,9–92,8, D = 1,0389, %G = 26,48 (ALTO), gordo 27,1, magro 75,4, residual 24,7, peso para 18% = 91,9. Todos batem com o PDF.

### 3.2 Gasto energético e metas (aba `macronutrientes e calorias` e `MET`)

Seletores da aba: `O1` lactante (1 sim, 2 não), `O2` período da lactação (1 = 0–6 meses, 2 = 7–12), `O3` protocolo de carboidrato, `O4` de proteína, `O5` de lipídio, `O6` gestação, `R5` gestante.

| Resultado | Fórmula |
| --- | --- |
| GEB FAO 1985 | homem 18–29 `15,3P + 679`; 30–59 `11,6P + 879`; 60+ `13,5P + 487`; mulher 18–29 `14,7P + 496`; 30–59 `8,7P + 829`; 60+ `10,5P + 596` |
| GEB FAO 2004 | homem `15,057P + 692,2`, `11,472P + 873,1`, `11,711P + 587,7`; mulher `14,818P + 486,6`, `8,126P + 845,6`, `9,082P + 658,5` (mesmas faixas) |
| GEB IOM 2002 | eutrófico/magro: homem `204 − 4I + 450,5E + 11,69P`; mulher `255 − 2,35I + 361,6E + 9,39P`; sobrepeso/obeso: homem `293 − 3,8I + 456,4E + 10,12P`; mulher `247 − 2,67I + 401,5E + 8,6P` |
| GEB Cunningham 1991 | `370 + 21,6 × massa magra` (ou massa muscular + óssea + residual no protocolo 2) |
| GET FAO 1985 | `GEB × fator`: sedentário 1,3; leve 1,55 (H) / 1,56 (M); moderada 1,78 / 1,64; intensa 2,1 / 1,82 |
| GET FAO 2004 | homem 1,53 / 1,53 / 1,76 / 2,25; mulher igual, + 675 kcal na lactação (a fórmula completa está no anexo, linha 18) |
| GET IOM 2005 | `GEB IOM × 1,2 / 1,5 / 1,75 / 2,2` |
| GET RDI 2005 (EER) | homem `662 − 9,53I + AF × (15,91P + 539,36E)`, AF 1 / 1,11 / 1,25 / 1,48; mulher `354 − 6,91I + AF × (9,36P + 727E)`, AF 1 / 1,12 / 1,27 / 1,45; lactante + 330 (0–6 m) ou + 400 (7–12 m) |
| GET por MET | `GEB/h = Cunningham/24`; sono `GEB/h × horas × 0,9`; trabalho `GEB/h × horas × fator (1,55–2,1 conforme sexo e intensidade)`; até 5 atividades `minutos/60 × MET × P` (tabela de 113 atividades); rotina `GEB/h × horas restantes × 1,3`; GET = soma |
| VET | manutenção `= GET`; ganho `GET + (kg/mês ÷ 4 × 1000) × 500/450`; perda `GET − (…)` |
| Kcal/kg | `VET / P` |
| Carboidrato | protocolo 1: 45–65% do VET; 3: 50–60%; 4: 5–10 g/kg; 2: mínimo 130 g (210 lactante, 175 gestante) |
| Proteína | protocolo 1: 10–35%; 3: mínimo 15%; 4: endurance 1,2–1,6 g/kg, hipertrofia 1,4–1,8 g/kg; 2: RDA (71 g lactante) |
| Lipídio | mínimo 20% (protocolo 1) ou 25% (2), máximo 35%; dislipidêmico (SBC 2007): saturado 7%, mono 20%, poli 10%, trans 1% |
| g a partir de % | carboidrato e proteína `% × VET / 4`; lipídio `% × VET / 9` |
| Colesterol | 300 mg/dia, ou 200 se colesterol total ≥ 200 na bioquímica |
| Fibras, água, 13 minerais e 13 vitaminas | tabelas DRI por sexo, idade e gestação/lactação (macros `RDI_*`; valores no anexo `macros-vba-18-anos.txt`) |

### 3.3 Crianças (0 a 9) e adolescentes (10 a 17)

| Resultado | Como a planilha faz |
| --- | --- |
| Idade em anos e meses | texto "1 ano e 3 meses" a partir da data |
| Classificação do peso ao nascer | `<1000 extremamente baixo`, `≤1500 muito baixo`, `≤2500 baixo`, `≤3000 insuficiente`, `≤4000 normal`, senão macrossomia |
| Peso/idade, estatura/idade, IMC/idade (0–9) | busca por idade em meses nas curvas OMS (abas P.I., E.I., I.M.C., 120 meses × percentis e escores Z por sexo); seletor percentil (0,1/3/97) ou Z (−3/−2/+2); IMC usa 0,1/3/85/97/99 ou −3/−2/+1/+2/+3 (macros `TESTA_PESO`, `TESTA_EST`, `TESTA_IMC`) |
| Faixa esperada de peso para o IMC | `IMC de referência × estatura²` |
| CMB, DCT, DCSE (adequação) | `medida / P50 de Frisancho × 100`; `<70 DG`, `70–80 DM`, `80–90 DL`, `90–110 A`, `110–120 EP`, `>120 O` |
| AMBc (área muscular do braço corrigida) | `((CB×10 − π×DCT)² / 4π) − 10` (menino) ou `− 6,5` (menina); percentis 5/10/90 de Frisancho |
| AGB | área de gordura do braço com percentis de Frisancho |
| GEB e GET IOM, criança e adolescente | GEB menino `79 − 34,2I + 730E + 15,3P`, menina `322 − 26I + 504E + 11,6P`; GET menino `114 − 50,9I + AF(19,5P + 1161E)`, menina `389 − 41,2I + AF(15P + 701,6E)` |
| GET RDI, criança | `< 3 meses (89P − 100) + 175`; `4–6 m + 56`; `7–12 m + 22`; `1–2 anos + 20`; 3–8 anos menino `88,5 − 61,9I + AF(26,7P + 903E) + 20`, menina `135,3 − 30,8I + AF(10P + 934E) + 20`; 9 anos `+ 25` |
| % gordura, adolescente (Slaughter) | soma tricipital + subescapular; menina `≤35: 1,33S − 0,013S² − 2,5`, `>35: 0,546S + 9,7`; menino por etnia e faixa (constantes −1,7/−3,4/−5,5 ou −3,5/−5,2/−6,8), `>35: 0,783S + 1,6` |
| Classificação do % gordura, adolescente (Deurenberg 1990) | menino `<6 MT BXO`, `6–10 BXO`, `10–20 ADEQ`, `20–25 MOD ALTO`, `25–31 ALTO`, `>31 MT ALTO`; menina `<12`, `12–15`, `15–25`, `25–30`, `30–36`, `>36` |
| IMC/idade, adolescente | tabela por idade 10–17 com P3/P85/P97 ou −2/+1/+2 → BP, NO, SP, OB |

### 3.4 Dieta (planilha "Dieta sem restrições")

| Parte | Como funciona |
| --- | --- |
| Escolha as porções | Uma porção por grupo, que define a coluna "kcal" de toda a dieta e das listas: Pão 70 ou 150; Recheio 25 ou 40; Carne/ovo 110 ou 200; Arroz 65/80/100/150; Feijão 20/40/45; Leite 55/70/100; Fruta 70; Vegetal refogado 25 ou 50; Vegetal cru 5 ou 13 (ou "à vontade"); Castanha 30 ou 70; Gordura 45 ou 70; Açúcar 50/80/100; Sopa 250; Embutido 60; Bebida 120. Cada opção também pode ser "não usar". Opção "medidas caseiras detalhadas (cheio, raso, nivelado)" |
| Perfil de 1 porção do grupo | `média` dos alimentos do grupo na porção escolhida, para 36 nutrientes (`Alimentos!F1230 = AVERAGE(F1165:F1229)`), com desvio-padrão ao lado. Cada alimento: `nutriente/100 g × gramas da porção` |
| Calcule a dieta | Grade 15 grupos × 10 refeições (desjejum 6:00, colação, almoço, antes/durante/após o treino, lanche, lanche, jantar, ceia) + 4 opções alternativas (lanche 2ª opção "sanduba", lanche 3ª, jantar sanduba, jantar sopa); até 4 "outros alimentos" livres e 10 produtos da aba Produtos (`VLOOKUP(nome) / 100 × gramas`) |
| Totais por refeição e dia | `Σ porções × nutriente por porção`; kcal, kcal/kg, % do VCT, carboidrato g, g/kg e %, proteína g, g/kg e %, lipídio g, g/kg e `% = 100 − %gli − %ptn`, saturado, mono, poli (g e %), trans, Ca, Mg, B2, B12, vit. E e os demais micronutrientes |
| Peso para os "por kg" | adulto com IMC ≥ 30: **peso ajustado** `(P − Pideal) × 0,25 + Pideal`, com `Pideal = E² × (18,5 + 24,9)/2`; senão o peso atual |
| Cardápio | Por refeição: grupo, número de porções e "Substituições: 1 porção do grupo X", kcal da refeição e "Total de carboidratos desta refeição"; texto livre "à vontade" e o rodapé "Além das refeições acima, você também poderá ingerir…" |
| Listas de substituição | pequena, média, grande, modelável e "com contagem de carboidratos" (para diabetes), geradas da porção escolhida |
| Alimentos | 522 alimentos por 100 g com 36 nutrientes (energia, gli, pro, lip, sat, mono, poli, trans, colesterol, fibra, Na, K, Ca, P, Fe, Cu, I, Mg, Mn, Se, Zn, B1, B2, niacina, pantotênico, B6, folato, B12, retinol, vit. A, C, D, E, água) |
| Produtos | 411 suplementos, leites e dietas clínicas (marca, classificação, apresentação, embalagem, sabores, medida caseira, g/ml e nutrientes por 100 g) |

### 3.5 Clínica, bioquímica, comportamental e alimentar

| Aba | Cálculos |
| --- | --- |
| av. clínica | Classificação da pressão arterial `CLASS_PA(sistólica, diastólica)`: ótima, normal, limítrofe/pré-hipertensão, hipertensão estágio I, II, III, sistólica isolada (tabela completa no anexo). Demais campos são de anamnese (queixas, intestino, urina, edema, estresse, sono, exames, cirurgias, medicamentos, doenças na família) |
| av. bioquímica | ~150 exames em 12 blocos (rotina, complementares, hemograma vermelho e branco, perfil lipídico, EAS, proteínas, minerais, vitaminas, hormônios, imunológicos, leptina…). Cada exame: valor por data e resultado (`baixo`, `limite inferior`, `normal`, `limite superior`, `alto`, ou rótulos próprios como "int. à glicose"/"diabetes", "ótimo/limítrofe/alto", "presente/ausente") por faixa, que pode depender de sexo, idade (< 60), fase do ciclo, menopausa ou IMC |
| av. comportamental | Álcool: `doses × dias × ml da medida` e `g de etanol/semana = ml × 0,8 × 7 × teor`; tabagismo; atividade física com `duração = fim − início` |
| av. alimentar | Sal `g/pessoa/dia = g no mês / pessoas / 30` (referência OMS ≤ 5 g); óleo `ml/pessoa/dia` e `× 9 kcal`; registro alimentar por refeição; frequência alimentar de 30 grupos |

### 3.6 Inconsistências achadas (correção aprovada pelo Guilherme em 31/10/2026)

O app implementa a coluna "Decisão". Cada correção tem um vetor de teste próprio marcando que difere da planilha original. As planilhas da Luciana ficam como estão.

| # | Onde | O que acontece | Decisão |
| --- | --- | --- | --- |
| 1 | `SLAUGHTER` (10–17) | A macro usa `IDADE`, que não é parâmetro: para meninos todas as faixas de idade caem na fórmula de soma > 35. Etnia 2 tem `IDADE > 9 And IDADE > 12` | Passar a idade e corrigir para `> 9 And < 12` |
| 2 | `IMC_ADULTO` | OB II até `< 39`; a OMS usa `< 40` | Usar 40 |
| 3 | `DIAG_COMP_IMC` | Compara com `"MS I"`, mas o risco gravado é `"MS"`: obesidade III fica sem frase | Usar `MS` |
| 4 | `TESTA_AMBC` | Usa `PESO_C`, que não existe: nunca devolve "> 90°" | Comparar com o P90 |
| 5 | `TESTA_CMB` | Faixas se sobrepõem (`> 90 e ≤ 110` depois `> 100 e ≤ 120`) | 110,1–120 = EP |
| 6 | Proteína mínima, protocolo 2 | Chama `RDI_PTN` passando o **peso** no lugar da idade | Passar a idade |
| 7 | Micronutrientes, cobre | `IF(O1, 1300, …)` sem `= 1`: todo mundo recebe 1300 mcg (valor de lactante) | `O1 = 1` |
| 8 | Micronutrientes, potássio e sódio | Rótulo "mg/dia" com valores em gramas (5,1 e 1,5) | Gravar em mg (5100 e 1500) |
| 9 | Vitamina A | Rótulo "mg/dia", valores em mcg | mcg |
| 10 | Bioquímica | Valor exatamente no limite vira "limite inferior/superior" e só valores estritamente entre os limites são "normal" | Corrigir: valor igual ao limite conta como "normal" (faixa fechada), e a UI mostra "no limite" como destaque leve |
| 11 | `GET_IOM` / `GEB_IOM` | Criança usa estatura em cm (`EST/100`), adolescente em m | Corrigir: o app usa metros em todas as faixas, e o vetor confirma que o resultado é o mesmo |

## 4. Como fica no app

### 4.1 Nutri: avaliação em 4 passos, com cálculo ao vivo

`NovaAvaliacaoTela` vira um fluxo de 4 passos; no tablet e no desktop o painel da direita mostra os resultados recalculando a cada campo (prévia local, offline).

1. **Medidas**: peso, estatura, perímetros, dobras, punho e diâmetros. Botão "repetir medidas da última avaliação", teclado decimal, faixas de sanidade (RN-AVA-03) e um aviso discreto do que falta ("informe punho e joelho para calcular o peso ósseo"). Os campos aparecem conforme a faixa etária (criança mostra peso ao nascer, CB, DCT e DCSE; adulto as 7 dobras).
2. **Composição**: % gordura, pesos gordo/magro/ósseo/residual, classificação com a tabela de referência em destaque na faixa do paciente, % desejado com o "peso para esse %". Gráfico de **rosca** da composição (gordura, músculo, osso, residual).
3. **Necessidades**: GEB e GET por protocolo lado a lado (a nutri toca no que quer usar), objetivo (ganho/manutenção/perda e kg por mês), VET, metas de carboidrato, proteína e lipídio como **rosca de distribuição** com o anel externo mostrando a faixa recomendada, fibras, água e micronutrientes.
4. **Resumo**: texto gerado (editável antes de salvar), o que será compartilhado com o paciente (chaves por seção) e "Gerar PDF".

Cada número tem um "i" com a fórmula, a referência e a explicação curta. Os textos de ajuda das planilhas (as caixas das macros `Image*_Click`) viram esse conteúdo.

**Meus protocolos** (Conta → Protocolos): 3 ou 7 dobras, peso magro ou muscular, percentil ou escore Z, GEB/GET preferidos, faixas de macros. Vale para as próximas avaliações; cada avaliação grava o que usou.

### 4.2 Nutri: dieta calculada contra as metas

O montador atual ganha o modo **Planejar por porções**, que é a aba "Calcule a dieta" no app:

- Grade grupos × refeições com contadores (+/−). No celular, uma refeição por vez; no tablet, a grade inteira.
- Barra de metas fixa no topo: kcal do dia vs VET, e uma rosca de carboidrato/proteína/lipídio que fica verde dentro da faixa e laranja fora (laranja `#C2551F` só para alerta, como o tema já define).
- Barras de kcal por refeição; aba "Micronutrientes" com adequação à DRI.
- Porção por grupo escolhida uma vez por dieta (Pão 70 ou 150 etc.).
- Opções alternativas de refeição (lanche 2ª opção, jantar sopa), itens "à vontade" sem kcal, rodapé "além das refeições", suplementos e produtos com medida própria.
- "Converter em alimentos": a grade preenche a dieta atual (itens com alimento de referência) para manter o resto do sistema igual (snapshot, check-in, lista de compras).

### 4.3 Dietas pré-prontas por condição (modelos)

- **Modelo de dieta** = refeições, porções por grupo, porção escolhida por grupo, alternativas, textos "à vontade", orientação e **restrições** (ex.: sem lactose, sódio até 2000 mg, sem glúten, contagem de carboidratos).
- O sistema traz modelos iniciais (seção 6.3). Cada nutri vê os do sistema só para leitura e trabalha com **cópias dela**, que edita, renomeia, duplica ou apaga. Ela também salva qualquer dieta como modelo.
- **Aplicar a um paciente** cria um rascunho: escolhe o modelo, o app mostra o VET do paciente e propõe escalar as porções (arredondando para 0,5) para chegar perto dele; a nutri revisa no planejador.
- **Restrições filtram a lista de substituição** do paciente: alimentos marcados (contém lactose, glúten, alto sódio, açúcar de adição) saem da lista dele, e a dieta avisa se um item não respeita a restrição.
- Sugestão (não automática): se a avaliação mostra PA em hipertensão ou glicemia alterada, o app sugere os modelos correspondentes. A decisão é sempre da nutri.

### 4.4 Paciente: "Meus resultados"

Aba nova em Evolução, pensada para leigo:

- **Cartões com semáforo**: IMC numa régua colorida com a faixa dele marcada, % gordura num medidor, cintura com risco em palavras ("risco aumentado"), sem siglas (MG, SP, MT BOM viram texto).
- **Composição**: rosca gordura × massa magra × osso × residual e a frase-resumo da nutri.
- **Minha meta**: peso atual → peso para o % desejado, com a distância em kg e a linha do tempo.
- **Evolução**: linhas de peso, % gordura, cintura e dobras com a faixa de referência sombreada (ex.: peso teórico mín.–máx.); para criança, a **curva de crescimento da OMS** com o ponto da criança.
- **Minha dieta**: rosca de carboidrato/proteína/gordura do dia e de cada refeição, e "1 porção do grupo X" em cada item com atalho para a lista de substituição (P0 do relatório).
- **Exames** (se a nutri compartilhar): cada exame com uma barra de faixa (baixo, normal, alto) e o ponto do valor.
- Só aparece o que a nutri marcou como compartilhado (RN-AVA-05, agora por seção). Tudo funciona offline depois da sync.

### 4.5 Gráficos

Biblioteca própria em `ui/comum/graficos` (Compose Canvas, sem dependência nova, funciona em Android, iOS, desktop e web): `GraficoRosca` (pizza/rosca com rótulos e anel de meta), `GraficoLinha` (o atual, ampliado para várias séries, faixa sombreada e pontos tocáveis), `GraficoBarras`, `ReguaFaixas` (IMC, exames), `Medidor` e `CurvaCrescimento` (percentis OMS). Requisitos: animação curta na entrada, cores do tema, tamanhos para celular/tablet, `semantics` com o valor por extenso e "ver como tabela" para leitor de tela. Os mesmos gráficos saem no PDF (o backend desenha a versão estática com a mesma paleta).

## 5. Regras de negócio novas (entram em `regras-de-negocio.md` pelo BE-C7)

- **RN-CAL-01** O backend calcula e grava todos os resultados derivados (composição, classificações, GEB/GET, VET, metas, texto-resumo). O cliente mostra prévia com as mesmas fórmulas; o valor gravado é o do backend (como RN-DIE-05).
- **RN-CAL-02** A faixa etária da avaliação é decidida pela idade na data: 0–9, 10–17, 18–59, 60+.
- **RN-CAL-03** Cada avaliação grava o protocolo e a `versao_referencia` usados. Recalcular só acontece se a nutri pedir.
- **RN-CAL-04** Resultado que depende de dado ausente fica nulo, com o motivo (ex.: `FALTA_PUNHO`), nunca zero.
- **RN-CAL-05** Peso para "por kg" na dieta: adulto com IMC ≥ 30 usa o peso ajustado.
- **RN-CAL-06** Vetores de teste da planilha são contrato: mudança de fórmula exige mudar o vetor nos dois repositórios.
- **RN-MOD-01** Modelos do sistema são somente leitura; a nutri trabalha com cópias dela, isoladas por `nutricionista_id` (de outra nutri = 404).
- **RN-MOD-02** Aplicar modelo cria rascunho (RN-DIE-02); nunca publica sozinho.
- **RN-MOD-03** Restrição do modelo vira restrição do paciente na dieta publicada e filtra a lista dele no snapshot (RN-DIE-03).
- **RN-RES-01** O paciente vê resultados por seção conforme o compartilhamento da nutri; exames são não compartilhados por padrão.
- **RN-RES-02** Textos para o paciente não usam siglas clínicas.

## 6. Tarefas

### 6.1 Ondas

| Onda | Backend | Mobile | Design | Integração ao fim da onda |
| --- | --- | --- | --- | --- |
| 17. Fundação | **BE-C7** contrato da fase 7 (avaliação ampliada, resultados, protocolos, vetores de teste) · **BE-39** tabelas de referência (adulto) · **BE-40** motor de cálculo antropométrico adulto | **MOB-47** biblioteca de gráficos · **MOB-48** `dominio/calculos` com os vetores · **MOB-49** P0 do relatório (paciente vê perímetros, dobras, composição, parecer, macros e substituições) | **DES-08** avaliação em 4 passos e "Meus resultados" | Os vetores passam nos dois repos; paciente já vê o que o backend devolve |
| 18. Avaliação adulto | **BE-41** avaliação ampliada (campos novos opcionais, protocolo, resultados e texto-resumo gravados) · **BE-42** "Meus protocolos" da nutri | **MOB-50** avaliação em 4 passos (passos 1, 2 e 4) · **MOB-51** "Meus resultados" do paciente | **DES-09** dieta por porções e modelos | **Marco M8 (1.2.0)**: avaliação de adulto igual à planilha, com gráficos para nutri e paciente |
| 19. Energia e metas | **BE-43** GEB/GET (FAO 85/04, IOM, RDI, Cunningham), VET, metas de macro, lipídios SBC, colesterol, fibras, água · **BE-44** MET (tabela de 113 atividades e cálculo) · **BE-45** metas de micronutrientes (DRI) · **BE-46** PDF de avaliação no layout da nutri com gráficos | **MOB-52** passo 3 "Necessidades" · **MOB-53** Meus protocolos | — | — |
| 20. Dieta calculada | **BE-C8** contrato da fase 8 (porções, produtos, metas na dieta, modelos) · **BE-47** grupos com variantes de porção, 3 grupos novos (Vegetal cru, Embutido, Bebida) e 36 nutrientes por alimento importados da planilha · **BE-48** produtos e suplementos · **BE-49** dieta ligada às metas: alternativas, itens à vontade, rodapé, totais de micronutrientes, adequação, peso ajustado · **BE-50** PDF da dieta (coluna Substituições) e PDF da lista de substituição | **MOB-54** planejador por porções com rosca de metas e barras por refeição · **MOB-55** paciente: rosca de macros e lista de substituição para consulta | — | Dieta de 2509 kcal do PDF montada pela grade bate com a planilha |
| 21. Dietas pré-prontas | **BE-51** marcadores de restrição nos alimentos · **BE-52** modelos de dieta (sistema + nutri, aplicar com escala por VET, restrições filtram a lista) · **BE-53** modelos iniciais (seed) | **MOB-56** biblioteca de modelos (lista, editar, duplicar, salvar dieta como modelo) · **MOB-57** aplicar modelo ao paciente | — | **Marco M9 (1.3.0)**: nutri prescreve a partir de um modelo e ajusta às metas |
| 22. Crianças e adolescentes | **BE-54** referências OMS 0–9 (P/I, E/I, IMC/I por mês, percentil e Z), Frisancho, IMC 10–17 · **BE-55** cálculo infantil e adolescente (classificações, CMB/AMBc/AGB, Slaughter, Deurenberg, GEB/GET IOM e RDI) | **MOB-58** avaliação infantil e adolescente · **MOB-59** curva de crescimento para nutri e responsável | **DES-10** criança, exames e anamnese | — |
| 23. Clínica e exames | **BE-56** catálogo de exames (faixas por sexo, idade, ciclo, IMC) e lançamento por avaliação · **BE-57** PA, comportamental e alimentar estruturados (no questionário padrão) | **MOB-60** exames (nutri lança, paciente vê barra de faixa se compartilhado) · **MOB-61** anamnese estruturada com os cálculos de álcool, sal e óleo | — | **Marco M10 (1.4.0)**: as 4 planilhas cobertas |

Tarefas sem dependência entre si na mesma coluna e onda podem ir em PRs paralelos (BE-43 e BE-44; MOB-47 e MOB-49). MOB-49 pode começar já, porque o backend já devolve os dados.

### 6.2 Backend

| Tarefa | O que faz | Pronto quando |
| --- | --- | --- |
| BE-C7 | Records `AvaliacaoResposta` ampliada (campos novos opcionais, bloco `resultados`, `protocolo`, `faixaEtaria`, `compartilhamento` por seção), `ProtocolosNutriResposta`, JSONs de exemplo, `CodigoErro` novos (`DADO_INSUFICIENTE_CALCULO`, `PROTOCOLO_INVALIDO`), vetores em `contrato/calculos/` (adulto: 3 e 7 dobras, idoso, casos de borda, seus dados), RN-CAL/RN-RES no doc | `/v3/api-docs` revisado; vetores com a célula de origem |
| BE-39 | Módulo `referencias` (só `api/` exposto): tabelas `referencia_*` com IMC adulto/idoso, risco, cintura, compleição, Pollock e Wilmore, PA; changeset com `fonte` e `versao_referencia` | Consultas por sexo/idade testadas |
| BE-40 | `avaliacoes/dominio/calculos`: porta das macros adultas (`POL_*`, `IMC_*`, `CLASS_*`, `DIAG_*`) como funções puras com `BigDecimal` e arredondamento documentado | 100% dos vetores verdes; com as correções da seção 3.6 já aplicadas (cada uma com vetor próprio) |
| BE-41 | Changeset `030-avaliacao-ampliada`: peso usual/desejado, punho, diâmetros, compleição, atividade, objetivo, menstruada, % desejado, protocolo, resultados calculados; o servidor calcula ao salvar e no `/sync`; texto-resumo; compartilhamento por seção; série de dobras na evolução | Teste de API, regra e isolamento; contrato antigo continua aceito |
| BE-42 | `GET/PUT /eu/nutri/protocolos` com padrões; FonteSync | Teste de isolamento |
| BE-43 | `avaliacoes/dominio/energia`: GEB, GET, VET e metas da seção 3.2, gestante/lactante; grava o escolhido | Vetores verdes |
| BE-44 | Tabela `referencia_met` (113 atividades) e cálculo por MET | Vetor da aba MET verde |
| BE-45 | Tabelas DRI (fibras, água, 13 minerais, 13 vitaminas) por sexo, idade, gestação, lactação | Vetores das macros `RDI_*` |
| BE-46 | PDF de avaliação no layout da Luciana: cabeçalho completo, relação peso × estatura, perímetros com cintura, dobras com somatório, composição completa, 4 gráficos, tabelas de referência, texto-resumo, rodapé com CRN | PDF comparado lado a lado com o da nutri |
| BE-C8 | Contrato da dieta por porções, produtos, metas e modelos; vetores da aba "Calcule a dieta" | — |
| BE-47 | Changeset: `grupo_alimento_porcao` (variantes de kcal por grupo), 3 grupos novos, 36 nutrientes por 100 g no catálogo base; importador a partir da aba Alimentos (script em `ops/`, gera o changeset; não roda em produção a partir do xlsm) | Perfil médio da porção igual ao `Alimentos!F1230` |
| BE-48 | Catálogo de produtos (411) por nutri com base comum; nutri cadastra os dela | Isolamento |
| BE-49 | Dieta ganha porção por grupo, metas (VET, faixas), refeições alternativas, itens "à vontade", rodapé, produtos; totais de micronutrientes e adequação; peso ajustado | Dieta de 2509 kcal bate |
| BE-50 | PDF da dieta com "Substituições: 1 porção do grupo X" e rodapé; PDF da lista de substituição (pequena, média, grande e com contagem de carboidratos) | Comparado com os PDFs da nutri |
| BE-51 | Marcadores `CONTEM_LACTOSE`, `CONTEM_GLUTEN`, `ALTO_SODIO`, `ACUCAR_ADICIONADO`, `ALTO_POTASSIO` no catálogo; a nutri ajusta nos alimentos dela | Filtro testado |
| BE-52 | `modelo_dieta` (dono = nutri ou sistema), CRUD, duplicar, salvar dieta como modelo, `POST /pacientes/{id}/dietas/de-modelo` com fator de escala e restrições; FonteSync | Isolamento; aplicar cria rascunho |
| BE-53 | Seed dos modelos iniciais (seção 6.3) marcados `[SUPOSIÇÃO]` até o G-15 | Modelos aparecem para nutri nova |
| BE-54 | Curvas OMS 0–5 e 5–10 anos por mês (peso, estatura, IMC; percentis e Z), Frisancho (CB, DCT, DCSE, AMBc, AGB), IMC 10–17 | Valores conferidos com as abas da planilha |
| BE-55 | Cálculos infantil e adolescente da seção 3.3 | Vetores verdes |
| BE-56 | Catálogo de ~150 exames (unidade, faixas por condição, rótulos) e `exame_resultado` por avaliação; não compartilhado por padrão | Isolamento; faixas iguais às da planilha |
| BE-57 | Questionário padrão "Anamnese completa" com as perguntas das abas clínica, alimentar e comportamental (o módulo `questionarios` já existe); cálculos de PA, álcool, sal e óleo no envio | Teste de regra |

### 6.3 Modelos iniciais de dieta (BE-53, conteúdo a validar no G-15)

| Modelo | Base | Restrições e diferenças |
| --- | --- | --- |
| Sem restrições | a planilha "Dieta sem restrições" | nenhuma |
| Hipertensão (DASH) | sem restrições | sódio ≤ 2000 mg/dia, sem embutido, mais vegetal e fruta, leite desnatado, marcador `ALTO_SODIO` fora da lista |
| Intolerância à lactose | sem restrições | grupo Leite com opções sem lactose, `CONTEM_LACTOSE` fora da lista |
| Diabetes tipo 2 | sem restrições | açúcar zero, carboidrato distribuído nas refeições, lista "com contagem de carboidratos" |
| Dislipidemia | sem restrições | metas SBC (saturado 7%, trans 1%, colesterol 200 mg), gordura e embutido reduzidos |
| Doença celíaca | sem restrições | `CONTEM_GLUTEN` fora da lista |
| Gestante | sem restrições | metas de gestante, + fibras e água |
| Obesidade (hipocalórica) | sem restrições | VET de perda, porções menores de Pão/Arroz/Açúcar, peso ajustado |
| Doença renal crônica (sem diálise) | sem restrições | proteína reduzida, `ALTO_POTASSIO` e `ALTO_SODIO` fora da lista |

### 6.4 Mobile

| Tarefa | O que faz | Pronto quando |
| --- | --- | --- |
| MOB-47 | `ui/comum/graficos`: `GraficoRosca`, `GraficoLinha` ampliado, `GraficoBarras`, `ReguaFaixas`, `Medidor`, `CurvaCrescimento`; previews, semântica e "ver como tabela" | Previews nos 3 tamanhos; teste de semântica |
| MOB-48 | `dominio/calculos` em Kotlin com os mesmos vetores (cópia de `contrato/calculos/`) | `jvmTest` verde com os vetores |
| MOB-49 | P0 do relatório: paciente vê perímetros, dobras, composição e parecer na tela, macros por refeição e dia, "1 porção do grupo X" no item | Funciona offline |
| MOB-50 | Avaliação em 4 passos (Medidas, Composição, Resumo) com prévia ao vivo, repetir medidas, avisos de dado faltante, painel lateral no tablet | Conferida com o DES-08 |
| MOB-51 | "Meus resultados": semáforos, rosca de composição, minha meta, evolução com faixas, sem siglas | Offline; só o compartilhado |
| MOB-52 | Passo "Necessidades": protocolos lado a lado, objetivo, VET, rosca de metas, micronutrientes | Vetores na prévia |
| MOB-53 | Tela "Meus protocolos" | — |
| MOB-54 | Planejador por porções (grade, metas fixas, rosca, barras por refeição, alternativas, à vontade, produtos, converter em alimentos); escrita offline como o montador | Dieta de 2509 kcal montada pela grade |
| MOB-55 | Paciente: rosca de macros do dia e da refeição; tela "Lista de substituição" para consulta, agrupada por grupo | — |
| MOB-56 | Biblioteca de modelos (sistema e meus; editar, duplicar, salvar dieta como modelo, restrições) | — |
| MOB-57 | Aplicar modelo: escolher, ver escala pelo VET, revisar no planejador | Cria rascunho |
| MOB-58 | Avaliação infantil e adolescente (campos e resultados por faixa) | — |
| MOB-59 | Curva de crescimento OMS para a nutri e o responsável | — |
| MOB-60 | Exames: lançar por avaliação (busca por nome, bloco), paciente vê barra de faixa | — |
| MOB-61 | Anamnese estruturada com cálculos de álcool, sal e óleo | — |

### 6.5 Design

| Tarefa | Telas |
| --- | --- |
| DES-08 | Avaliação em 4 passos (celular e tablet com painel de resultados), "Meus resultados" do paciente, componentes de gráfico (rosca, régua, medidor, linha com faixa) |
| DES-09 | Planejador por porções (celular uma refeição por vez, tablet grade), barra de metas, biblioteca e aplicação de modelos |
| DES-10 | Avaliação infantil com curva de crescimento, exames (nutri e paciente), anamnese estruturada |

### 6.6 Tarefas do Guilherme e da Luciana

| Tarefa | Quando | O quê |
| --- | --- | --- |
| G-15 Modelos do SUS | Antes da onda 21 | A Luciana manda os modelos que usa no SUS (ou revisa os da seção 6.3) e marca os alimentos com lactose/glúten/sódio alto |
| G-16 Aviso das correções | Antes do M8 | Mostrar à Luciana a lista da seção 3.6 (as correções já estão aprovadas e implementadas); se ela discordar de alguma, vira ajuste de propriedade no BE-40 |
| G-17 Licença das tabelas | Antes da onda 20 | Confirmar a origem da tabela de alimentos e de produtos (TACO/IBGE/rótulos) e se pode ser distribuída a outras nutris; se não, cada nutri importa a dela |
| G-18 Aprovar o plano | Agora | Dizer se a ordem das ondas está boa (em especial se crianças vêm antes das dietas pré-prontas) |

## 7. Marcos

| Marco | Versão | O que a nutri e o paciente ganham | Como conferir |
| --- | --- | --- | --- |
| M8 | 1.2.0 | Avaliação de adulto e idoso igual à planilha, com prévia ao vivo, texto-resumo, PDF com gráficos; paciente vê "Meus resultados" | Avaliação do Guilherme refeita no app gera os mesmos números do PDF |
| M9 | 1.3.0 | Necessidades e metas, dieta por porções contra as metas, suplementos, listas em PDF, modelos por condição editáveis | Dieta de 2509 kcal pela grade bate; modelo "Hipertensão" aplicado tira os embutidos da lista do paciente |
| M10 | 1.4.0 | Crianças e adolescentes com curvas da OMS, exames, anamnese estruturada | Caso de cada faixa etária com os vetores |

## 8. Riscos e decisões

- **Precisão:** a planilha calcula em ponto flutuante do Excel; o backend usa `BigDecimal`. Os vetores definem a tolerância (0,01 para kg, 0,1 para %).
- **Tamanho da sync:** 36 nutrientes × 522 alimentos e as curvas OMS são estáticos; vão por versão de referência (baixa uma vez), não pela fila de sync.
- **Responsabilidade clínica:** classificações e modelos são apoio; o texto do app diz que a conduta é da nutri. Nada é aplicado ao paciente sem ação dela.
- **Push e privacidade:** nenhum resultado clínico no texto do push (regra já existente); exames não compartilhados por padrão.

## 9. Como um agente pega a próxima tarefa

Igual ao `plano-de-execucao.md`, seção 7, a partir da onda 17. Antes de portar uma fórmula, abrir o anexo da planilha (`planos/anexos-planilhas/formulas-*.md`) e a macro (`macros-vba-*.txt`), criar o vetor de teste e só depois o código.
