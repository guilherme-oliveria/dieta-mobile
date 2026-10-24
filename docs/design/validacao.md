# Validação do canvas de design

Data: 24/10/2026. Canvas: https://claude.ai/artifact/6thej5zGsTo81ZosiigGxC (13 artboards). Comparado com o doc do MVP ("Dieta+ — Análise dos PDFs e proposta inicial") e com as decisões de produto já tomadas.

## Resumo

O canvas cobre bem o coração do MVP: o paciente vê a dieta do dia, troca alimento pelo grupo, gera lista de compras, recebe lembretes e responde a anamnese; a nutri lista pacientes, abre a ficha, monta dieta e registra avaliação. O visual é coerente e cabe no Material 3 do Compose sem esforço.

Faltam três coisas para dar para implementar sem adivinhar:

1. As telas de entrada de conta (cadastro, convite, consentimento LGPD, recuperar senha) e de conta/assinatura da nutri.
2. Os estados que não são "tudo certo": carregando, vazio, erro, sem internet, sincronizando, plano no limite.
3. O desktop, que ainda não tem nenhum artboard. A decisão abaixo é reusar os layouts de tablet com ajustes de janela e teclado.

## O que está bom e fica como está

- Paleta curta (verde de marca, fundo creme, laranja só para alerta) e uma fonte (Plus Jakarta Sans). Fácil de virar tokens.
- Navegação inferior de 5 abas no paciente e de 4 abas na nutri no celular; navigation rail no tablet. Bate com o contrato de WindowSizeClass da arquitetura.
- Botões "Comi / Troquei / Pulei" na própria refeição alimentam o painel de adesão da nutri. Entra no MVP (ver suposições).
- Troca de alimento mostra só o que está na lista do paciente e já na porção certa, exatamente a regra do doc.
- Montador de dieta no tablet com lista de refeições, itens com porções e a lista do paciente ao lado (alimento riscado por alergia) é o desenho certo para tela larga.
- Avaliação deixa claro que no MVP só o IMC é calculado.
- Acessibilidade bem encaminhada: botões reais, `aria-label` em ícones, `fieldset/legend`, abas com `tablist`.

## O que corrigir antes de implementar

| # | Onde | Problema | Correção |
| --- | --- | --- | --- |
| 1 | `ui/theme/Color.kt` | As cores do código não batem com o canvas: texto `#1C1F1A` (canvas `#17251D`), secundário `#5B6157` (canvas `#5B6B61`), verde claro `#D7EBDD` (canvas `#E6F2EA`). Faltam borda, verde escuro, alerta claro e azul de informação | Trocar pelos tokens de [componentes.md](componentes.md) |
| 2 | Todas | Laranja `#C2551F` como texto sobre o fundo `#F5F6F1` dá contraste 4,19:1 (falha AA) | Texto de alerta usa `#9A3F14` (5,9:1 sobre `#FBEDE3`); `#C2551F` só em ícone, ponto e borda |
| 3 | Compras | Item marcado em `#7A877E` riscado tem contraste 3,75:1 | Usar `#66736A` (≥4,5:1) mantendo o risco |
| 4 | Ficha (celular) | Barras de adesão verde e laranja-claro se distinguem só pela cor | Mostrar o % em cima da barra e usar altura; laranja claro mais escuro (`#C98A63`) |
| 5 | Hoje, Pacientes | Linhas com altura fixa (46 e 64 px) quebram com fonte grande do sistema | `heightIn(min = …)` em vez de altura fixa; testar com escala de fonte 200% |
| 6 | Hoje | A 5ª aba se chama "Perfil" mas abre "Lembretes" | A aba Perfil abre a tela Perfil (nova), que tem Lembretes, Minhas nutris, Dados e privacidade e Sair |
| 7 | Nutri celular × tablet | Celular tem Pacientes/Agenda/Alimentos/Conta; tablet tem Início/Pacientes/Agenda/Alimentos/Conta. No celular, "Agenda" abre o Painel | Mesmas 5 entradas nas duas: Início, Pacientes, Agenda, Alimentos, Conta. No celular "Início" é o painel em coluna |
| 8 | Entrar | "Sou nutricionista" leva direto à lista de pacientes | Leva ao cadastro da nutri (CRN, teste com 5 pacientes) |
| 9 | Anamnese | O "X" volta para Evolução, mas a anamnese é aberta pelo aviso de Hoje/Evolução/push | Fechar volta para a tela de origem (pilha de navegação) e mantém "salvo automaticamente" |
| 10 | Montar dieta | "Publicar para o paciente" não tem confirmação nem mostra a diferença para a dieta anterior | Diálogo de publicação: resumo (kcal, refeições, itens que mudaram) e aviso de que o paciente recebe push |
| 11 | Todas | Nenhum estado de carregando, vazio, erro ou sem internet | Ver componentes `EstadoVazio`, `EstadoErro`, `FaixaOffline`, `IndicadorSync` |
| 12 | Todas | Sem modo escuro | MVP só tema claro; tokens já nomeados para receber o escuro depois |

## Telas que faltam

Ordem por fase em [plano-de-acao-ui.md](plano-de-acao-ui.md); pacote de cada uma em [telas.md](telas.md).

Entrada e conta (MVP):

- Cadastro do paciente pelo código ou link da nutri, com aceite de consentimento LGPD (dados de saúde) e de termos.
- Cadastro da nutri (nome, CRN, e-mail, senha) começando no teste de 5 pacientes.
- Recuperar senha (e-mail com código).
- Escolher perfil, quando a mesma conta é nutri e paciente.

Paciente (MVP):

- Perfil: dados pessoais, minhas nutris, notificações, dados e privacidade (exportar, excluir), sair.
- Minhas nutris: lista de vínculos, trocar a nutri ativa, aceitar novo vínculo com autorização para copiar o histórico.
- Central de avisos (o sino da tela Hoje).
- Registrar peso em casa (o botão de Evolução abre uma folha inferior).
- Água: tocar no card de água soma um copo; detalhe com meta de 35 ml/kg.
- Orientações gerais da nutri (texto padrão dela).
- Ver e compartilhar o PDF da dieta.
- Primeira abertura sem dieta publicada ("sua nutri está montando sua dieta").

Nutri (MVP):

- Painel no celular (versão em coluna do painel do tablet).
- Convidar paciente: nome, e-mail ou telefone, gera código e link; mostra o limite do plano.
- Ficha do paciente no tablet (lista-detalhe: pacientes à esquerda, ficha à direita).
- Agenda de consultas (dia e semana), criar e remarcar retorno.
- Alimentos: a lista de substituição da nutri por grupo (adicionar, tirar, ajustar medida).
- Anamnese: editar o modelo de perguntas e ler as respostas de um paciente.
- Histórico de avaliações e comparação entre duas datas.
- Conta e assinatura: plano, pacientes ativos x limite, status (teste, ativa, em atraso, cancelada), link de pagamento, dados do perfil (CRN, logo).
- Bloqueio de limite: ao convidar o 21º paciente (ou 6º no teste), folha explicando o limite com o botão de assinatura.
- Modo leitura quando a assinatura está em atraso depois da carência.

Desktop (MVP, perfil nutri):

- Mesmas telas da nutri no layout expandido, com barra de status de sincronização e diálogo de conflito.
- Primeiro acesso: baixar os dados do consultório para uso sem internet.

Depois do MVP: foto do prato, chat com a nutri, resumo semanal, vitrine de nutris, consultoria paga, site Angular (o canvas já tem nota disso), modo escuro.

## Suposições (decididas sem perguntar ao Guilherme)

1. Check-in de refeição (Comi/Troquei/Pulei) entra no MVP, porque o painel e a ficha da nutri dependem da adesão. Foto do prato fica para depois.
2. Desktop é para a nutri. O paciente consegue entrar no desktop, mas usa o layout de tablet sem trabalho extra de design.
3. A contratação do plano não é vendida dentro do app: a tela de assinatura abre o checkout do gateway no navegador. Regras da Apple e do Google sobre isso precisam ser conferidas antes de publicar.
4. A fonte Plus Jakarta Sans vai empacotada em `composeResources/font` (funciona offline no desktop).
5. Material 3 nas três plataformas, com os ajustes de iOS de [plataformas.md](plataformas.md). Sem telas nativas em SwiftUI.
6. Tema claro só no MVP.
7. Os textos e números do canvas são exemplos e não regra; regras (limite de pacientes, carência, cálculo do IMC) vêm de `docs/arquitetura/`.
