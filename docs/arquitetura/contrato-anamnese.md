# Anamnese estruturada — BE-57b

Extensão aditiva de `/api/v1`, sem rotas novas. `PerguntaQuestionario.codigo` é opcional, único no modelo, até 80 caracteres no formato `[A-Z][A-Z0-9_]*`. O texto é editável; o código define a semântica e a unidade. Perguntas antigas sem código continuam aceitas. Código desconhecido é preservado e não calcula resultados.

| Código | Tipo | Unidade e limites |
| --- | --- | --- |
| PA_SISTOLICA, PA_DIASTOLICA | NUMERO | mmHg, inteiro de 1 a 1000 |
| ALCOOL_BEBIDA | ESCOLHA_UNICA | Absinto, Cachaça, Cerveja, Ice, Rum ou gim, Vinho, Vodca, Uísque |
| ALCOOL_UNIDADE | ESCOLHA_UNICA | Medidas abaixo, com volume no rótulo |
| ALCOOL_QUANTIDADE | NUMERO | Medidas por dia de consumo, zero permitido |
| ALCOOL_DIAS_SEMANA | NUMERO | Frequência semanal de 0 a 7; aceita fração |
| SAL_GRAMAS_MES | NUMERO | g no mês, zero permitido |
| SAL_PESSOAS | NUMERO | Inteiro de 1 a 1.000.000 |
| OLEO_ML_MES | NUMERO | ml no mês, zero permitido; converter litros para ml no cliente |
| OLEO_PESSOAS | NUMERO | Inteiro de 1 a 1.000.000 |
| ATIVIDADE_INICIO, ATIVIDADE_FIM | HORARIO | HH:mm, duração atravessa meia-noite |

O modelo completo usa uma linha por bebida da planilha: `ALCOOL_<BEBIDA>_UNIDADE`, `ALCOOL_<BEBIDA>_QUANTIDADE`, `ALCOOL_<BEBIDA>_DIAS_SEMANA`, onde `<BEBIDA>` é ABSINTO, CACHACA, CERVEJA, ICE, RUM_GIM, VINHO, VODCA ou UISQUE. Tipos/unidades/limites são os mesmos das entradas genéricas da tabela. `alcool` soma as bebidas informadas e completas. Se qualquer linha informada estiver parcial, o total é nulo até completá-la ou apagar todas as suas entradas; linhas totalmente não informadas não entram na soma. As entradas genéricas continuam disponíveis para modelos personalizados.

Para a semana, `ATIVIDADE_<DIA>_INICIO` e `ATIVIDADE_<DIA>_FIM`, com DIA = SEGUNDA, TERCA, QUARTA, QUINTA, SEXTA, SABADO ou DOMINGO. `atividadesDuracaoMinutos` mapeia somente os dias com ambos os horários para a duração calculada. Cada dia tem um intervalo e uma pergunta de texto para exercícios/frequência; a tabela de múltiplas linhas da planilha foi adaptada aos tipos de pergunta existentes. Intervalos incompletos não entram no mapa. O campo singular `atividadeDuracaoMinutos` atende as perguntas genéricas da tabela.

O limite geral dos demais números é 1.000.000.000. Códigos conhecidos não admitem tipos incompatíveis; as duas escolhas admitem somente opções reconhecidas (a nutri pode selecionar um subconjunto de ao menos duas). Erro `FORMATO_INVALIDO` com campo da pergunta, também no sync.

Medidas: Cálice (80 ml), Caneca (450 ml), Copo americano (140 ml), Copo duplo (200 ml), Dose (50 ml), Garrafa grande (600 ml), Garrafa long neck (355 ml), Latinha (355 ml), Taça média (120 ml), Taça pequena (80 ml), Tulipa (280 ml), Tulipinha (180 ml). Teores e fórmulas são os já versionados no BE-57a em `contrato/calculos/anamnese.json`.

`QuestionarioResposta.derivados` é opcional, somente leitura, entregue nas leituras de nutri/paciente, REST e sync. A gravação parcial mescla respostas, recalcula e persiste os resultados na mesma transação; apagar uma entrada ou ocultá-la pela condição limpa o respectivo resultado. Campos faltantes não valem zero. O envio final recalcula e congela os resultados; alterações posteriores no modelo não mudam perguntas, códigos, respostas nem resultados do questionário enviado/respondido. Dados anteriores à extensão têm `derivados=null` até a próxima gravação/conclusão válida; questionários já concluídos não são recalculados retroativamente.

```json
{
  "derivados": {
    "pressaoArterial": "HIPERTENSAO_SISTOLICA_ISOLADA",
    "alcool": { "mlPorSemana": 3195.00, "gramasEtanolPorSemana": 127.80, "kcalPorSemana": 894.60 },
    "salGramasPorPessoaDia": 7,
    "oleoMlPorPessoaDia": 16,
    "oleoKcalPorPessoaDia": 144,
    "atividadeDuracaoMinutos": 45,
    "atividadesDuracaoMinutos": { "SEGUNDA": 45 }
  }
}
```

Exemplo sintético: PA 140/80; cerveja, 3 latinhas de 355 ml em 3 dias/semana; sal 650 g/mês para 3 pessoas; óleo 2000 ml/mês para 4 pessoas; atividade 23:30–00:15. Sal e óleo truncam para inteiro como no BE-57a; não reproduzem os decimais dos exemplos dos artboards. Sem entradas suficientes, cada resultado respectivo é `null` (inclusive o bloco álcool inteiro).

O modelo “Anamnese completa” nasce uma vez por nutri no primeiro acesso aos modelos. Tem os 12 blocos da anamnese existente, clínica e hábitos ampliados, oito bebidas, atividade nos sete dias e frequência alimentar com os 20 grupos da fonte. Os cálculos são opcionais, sem conduta automática. Cópias antigas/editadas são preservadas; uma origem interna estável impede duplicação mesmo após renomear a nova cópia. Novas contas recebem o completo como padrão; contas com padrão anterior recebem uma cópia adicional editável.

Fonte conferida em modo de leitura: arquivos locais “18 anos acima - avaliação completa alterada.xlsm”, “10 a 17 anos - avaliação completa.xlsm” e “0 a 9 anos - avaliação completa.xlsm”, fornecidos em `Documento dieta`. Apenas rótulos foram usados, sem respostas, cadastro ou dados clínicos reais. As três abas `av. alimentar!A93:A112` têm **20 grupos**, e `B92:H92` tem sete frequências (mais de uma vez/dia até raro ou nunca). A estimativa de 30 grupos no plano não corresponde aos arquivos fornecidos; nenhum grupo adicional foi inventado. Clínica: `av. clínica!A17:H41`. Alimentar: `av. alimentar!A10:H38`, recordatório e frequência. Comportamento: `av. comportamental!A14:F26` e atividade por dia da semana a partir da linha 28. O modelo preserva também os campos prévios de objetivo, alergias, preferências e saúde da mulher. O visual do MOB-61 deve usar os volumes e resultados do contrato, em vez dos números ilustrativos dos artboards. As planilhas não foram alteradas nem copiadas para os repositórios.
