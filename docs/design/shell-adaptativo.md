# Navegação e shell — MOB-03

`App` inicia Koin e o tema e recebe um `PerfilNavegacao?`. Sem perfil, abre apenas o placeholder de entrada, respeitando as áreas seguras; não há login ou escolha de perfil fictícios. MOB-05/MOB-06 conectarão sessão e autenticação. Para conferir o shell sem serviços externos, use os previews ou `App(perfil = PerfilNavegacao.PACIENTE)` / `NUTRICIONISTA` no ponto de entrada de desenvolvimento.

As rotas são tipos `@Serializable`. Os grafos de paciente e nutri são distintos e somente o grafo do perfil recebido é registrado. O perfil aqui escolhe a apresentação: não implementa autorização, que continua no backend. Uma mudança/limpeza do perfil descarta a pilha e o estado visual anteriores.

- Paciente: Hoje, Dieta, Compras, Evolução e Perfil.
- Nutri: Início, Pacientes, Agenda, Alimentos e Conta, nos mesmos lugares lógicos nos três tamanhos.
- Cada aba preserva sua pilha ao alternar destinos; a ficha tipada pertence à área Pacientes e mantém essa aba selecionada.
- A classe compacta usa navegação inferior; média e expandida usam trilho. A lista de pacientes ocupa 360 dp na expandida, ao lado do detalhe. No médio, segue o painel único previsto em `plataformas.md` quando a largura não comporta os dois; navegar à ficha troca o painel e voltar restaura a lista.
- Redimensionar mantém o mesmo controlador e a mesma pilha. O shell consome `WindowInsets.safeDrawing`; a navegação inferior respeita as áreas de sistema. Alturas mínimas acompanham a escala de fonte e o trilho pode rolar verticalmente.

As abas e a ficha são placeholders vazios, sem modelos fictícios, API, banco ou regras de negócio. As telas dos artboards continuam pertencendo às tarefas futuras. Os componentes de navegação seguem `Main.dc.html`/`T-Painel.dc.html` e as correções de `validacao.md` (cinco entradas da nutri; Perfil não abre Lembretes).

O desktop usa o título localizado Dieta+, inicia em 1280×800 e aplica tamanho mínimo 1024×700. Persistir posição/tamanho, atalhos e bandeja continuam no escopo de MOB-31.

`NavegacaoAppTest` cobre troca de abas/perfil, entrada sem sessão, ficha/voltar, restauração da ficha ao trocar de aba, preservação ao redimensionar e disposição em três tamanhos, incluindo fonte 200%. As capturas em `evidencias/MOB-03/` são do renderizador Compose desktop (galeria de placeholders, não telas clínicas). Rodam com `./gradlew :shared:jvmTest`. Testes nativos iOS e gestos/safe area em aparelhos exigem macOS/dispositivos e CI; não foram executados no Windows.

Observações não bloqueantes do QA de MOB-02 sobre campos/raios ficam para a tarefa que tocar esses componentes. Nenhum campo foi alterado nesta entrega.
