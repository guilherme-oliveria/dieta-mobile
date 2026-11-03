# Android, iOS e desktop

Um código de UI só (Compose Multiplatform). O que muda é o tamanho de janela e alguns comportamentos de plataforma.

## Layout adaptativo

Decisão de arquitetura: `WindowSizeClass` define o layout, não a plataforma.

| Classe de largura | Onde | Navegação | Layout |
| --- | --- | --- | --- |
| Compacta (< 600 dp) | Celular | `BarraNavegacaoInferior` | Uma coluna, telas empilhadas (artboards 390×844) |
| Média (600–840 dp) | Tablet em pé, janela estreita no desktop | `TrilhoNavegacao` | Uma coluna mais larga; lista-detalhe só se couber |
| Expandida (≥ 840 dp) | Tablet deitado, desktop | `TrilhoNavegacao` | Lista-detalhe e painéis lado a lado (artboards 1180×820) |

Regras:

- Pacientes → Ficha vira lista-detalhe na expandida (lista com 360 dp à esquerda).
- Montar dieta: 3 colunas na expandida (refeições, itens, lista do paciente); na compacta, refeições viram abas no topo e a lista do paciente abre numa folha inferior.
- Avaliação: 2 colunas na expandida; uma coluna com seções recolhíveis na compacta.
- Conteúdo de leitura (Hoje, Refeição) tem largura máxima de 640 dp, centrado, em telas grandes.
- Testar sempre em 360×640, 390×844, 820×1180, 1180×820 e 1440×900.

## Android

- Edge-to-edge com `WindowInsets` (barra de status e gestos); nada de barra de status falsa.
- Botão/gesto voltar do sistema volta na pilha; em wizard (Anamnese) volta um passo.
- Notificações: canais "Refeições", "Água", "Pesagem", "Avisos da nutri". A notificação de refeição tem as ações Comi / Troquei / Pulei.
- Permissão de notificação (Android 13+) pedida no contexto, na tela Lembretes, não na primeira abertura.
- Alarmes exatos para refeição: pedir `SCHEDULE_EXACT_ALARM` só se o usuário ligar lembretes.

## iOS

- Respeitar safe area (notch e home indicator); a barra inferior fica acima do home indicator.
- Gesto de voltar deslizando da borda esquerda deve funcionar em toda tela empilhada.
- Notificações locais com categoria de ações (Comi / Troquei / Pulei). Pedido de permissão também na tela Lembretes.
- Agenda: o botão "Apple Calendário" abre a URL `webcal://` do feed .ics do backend.
- Teclado: campos numéricos usam teclado decimal com vírgula; "Concluído" fecha o teclado.
- Mantemos Material 3, mas sem efeito ripple exagerado: usar a indicação padrão do Compose.

## Desktop (nutri, sem internet)

- Janela mínima 1024×700; abre em 1280×800 e lembra tamanho e posição.
- Sempre layout expandido; com janela estreita cai para médio.
- `IndicadorSync` fixo no rodapé do trilho: "Sincronizado às 14:32", "3 alterações para enviar", "1 conflito".
- Sem internet: tudo da nutri funciona (pacientes, avaliação, montar dieta). Publicar dieta e convidar paciente ficam na fila e mostram "vai sair quando voltar a internet".
- Conflito (HTTP 409 por item no `/sync`): `DialogoConflito` com as duas versões lado a lado.
- Teclado: Tab percorre os campos na ordem visual; Enter salva no diálogo; Ctrl/Cmd+F busca paciente; Ctrl/Cmd+S salva avaliação ou rascunho; Esc fecha diálogo e folha.
- Hover em linhas de lista e botões (`BordaCampo` mais escura e fundo `SuperficieSuave`), foco visível com contorno verde de 2 dp.
- PDF da dieta: "Salvar como" e "Imprimir" pelo diálogo do sistema.
- Menu da janela: Arquivo (Sincronizar agora, Sair), Ajuda (Sobre). Sem outros menus no MVP.

## Acessibilidade (todas)

- Fonte do sistema até 200% sem cortar texto: alturas mínimas, nunca fixas.
- `contentDescription` em todo botão só com ícone; `semantics { heading() }` nos títulos de seção.
- Status nunca só por cor: chip tem texto, gráfico tem valor.
- TalkBack e VoiceOver: ordem de leitura igual à ordem visual.

### Checklist de acessibilidade da web (A6, 29/10/2026)

Medido com axe-core no Chromium (`e2e/acessibilidade.spec.ts`, roda em `ops/e2e-wsl.sh`) e conferido em capturas. O que sobra depende de pessoa e aparelho (G-14: NVDA, VoiceOver, TalkBack, Safari, Firefox).

- [x] `contentDescription`/nome em todo botão de ícone e campo: nenhum controle sem nome nas 20 telas varridas (Entrar, convite, cadastro, nutri, ficha, montador, paciente). Corrigidos `Checkbox` da lista de compras e `Switch` do perfil, que saíam sem nome.
- [x] `heading()` nos títulos: todas as telas têm título. Faltava na aba Evolução do paciente. Página com `<h1>` e landmark `<main>` no `index.html`.
- [x] Ordem de foco (Tab) em Entrar: e-mail, senha, mostrar senha, esqueci a senha (conferido em capturas); os demais botões seguem a ordem visual do layout, mas não foram capturados um a um. Ficha, Hoje e montador: não conferidos por Tab (G-14).
- [x] Foco visível: campos já tinham borda verde; botões tinham só um véu fraco, agora têm contorno de 2 dp (`anelDeFoco` em `Botoes.kt`).
- [x] Contraste de texto 4,5:1: todos os pares usados passam (`ContrasteTest`). O dia escolhido em Hoje e Compras parecia desabilitado (cinza claro); agora é fundo verde claro (`BotaoTexto(selecionado = true)`).
- [ ] Contraste de componentes (WCAG 1.4.11, 3:1): `BordaCampo` `#D5DBD0` dá 1,41:1 sobre branco e a borda do card 1,26:1. Decisão de design: escurecer a borda do campo (algo como `#8A9A8F`).
- [x] Fonte a 200%: janela de 640×450 px CSS (zoom de 200%) mantém o layout sem cortar texto em Entrar, Hoje, Compras, Início e Pacientes (capturas). O Android/iOS segue a escala do sistema.
- [~] Alvo de toque mínimo (48 dp): botões e campos têm 48 dp ou mais de área efetiva (`minimumInteractiveComponentSize`), mas os nós de acessibilidade de chips e do `Sair` medem 32 a 40 px. Linhas do Compras e do Perfil agora são o alvo inteiro (antes só o controle de 24 px).
- [ ] Estado "selecionado" (aba atual, chip escolhido) e título de nível (`aria-level`): o Compose para web não os expõe no DOM. Leitor de tela real precisa confirmar o efeito (G-14).
- [ ] Árvore de acessibilidade desatualizada depois que folha ou diálogo fecha: ver `web.md`. Bloqueia a revisão com leitor de tela.
