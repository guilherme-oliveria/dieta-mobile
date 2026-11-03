# Artboards do canvas (cópia)

Cópia dos artboards do canvas https://claude.ai/artifact/6thej5zGsTo81ZosiigGxC, para agentes sem acesso ao claude.ai (Codex e outros) lerem o desenho de cada tela.

- **O canvas é a fonte.** Estes arquivos são uma foto de 24/10/2026. Quando uma tarefa `DES-NN` desenhar ou mudar telas no canvas, o mesmo PR atualiza esta pasta.
- Cada `*.dc.html` é o HTML de uma tela: layout, cores, tamanhos, textos e os dados de exemplo (no `renderVals()` do script, no fim do arquivo). Não abre sozinho no navegador (depende do `support.js` do canvas); leia como especificação.
- `canvas.json` dá o título e o tamanho de cada artboard (celular 390×844, tablet 1180×820).
- Antes de implementar, aplique as correções de [../validacao.md](../validacao.md) (cores de contraste, abas da nutri, "Sou nutricionista" etc.): elas vencem o artboard.
- Mapa artboard → composable em [../telas.md](../telas.md).

| Arquivo | Tela |
| --- | --- |
| `Main.dc.html` | Paciente · Hoje |
| `P-Refeicao.dc.html` | Paciente · Refeição |
| `P-Substituir.dc.html` | Paciente · Trocar alimento |
| `P-Compras.dc.html` | Paciente · Lista de compras |
| `P-Progresso.dc.html` | Paciente · Evolução |
| `P-Lembretes.dc.html` | Paciente · Lembretes e agenda |
| `P-Anamnese.dc.html` | Paciente · Anamnese (passo 7) |
| `Entrar.dc.html` | Entrar |
| `N-Pacientes.dc.html` | Nutri · Pacientes (celular) |
| `N-Paciente.dc.html` | Nutri · Ficha do paciente (celular) |
| `T-Painel.dc.html` | Nutri · Painel (tablet) |
| `T-Dieta.dc.html` | Nutri · Montar dieta (tablet) |
| `T-Avaliacao.dc.html` | Nutri · Avaliação (tablet) |
