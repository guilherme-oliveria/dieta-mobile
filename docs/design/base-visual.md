# Base visual — MOB-02

A implementação segue os tokens de `componentes.md` e as correções de `validacao.md`. Referências visuais: `Entrar.dc.html`, `Main.dc.html` e os cartões/campos dos demais artboards. Não há telas do produto ou navegação nesta entrega.

## Disponível

- `DietaTheme`: paleta clara, Plus Jakarta Sans local em 400/500/600/700/800, tipografia Material 3 e formas.
- `Dimens` e `FormasDieta`: espaçamentos, altura mínima, largura máxima de leitura e raios centralizados.
- `IconesDieta`: casa, talheres, carrinho, gráfico, pessoa, sino, água, cadeado, calendário, lista, mais, voltar, fechar, busca, alerta, informação e sem internet. Vetores próprios com traço de 2 unidades e cantos arredondados; `Voltar` espelha em RTL. Quem criar um botão com ícone deve fornecer o rótulo acessível no botão.
- `BotaoPrincipal`, `BotaoContornado`, `BotaoTexto`: estados habilitado/desabilitado/carregando; carregamento mantém o rótulo, anuncia o estado e bloqueia novas ações.
- `CampoTexto`, `CampoHora`, `CampoNumero`: rótulo acima, estado de erro acessível e texto de erro com contraste AA. O campo numérico preserva a entrada com vírgula; o chamador trata a conversão/validação. Hora permite digitação de `:` e não interpreta o valor nesta camada.
- `CardDieta`, `CardProximaRefeicao`, `LinhaRefeicao` e `ChipStatus`: conteúdo recebido por parâmetros e ações delegadas ao chamador, sem dados de paciente ou regras de API.
- `EstadoVazio`, `EstadoErro`, `EstadoCarregando` e `FaixaOffline`: mensagens em resources e mudanças anunciadas por região acessível. O estado vazio pode receber ação; erro oferece nova tentativa.
- `ConteudoLeitura`: área centrada com largura máxima de 640 dp, margem de 20 dp na classe compacta e 24 dp nas classes média/expandida. Usa `WindowSizeClass`; não escolhe navegação ou shell.

Alturas são mínimas e textos podem quebrar linha. O cartão de refeição reorganiza as ações conforme o espaço disponível. Chips de status não são controles interativos e sempre contêm texto, além da cor. Os componentes mantêm estado fora deles e não usam rede, funcionando sem internet.

## Conferência

`PreviewsComponentes.kt` contém previews da base nos três tamanhos e a 200%, além das variantes de botões, campos, estados e ícones. A galeria é apenas uma amostra de preview, sem rota no aplicativo.

Os testes JVM geram capturas em `shared/build/reports/componentes/` para 390×1180, 820×1180 e 1180×1180, mais 390×2400 com fonte 200%. A altura maior dessa última captura permite conferir a expansão do conteúdo sem recortes; não representa uma resolução de aparelho. Evidências conferidas desta entrega ficam em `evidencias/MOB-02/`.

Os testes cobrem ações e bloqueio durante carregamento, edição decimal, anúncio de erro, ações de vazio/erro, centralização dos estados, expansão com fonte 200%, margens/largura nos três tamanhos e contraste AA dos pares de texto. Execute `./gradlew :shared:jvmTest :shared:testAndroidHostTest`.

As capturas são do renderizador Compose desktop. TalkBack, VoiceOver, teclado e visual em aparelhos Android/iOS precisam da conferência de plataforma das tarefas de tela e da fase de publicação. Não há teste nativo iOS no Windows.

Navegação/trilho/barra inferior pertencem a MOB-03. Os demais componentes específicos (gráficos, seletor de porção, conflitos e assinatura) entram nas tarefas que os utilizarem. Fonte e licença: `third-party/plus-jakarta-sans/`.
