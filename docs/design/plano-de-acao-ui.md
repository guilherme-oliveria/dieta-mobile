# Plano de ação de UI

Para pessoas e agentes (Claude, Codex). Cada tarefa é um PR pequeno. Uma tarefa só começa quando a anterior da mesma fase está mergeada, salvo quando diz "em paralelo".

Dependências de dados (API, banco local, sync) estão em `docs/arquitetura/`. Enquanto o endpoint não existir, a tela usa um repositório falso em `commonMain` com os dados de exemplo dos PDFs, e o PR diz isso.

## Critério de pronto de toda tarefa de tela

- Segue o artboard (se desenhada) e os tokens de [componentes.md](componentes.md).
- Tem `@Preview` com dados de exemplo nos tamanhos compacto e expandido.
- Tem os estados carregando, vazio, erro e, se lê dado local, sem internet.
- Textos em `strings.xml`; nenhum hex ou dp solto fora de `ui/theme`.
- Funciona com fonte 200% e com leitor de tela (rótulos e ordem).
- `./gradlew :shared:jvmTest :shared:testAndroidHostTest` e o build do desktop passam; teste de ViewModel para a lógica da tela.
- Atualiza o status em [telas.md](telas.md).

## Fase 0 · Base visual (antes de qualquer tela)

1. Corrigir `ui/theme/Color.kt` com todos os tokens de [componentes.md](componentes.md) e mapear no `ColorScheme`.
2. Adicionar Plus Jakarta Sans em `composeResources/font` e `Typography` do tema.
3. `Dimens` (espaçamentos, raios, alturas mínimas) e `Shapes` no tema.
4. Ícones em `ui/comum/icones` (casa, talheres, carrinho, gráfico, pessoa, sino, água, cadeado, calendário, lista, mais, voltar, fechar, busca).
5. Componentes de `ui/comum` da tabela, com preview de cada variante, incluindo `EstadoVazio`, `EstadoErro`, `FaixaOffline`.
6. Esqueleto de navegação: `AppScaffold` que escolhe `BarraNavegacaoInferior` ou `TrilhoNavegacao` pela `WindowSizeClass`, com as abas de paciente e de nutri e telas vazias no lugar.

## Fase 1 · Entrar e criar conta

1. `EntrarScreen` (desenhada), com erro de senha e limite de tentativas.
2. `CadastroPacienteScreen` + `ConsentimentoScreen`: código de 8 caracteres ou link de convite, aceite LGPD obrigatório para seguir.
3. `CadastroNutriScreen`: nome, CRN, e-mail, senha; termina na Início com faixa "Teste: 5 pacientes por 30 dias".
4. `RecuperarSenhaScreen` e `EscolherPerfilScreen` (em paralelo).

## Fase 2 · Núcleo do paciente e da nutri

Paciente:

1. `HojeScreen` com check-in FEITO / TROCADO / PULADO e o estado "sua nutri está montando sua dieta".
2. `RefeicaoScreen` e `OrientacoesScreen`.
3. `SubstituirScreen` (só alimentos da lista do paciente, quantidade já na porção).
4. `PerfilScreen` com os links para Lembretes, Minhas nutris, Privacidade e Sair (as telas de destino podem ser vazias até a fase 3–4).

Nutri (em paralelo ao paciente):

5. `PacientesScreen` com busca e filtros, e `ConvidarSheet` gerando código e link; `FolhaLimitePlano` ao receber `LIMITE_PACIENTES_ATINGIDO`.
6. `FichaScreen` no celular e na lista-detalhe do layout expandido.
7. `PainelScreen` no expandido (desenhado) e no compacto (coluna).

## Fase 3 · Dieta, avaliação e hábitos

1. `MontarDietaScreen` no expandido (desenhada), depois no compacto; `PublicarDialog` mostrando o que mudou.
2. `AlimentosScreen`: lista da nutri por grupo, adicionar, tirar e ajustar.
3. `AvaliacaoScreen` (IMC calculado; % gordura e massas digitados no MVP).
4. Paciente: `ComprasScreen`, `EvolucaoScreen` + `RegistrarPesoSheet`, `AguaSheet` (em paralelo).
5. `AnamneseScreen` com os 12 passos do doc do MVP; "Saúde da mulher" só para sexo feminino; salva a cada passo.
6. `LembretesScreen`: notificações locais com ações, assinatura do .ics (Google e Apple), horário de silêncio.
7. `AvisosScreen` (central do sino).

## Fase 4 · Conta, sincronização e desktop

1. `ContaScreen` com os estados de assinatura e o link de pagamento fora do app; `AvisoSomenteLeitura` para EM_ATRASO após a carência.
2. `AgendaScreen` (dia e semana, criar e remarcar retorno).
3. `ModeloAnamneseScreen` e `RespostasAnamneseScreen`.
4. `HistoricoAvaliacoesScreen` com comparação de duas datas.
5. `MinhasNutrisScreen` (troca de nutri com autorização da cópia) e `PrivacidadeScreen` (exportar e excluir).
6. Desktop: `IndicadorSync`, `DialogoConflito`, `PrimeiraSyncScreen`, atalhos de teclado e janela mínima de [plataformas.md](plataformas.md).
7. `PdfDietaScreen`: ver, compartilhar (celular), salvar e imprimir (desktop).

## Fase 5 · Revisão de plataforma antes da loja

- Rodar cada tela em Android (celular e tablet), iPhone, iPad e desktop Windows/Mac; registrar prints no PR.
- Conferir safe area, gesto de voltar, permissões de notificação e o comportamento de assinatura fora do app frente às regras da App Store e da Play Store.
- Ícone do app, splash e nome "Dieta+" nas três plataformas.

## Próximo no canvas de design

Desenhar no canvas, nesta ordem, as telas que estão "faltando" e não são só variação de outra: cadastro do paciente + consentimento, cadastro da nutri, perfil do paciente, convidar paciente + limite do plano, conta e assinatura, agenda, alimentos, montar dieta no celular, indicador de sync e conflito no desktop.
