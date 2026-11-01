# Dieta+ — regras de negócio

> Documento comum aos 3 repositórios. Cópia oficial em `dieta-backend/docs/arquitetura/`.
> **Quem aplica é o backend.** Os clientes (mobile e web) repetem as validações só para dar feedback rápido, nunca como garantia.
> Cada regra tem um ID (`RN-<área>-<n>`). Cite o ID no código (comentário curto), no teste e no PR.
> Quando uma regra é violada, o backend responde com o `codigo` de erro indicado (ver `integracao.md`, seção Erros).

## Perfis

| Perfil | Quem é | Vê |
| --- | --- | --- |
| `ADMIN` | Equipe da plataforma | Planos, assinaturas, métricas. **Não** vê dados clínicos. |
| `NUTRICIONISTA` | Nutri que assina | Os próprios pacientes vinculados e tudo que criou |
| `PACIENTE` | Pessoa atendida | Só os próprios dados, as dietas publicadas para ele e suas nutris |

Um usuário pode ter os perfis `NUTRICIONISTA` e `PACIENTE` na mesma conta (decisão de BE-05 no plano de execução). Cada sessão usa um perfil ativo, escolhido no login; `GET /eu` informa `perfis` disponíveis e `perfil` ativo. A abreviação "NUTRI" no plano é descritiva: o valor no JSON e no JWT é sempre `NUTRICIONISTA`. O perfil `ADMIN` continua restrito à equipe da plataforma.

## Contas e acesso (RN-CON)

- **RN-CON-01** E-mail é único na plataforma e é o login. Guardado em minúsculas, sem espaços.
- **RN-CON-02** Senha: mínimo 10 caracteres, não pode estar na lista de senhas vazadas comuns. Guardada só como hash Argon2id (bcrypt aceito como alternativa).
- **RN-CON-03** Após 5 tentativas de login erradas em 15 minutos, aquele e-mail fica bloqueado por 15 minutos. Resposta não revela se o e-mail existe. Código `LOGIN_BLOQUEADO`.
- **RN-CON-04** Cadastro de nutri exige nome, e-mail, senha, CRN (formato `CRN-<região> <número>`, ex.: `CRN-1 8814`) e aceite dos termos. A conta já nasce com uma `organizacao` do tipo `INDIVIDUAL` e uma assinatura no plano `TESTE`.
- **RN-CON-05** E-mail precisa ser confirmado (link com validade de 24 h) antes de a nutri convidar o primeiro paciente. Código `EMAIL_NAO_CONFIRMADO`.
- **RN-CON-06** Paciente só cria conta a partir de um convite (no MVP). Cadastro avulso de paciente é P2 (vitrine).
- **RN-CON-07** Recuperação de senha por link de uso único, validade 1 h. Trocar a senha revoga todos os refresh tokens do usuário.
- **RN-CON-08** CRN não é validado automaticamente no MVP; o admin pode marcar `crn_verificado`. `[SUPOSIÇÃO]`
- **RN-CON-09** Cadastro de conta (nutri ou paciente convidado) exige aceite explícito da versão de termos de uso aprovada **e** publicada. A fonte interna é `docs/legal/terms-of-use.md`, inicialmente versão `1.0.0`, `RASCUNHO`, sem aprovação/publicação presumidas. Sem ambas, o cadastro fica indisponível (`TERMOS_INDISPONIVEIS`); aceite ausente/negado ou versão divergente retorna `TERMOS_NAO_ACEITOS`. O backend registra userId, versão e instante do Clock na mesma transação da conta. Consentimento clínico do vínculo é independente. Login, refresh, recuperação e operações de contas existentes continuam normais.

## Assinatura e plano (RN-ASS)

- **RN-ASS-01** Quem assina é a `organizacao` (no MVP, a própria nutri). Planos ficam na tabela `plano` (preço, limite de pacientes, duração do teste). Mudar preço não exige deploy.
- **RN-ASS-02** Plano `TESTE`: até 5 pacientes ativos, por 30 dias `[SUPOSIÇÃO S1]`. Ao expirar, status `EXPIRADA`: a nutri só lê.
- **RN-ASS-03** **Limite de pacientes**: conta como ocupado todo vínculo em `CONVIDADO` ou `ATIVO`. Criar convite acima do limite é recusado com `LIMITE_PACIENTES_ATINGIDO` (o 6º no Teste, o 21º no Básico). Encerrar um vínculo libera a vaga.
- **RN-ASS-04** Status da assinatura: `TESTE`, `ATIVA`, `EM_ATRASO`, `CANCELADA`, `EXPIRADA`. Em `EM_ATRASO` a nutri continua editando por 7 dias de carência `[SUPOSIÇÃO S2]`; depois disso, e em `CANCELADA` ou `EXPIRADA`, a nutri fica **só leitura** (`ASSINATURA_INATIVA` em qualquer escrita).
- **RN-ASS-05** O paciente **nunca** é punido pela assinatura da nutri: continua vendo a última dieta publicada, recebendo lembretes e fazendo check-in.
- **RN-ASS-06** O app não vende nada. A contratação acontece no checkout hospedado do gateway (link enviado por e-mail e, no pós-MVP, pelo site) `[SUPOSIÇÃO S4]`.
- **RN-ASS-07** O status só muda por: webhook do gateway (validado pela assinatura/token do gateway e processado uma única vez por `evento_id`), job diário de expiração, ou ação do admin (com log).
- **RN-ASS-08** Downgrade para um plano com limite menor que os pacientes ativos é permitido, mas novos convites ficam bloqueados até ficar abaixo do limite. Nenhum vínculo é encerrado automaticamente.

## Vínculo nutri–paciente (RN-VIN)

- **RN-VIN-01** O paciente é uma **conta global**. A nutri não "possui" o paciente; ela possui o **vínculo** e o **prontuário** (avaliações, dietas, anamneses) que criou.
- **RN-VIN-02** A nutri cria a ficha do paciente (nome, e-mail opcional, nascimento, sexo) e gera um **convite**: código de 8 caracteres (sem 0/O/1/I) + link, validade 7 dias, uso único `[SUPOSIÇÃO S5]`. Reenviar gera um novo código e invalida o anterior.
- **RN-VIN-03** Se já existe usuário paciente com aquele e-mail, o convite é ligado a ele; se não, o paciente cria a conta ao aceitar.
- **RN-VIN-04** **Aceite = consentimento LGPD.** O paciente vê o termo (quem é a nutri, quais dados ela acessa, para quê) e aceita; gravamos `consentimento_em`, versão do termo e IP. Sem aceite o vínculo fica `CONVIDADO` e a nutri pode preencher a ficha, mas o paciente não recebe nada.
- **RN-VIN-05** Status do vínculo: `CONVIDADO` → `ATIVO` → `ENCERRADO`. Qualquer um dos dois pode encerrar. Encerrado: a nutri mantém o prontuário (só leitura) e o paciente mantém o acesso às dietas que já recebeu.
- **RN-VIN-06** Um paciente pode ter vínculos ativos com várias nutris. Cada nutri só vê o que ela mesma criou. Cópia de histórico para a nova nutri (com autorização do paciente) e a escolha da antiga de manter ou excluir é P2 `[SUPOSIÇÃO S8]`.
- **RN-VIN-07** Não existe dois vínculos `ATIVO`/`CONVIDADO` entre a mesma nutri e o mesmo paciente. Código `VINCULO_JA_EXISTE`.

## Alimentos e lista de substituição (RN-ALI)

- **RN-ALI-01** Três níveis, cada um começa como cópia do anterior:
  1. **Catálogo base** da plataforma (12 grupos com kcal por porção, alimentos com medida caseira e gramas), mantido pelo admin. Carga inicial a partir do PDF "Lista de substituição".
  2. **Lista da nutri**: criada como cópia do catálogo no cadastro; ela adiciona, remove e ajusta.
  3. **Lista do paciente**: ajustes por paciente (ex.: remover lactose, adicionar algo que ele gosta), guardados como diferenças sobre a lista da nutri.
- **RN-ALI-02** Grupos do catálogo inicial e kcal por porção: Pão 150, Carne ou ovo 200, Arroz 80, Fruta 70, Leite 55, Feijão 45 e os demais conforme o PDF. Um alimento pertence a um grupo e define quanto equivale a **1 porção** (medida caseira + gramas).
- **RN-ALI-03** Alimento usado em dieta publicada não é apagado: é desativado (some das novas dietas, continua nas antigas).
- **RN-ALI-04** Mudança no catálogo base não altera listas de nutris que já existem; o admin pode oferecer "atualizações do catálogo" que a nutri aceita (P2).

## Plano alimentar / dieta (RN-DIE)

- **RN-DIE-01** Estrutura: dieta → refeições (nome, horário, ordem) → itens (alimento de referência, quantidade em porções do grupo, medida caseira, observação).
- **RN-DIE-02** Estados: `RASCUNHO` → `PUBLICADA` → `SUBSTITUIDA` (quando outra é publicada) ou `ARQUIVADA`. Só pode existir **uma** `PUBLICADA` por par nutri–paciente.
- **RN-DIE-03** **Publicar congela.** Na publicação o backend grava um snapshot imutável da dieta e da lista de substituição do paciente naquele momento. Mudanças futuras nas listas não mudam a dieta publicada.
- **RN-DIE-04** Dieta publicada não é editada. "Editar" cria um novo rascunho copiado dela; publicar o novo substitui o anterior.
- **RN-DIE-05** Totais (kcal, carboidratos, proteínas, gorduras por refeição e no dia) são calculados pelo backend a partir das porções; o cliente pode mostrar uma prévia com a mesma fórmula, mas o valor gravado é o do backend.
- **RN-DIE-06** Uma refeição sem itens não é publicada (some o "0 kcal" dos PDFs). Dieta sem nenhuma refeição com itens é recusada: `DIETA_VAZIA`.
- **RN-DIE-07** Paciente com várias nutris escolhe a "dieta em uso"; padrão é a publicada mais recente `[SUPOSIÇÃO S9]`. Lembretes e lista de compras usam a dieta em uso.
- **RN-DIE-08** Publicar uma dieta gera push "Nova dieta" ao paciente (se o vínculo está `ATIVO`) e atualiza o feed .ics.
- **RN-DIE-09** Orientação geral (texto padrão da nutri, ex.: hidratação 35 ml/kg) é anexada à dieta na publicação, também congelada.
- **RN-DIE-10** A porção de cada grupo (Pão 70 ou 150 kcal etc.) é escolhida **uma vez por dieta** e vale para todos os itens por grupo; cada item traz exatamente uma origem (alimento, grupo ou produto). Publicar congela as porções escolhidas junto com a dieta (RN-DIE-03).
- **RN-DIE-11** Refeições `ALTERNATIVA` (ex.: lanche 2ª opção) mostram o próprio total e a diferença para a refeição que substituem, e **não** entram no total do dia.
- **RN-DIE-12** Item "à vontade" não tem quantidade nem kcal; o rodapé ("Além das refeições acima…") é texto da nutri, congelado na publicação.
- **RN-DIE-13** Total de micronutriente soma só os itens que informam o nutriente; os demais ficam listados em `incompleto`. Nunca vira zero por falta de dado (RN-CAL-04). Os valores por porção dos grupos são as médias da planilha da Nut. Luciana e são provisórios; o app e o PDF citam a fonte.

## Substituição pelo paciente (RN-SUB)

- **RN-SUB-01** O paciente só troca por alimentos **do mesmo grupo** que estejam na **lista dele congelada** na dieta.
- **RN-SUB-02** A quantidade do substituto é calculada para manter as mesmas porções: `quantidade = porções do item × medida de 1 porção do substituto`.
- **RN-SUB-03** A troca não altera a dieta; vira um check-in `TROCADO` com o alimento escolhido.

## Rotina do paciente (RN-ROT)

- **RN-ROT-01** Check-in por refeição e por dia: `FEITO`, `TROCADO` (com alimento) ou `PULADO`. Um check-in por refeição/dia; o último vale. Pode ser feito até 3 dias depois.
- **RN-ROT-02** Check-ins são do paciente, mas a nutri do vínculo ativo os vê (painel de adesão). São só de inclusão: nunca geram conflito de sync.
- **RN-ROT-03** Água: meta padrão = 35 ml × peso mais recente (kg); a nutri pode sobrescrever na dieta. Registros de água e peso em casa seguem a mesma regra de inclusão.
- **RN-ROT-04** Lista de compras: gerada no aparelho a partir da dieta em uso para N dias (padrão 7), somando gramas por alimento de referência. Itens marcados ficam só no aparelho no MVP.
- **RN-ROT-05** Lembretes de refeição são **locais** (agendados no aparelho a partir da dieta em uso) e funcionam sem internet. Respeitam horário de silêncio e cada tipo pode ser desligado.

## Avaliação (RN-AVA)

- **RN-AVA-01** Avaliação = data/hora + peso, estatura, 9 perímetros, 7 dobras, resultados de composição (% gordura, peso gordo, peso magro) e parecer em texto. Campos de medida são opcionais, exceto peso e estatura.
- **RN-AVA-02** No MVP o backend **calcula só o IMC** (`peso / estatura²`, 2 casas) e a classificação OMS. % gordura e afins são digitados pela nutri. Fórmulas da planilha (7 dobras, Pollock e Wilmore) são P2 e devem bater com a planilha em testes.
- **RN-AVA-03** Limites de sanidade: peso 20–400 kg, estatura 0,5–2,5 m, dobras 1–80 mm, perímetros 10–250 cm. Fora disso: `VALOR_FORA_DO_INTERVALO`.
- **RN-AVA-04** Avaliação pode ser editada pela nutri por 30 dias; depois só com justificativa registrada no log. `[SUPOSIÇÃO]`
- **RN-AVA-05** O paciente vê as avaliações que a nutri marcar como "compartilhada" (padrão: sim) e o gráfico de evolução.

- **RN-AVA-06** `[planilhas]` Avaliação ampliada: campos novos (punho, diâmetros, pesos usual e desejado, % desejado, atividade, objetivo) são opcionais, e o contrato antigo continua aceito.

## Cálculos da avaliação (RN-CAL) e resultados (RN-RES)

Origem: plano das planilhas da nutri (`plano-melhorias-planilhas.md`). Substituem o "só IMC" da RN-AVA-02 à medida que BE-40 e seguintes entram.

- **RN-CAL-01** O backend calcula e grava todos os resultados derivados (composição, classificações, GEB/GET, VET, metas, texto-resumo). O cliente mostra prévia com as mesmas fórmulas; o valor gravado é o do backend (como RN-DIE-05).
- **RN-CAL-02** A faixa etária da avaliação é decidida pela idade na data: 0–9, 10–17, 18–59, 60+.
- **RN-CAL-03** Cada avaliação grava o protocolo e a `versaoReferencia` usados. Recalcular só acontece se a nutri pedir. Protocolo desconhecido: `422 PROTOCOLO_INVALIDO`.
- **RN-CAL-04** Resultado que depende de dado ausente fica nulo, com o motivo (ex.: `FALTA_PUNHO`), nunca zero. `DADO_INSUFICIENTE_CALCULO` só quando a nutri pede um cálculo explícito sem os dados.
- **RN-CAL-05** Peso para "por kg" na dieta: adulto com IMC ≥ 30 usa o peso ajustado `(P − Pideal) × 0,25 + Pideal`, com `Pideal = E² × 21,7`.
- **RN-CAL-06** Vetores de teste da planilha (`src/test/resources/contrato/calculos/`) são contrato: mudança de fórmula exige mudar o vetor nos dois repositórios.
- **RN-CAL-07** Total da dieta por porções = Σ (porções × perfil da porção do grupo) + Σ (gramas ÷ 100 × perfil do produto); a soma usa os perfis sem arredondar e grava 2 casas em HALF_UP. Os vetores `contrato/calculos/dieta-porcoes.json` são contrato nos dois repositórios.
- **RN-MOD-01** Modelos do sistema são somente leitura; a nutri trabalha com cópias dela, isoladas por `nutricionista_id` (de outra nutri = 404). Editar ou apagar um modelo do sistema é `403 ACESSO_NEGADO`; ela o copia para editar.
- **RN-MOD-02** Aplicar modelo cria rascunho (RN-DIE-02); nunca publica sozinho. As porções são escaladas pelo alvo de energia do paciente (arredondando para 0,5, mínimo 0,5).
- **RN-MOD-03** Restrição do modelo vira restrição do paciente na dieta publicada e filtra a lista dele no snapshot (RN-DIE-03).
- **RN-RES-01** O paciente vê resultados por seção conforme o compartilhamento da nutri; exames são não compartilhados por padrão.
- **RN-RES-02** Textos para o paciente não usam siglas clínicas.

## Anamnese (RN-ANA)

- **RN-ANA-01** A nutri tem modelos de questionário (blocos e perguntas). Toda nutri começa com o modelo padrão (objetivo, histórico de saúde, familiar, alergias, preferências, treino, rotina, recordatório 24 h, frequência alimentar, hábitos, digestão, saúde da mulher).
- **RN-ANA-02** Tipos de pergunta: texto, número, escolha única, múltipla escolha, escala 1–5, horário, lista por refeição. Pergunta pode ter condição de exibição (ex.: "Saúde da mulher" só se sexo feminino).
- **RN-ANA-03** A nutri envia um questionário ao paciente; o paciente responde no app (pode salvar parcial). Ao enviar, as respostas ficam imutáveis e a nutri é notificada.
- **RN-ANA-04** As perguntas são congeladas no envio (mudar o modelo não muda questionários já enviados).
- **RN-ANA-05** Horários de rotina das respostas podem pré-preencher os horários das refeições de um novo rascunho de dieta.

## Agenda de consultas (RN-AGE)

- **RN-AGE-01** A nutri agenda consultas (data, hora, duração, tipo: primeira, retorno, online) para pacientes vinculados.
- **RN-AGE-02** Lembrete de consulta: push + e-mail 24 h antes, para paciente e nutri.

## Notificações (RN-NOT)

- **RN-NOT-01** Push só para: nova dieta, questionário enviado/respondido, consulta amanhã, paciente sem check-in há 3 dias (para a nutri), assinatura vencendo/recusada (para a nutri). O resto é local no aparelho.
- **RN-NOT-02** Push nunca leva dado clínico no texto (ex.: "Você tem uma nova dieta", nunca "Dieta de 2509 kcal").
- **RN-NOT-03** Cada usuário liga/desliga cada tipo e define horário de silêncio (padrão 22:00–07:00).

## LGPD e auditoria (RN-LGPD)

- **RN-LGPD-01** Todo acesso de leitura ou escrita a dado clínico grava `log_acesso` (quem, o quê, quando, de onde). Guardado por 5 anos.
- **RN-LGPD-02** O paciente pode exportar todos os seus dados (JSON + PDF) a qualquer momento.
- **RN-LGPD-03** O paciente pode pedir exclusão da conta: os dados pessoais são anonimizados; o prontuário de cada nutri é mantido pelo prazo legal, desvinculado da identidade quando a lei permitir.
- **RN-LGPD-04** Exclusão de prontuário pela nutri é lógica (soft delete) até confirmar a retenção exigida pelo CFN `[SUPOSIÇÃO S6]`.
- **RN-LGPD-05** Admin da plataforma não acessa dado clínico pela API. Suporte que precise disso exige autorização registrada do titular.

## Sincronização offline (RN-SYN)

Detalhes técnicos em `integracao.md`. Regras:

- **RN-SYN-01** Todo registro sincronizável tem `id` UUID gerado pelo cliente (UUIDv7), `versao` (inteiro, incrementado pelo backend a cada escrita), `atualizado_em` e `excluido_em` (exclusão lógica).
- **RN-SYN-02** Escrita offline leva a `versao` em que foi baseada. Se a versão no servidor for outra, o backend **não sobrescreve**: devolve `CONFLITO_VERSAO` com o registro atual, e o app pergunta à nutri qual manter.

  Na leitura, `versao` do envelope é revisão monotônica da projeção no diário; a versão da entidade está em `dados.versao`, usada em `If-Match`. ENCERRADO conserva histórico autorizado por RN-VIN-05; tombstone representa perda real de visibilidade, não mero encerramento.
- **RN-SYN-03** Registros só de inclusão (check-in, água, peso em casa, log) nunca conflitam.
- **RN-SYN-04** Regras de negócio valem igual no sync: uma mutação offline que viole uma regra (ex.: limite de pacientes) é rejeitada naquele item com o código da regra, sem derrubar o lote.
- **RN-SYN-05** O paciente não edita nada da nutri, então só sincroniza escrita dos próprios registros de rotina e respostas de anamnese.
