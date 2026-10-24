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
