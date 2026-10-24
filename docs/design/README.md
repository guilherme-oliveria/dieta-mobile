# Design do Dieta+ (app KMP)

Esta pasta é a fonte de verdade de UI do app `dieta-mobile` (Android, iOS e desktop, um único app com perfis nutri e paciente). Vale para pessoas, para o Claude e para o Codex.

Canvas de referência (13 telas, celular 390×844 e tablet 1180×820): https://claude.ai/artifact/6thej5zGsTo81ZosiigGxC

| Arquivo | Para que serve |
| --- | --- |
| [validacao.md](validacao.md) | Resultado da validação do canvas para Android, iOS e desktop: o que está bom, o que corrigir e as telas que faltam |
| [componentes.md](componentes.md) | Tokens (cores, tipografia, espaçamento, raios) e componentes comuns, com o nome do composable |
| [plataformas.md](plataformas.md) | Layout adaptativo (WindowSizeClass) e diferenças de Android, iOS e desktop |
| [telas.md](telas.md) | Mapa tela → pacote Kotlin → artboard do canvas, com status (desenhada ou faltando) |
| [plano-de-acao-ui.md](plano-de-acao-ui.md) | Ordem de implementação das telas em fases, com critério de pronto de cada tarefa |

Arquitetura, regras de negócio e contrato com a API ficam em `docs/arquitetura/` (mantidos pelo PO de arquitetura). Quando os dois divergirem, a regra de negócio vale e a tela se ajusta.

## Regras rápidas para quem implementa

1. Toda tela usa `DietaTheme` e os tokens de [componentes.md](componentes.md). Nada de cor ou tamanho solto no código.
2. Antes de criar uma tela, ache-a em [telas.md](telas.md). Se ela está "desenhada", siga o artboard; se está "faltando", siga a descrição de lá e os componentes existentes.
3. Toda tela tem os quatro estados: carregando, vazio, erro e conteúdo. E, quando lê dados locais, o estado "sem internet".
4. Textos em português do Brasil, em `composeResources/values/strings.xml`, nunca escritos direto no composable.
5. Alvos de toque com pelo menos 44 dp (iOS) e 48 dp (Android); usamos 48 dp em tudo.
6. Laranja (`AlertaDieta`) só para alerta. Nunca como cor de marca ou de botão principal.
7. Dados de exemplo dos previews vêm dos PDFs da Nut. Luciana (dieta de 2509 kcal, 6 refeições), como no canvas.
