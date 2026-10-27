# MOB-10 — recorte independente

Nove capturas com exemplos fictícios oficiais C2: lista e formulário em 390×844, 820×1180 e 1180×820; ambos também em 390×844 com fonte em 200%. A nona usa o shell completo/DI/SQLite com engine de testes oficial, navega para Alimentos e abre o formulário; verifica Salvar acima da navegação inferior, mesmo com cabeçalho autenticado e fonte em 200%.

A tela Alimentos ainda está “faltando” em `docs/design/telas.md` (DES-04). As capturas seguem a descrição de tela, tokens/componentes existentes e o plano de UI; não representam desenho aprovado adicional do canvas. Foram conferidas rolagem, quebra de rótulos, alvos de toque, ações explícitas e cache offline. Testes verificam botões de escrita desabilitados offline, decimal local e confirmação antes da desativação.

As capturas não comprovam endpoint implementado, catálogo/macros reais, sync BE-14, iOS/dispositivo ou leitor de tela. Esses gates permanecem explícitos em `docs/arquitetura/alimentos-da-nutri.md` e no PR.
