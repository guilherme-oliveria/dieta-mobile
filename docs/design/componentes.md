# Tokens e componentes

Tudo o que está aqui foi tirado do canvas. O código fica em `shared/src/commonMain/kotlin/org/br/dietamais/ui/theme` (tokens) e `ui/comum` (componentes).

## Cores

| Token Kotlin | Hex | Uso | Papel no `ColorScheme` |
| --- | --- | --- | --- |
| `VerdeDieta` | `#2E7D4F` | Marca, botão principal, aba ativa, barra de progresso | `primary` |
| `VerdeDietaEscuro` | `#1F5C39` | Texto verde sobre fundo claro, hover/pressionado | `onPrimaryContainer` |
| `VerdeDietaClaro` | `#E6F2EA` | Chip "feita", seleção, fundo de avatar | `primaryContainer` |
| `FundoDieta` | `#F5F6F1` | Fundo das telas | `background` |
| `SuperficieDieta` | `#FFFFFF` | Cards, barras, campos | `surface` |
| `SuperficieSuave` | `#F0F2EC` | Chip neutro, botão secundário sem borda | `surfaceVariant` |
| `BordaDieta` | `#E2E6DE` | Borda de card e divisória de barra | `outlineVariant` |
| `BordaCampo` | `#D5DBD0` | Borda de campo e botão contornado | `outline` |
| `DivisoriaLista` | `#EEF1EA` | Linha entre itens de lista | — |
| `TextoDieta` | `#17251D` | Texto principal, chip "agora" | `onBackground`, `onSurface` |
| `TextoSecundarioDieta` | `#5B6B61` | Texto de apoio, aba inativa | `onSurfaceVariant` |
| `TextoRiscado` | `#66736A` | Item concluído/riscado | — |
| `AlertaDieta` | `#C2551F` | Só ícone, ponto e borda de alerta | `error` |
| `AlertaTexto` | `#9A3F14` | Texto de alerta | `onErrorContainer` |
| `AlertaClaro` | `#FBEDE3` | Fundo de aviso, chip "trocada" | `errorContainer` |
| `InfoDieta` | `#2F6FA8` | Água, eventos informativos (respondeu anamnese) | `tertiary` |
| `InfoClaro` | `#EAF1F8` | Fundo de aviso informativo | `tertiaryContainer` |

Contraste conferido (WCAG AA, 4,5:1 para texto): branco sobre verde 5,05; secundário sobre fundo 5,2; alerta-texto sobre alerta-claro 5,9; info sobre branco 5,3. `AlertaDieta` como texto sobre o fundo dá 4,19 e por isso não é usado como texto.

## Tipografia

Fonte: Plus Jakarta Sans (400, 500, 600, 700, 800), empacotada em `composeResources/font`.

| Estilo M3 | Tamanho / peso | Exemplo no canvas |
| --- | --- | --- |
| `headlineLarge` | 34 sp / 800, espaçamento −0,03 em | "Dieta+" na entrada |
| `headlineMedium` | 26–28 sp / 800, −0,02 em | "Olá, Guilherme", "Sua rotina" |
| `titleLarge` | 22 sp / 800 | "Almoço · 12:00", nome do paciente |
| `titleMedium` | 17 sp / 700 | Título de seção ("Refeições de hoje") |
| `bodyLarge` | 15–16 sp / 600 | Item de lista, campo |
| `bodyMedium` | 14 sp / 500–600 | Texto de botão, descrição |
| `bodySmall` | 13 sp / 500 | Texto de apoio |
| `labelSmall` | 11–12 sp / 700 | Chip, rótulo de aba, sobretítulo em caixa alta (+0,08 em) |

Números grandes (peso, kcal, %) usam 20 sp / 800.

## Espaçamento e forma

- Margem lateral da tela: 20 dp no celular, 24 dp no tablet e desktop.
- Espaço entre blocos: 12–16 dp; dentro de card: 8–14 dp.
- Raios: campo e botão 12–14 dp, card 16–20 dp, card de destaque 22 dp, chip e pílula 999 (totalmente arredondado).
- Card: fundo `SuperficieDieta`, borda 1 dp `BordaDieta`, sem sombra.
- Botão principal: 52 dp de altura; botões secundários e chips: mínimo 44 dp de área de toque (usar 48 dp de área com `minimumInteractiveComponentSize`).

## Componentes comuns (`ui/comum`)

| Composable | Onde aparece | Observação |
| --- | --- | --- |
| `BotaoPrincipal`, `BotaoContornado`, `BotaoTexto` | Todas | Altura 52, raio 14, estado carregando |
| `CampoTexto`, `CampoHora`, `CampoNumero` | Entrar, Anamnese, Avaliação | Rótulo acima, borda `BordaCampo`, erro em `AlertaTexto` |
| `CardDieta` | Todas | Card padrão com borda |
| `CardProximaRefeicao` | Hoje | Fundo verde, ações Comi / Troquei / Pulei |
| `LinhaRefeicao` | Hoje, Montar dieta | Hora, nome, kcal, `ChipStatus` |
| `ChipStatus` | Hoje, Pacientes, Painel | Variantes: feita, trocada, agora, depois, alerta, info |
| `ChipFiltro` | Pacientes, Montar dieta, Anamnese | Selecionável, `aria-pressed` vira `selected` semântico |
| `BarraProgresso` | Hoje, Anamnese | Refeições do dia, passos do questionário |
| `CardMetrica` | Ficha, Evolução, Painel | Rótulo, valor grande, variação |
| `GraficoAdesao`, `GraficoPeso` | Ficha, Evolução | Barras com valor; não depender só da cor |
| `Avatar` | Pacientes, Ficha | Iniciais com 4 tons fixos |
| `ItemListaAlimento` | Trocar alimento, Montar dieta, Compras | Nome, medida, gramas, ação |
| `SeletorPorcao` | Montar dieta | − quantidade + com frações (¼, ½, ¾) |
| `AvisoFaixa` | Ficha, Evolução | Variantes alerta e info |
| `LinhaInterruptor` | Lembretes | Título, detalhe, `Switch` |
| `BarraNavegacaoInferior` | Celular | 5 abas, ícone + rótulo |
| `TrilhoNavegacao` | Tablet e desktop | Mesmas abas na lateral |
| `EstadoVazio`, `EstadoErro`, `EstadoCarregando` | Todas | Ícone, frase, ação |
| `FaixaOffline` | Todas com dados locais | "Sem internet. Mostrando o que está salvo." |
| `IndicadorSync` | Desktop e tablet da nutri | Sincronizado / pendente (n) / com conflito |
| `DialogoConflito` | Nutri | "Este registro mudou em outro aparelho": manter o meu / usar o do servidor |
| `FolhaLimitePlano` | Nutri | Erro `LIMITE_PACIENTES_ATINGIDO`: explica o limite e abre o link de assinatura fora do app |

Ícones: traço de 2 px no estilo Lucide, como no canvas. Usar `ImageVector` próprios em `ui/comum/icones`, nunca emoji.
