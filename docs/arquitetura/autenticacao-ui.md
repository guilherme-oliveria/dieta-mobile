# Autenticação no app — MOB-06

`FluxoAutenticacao` observa `AutenticacaoViewModel`, que compartilha o estado das quatro telas de entrada. As telas recebem estado/callbacks e não acessam a API. `AutenticacaoRepositorio` usa `ApiDieta` para cadastro/login, `GET /eu`, recuperação/redefinição e logout. Não há mock em execução normal.

## Sessão e segurança

- A senha e o token de recuperação ficam apenas na memória do ViewModel, sem `SavedStateHandle`, `rememberSaveable`, rota ou preferência. O estado não os revela em `toString`. Ao voltar, sair ou concluir entrada/cadastro/redefinição são descartados.
- Login com vários perfis mostra somente NUTRICIONISTA/PACIENTE oferecidos pelo servidor. Escolher repete o login com as credenciais em memória. Cancelar invalida também a geração da sessão, para uma resposta atrasada não reabrir a conta.
- O cadastro não autentica automaticamente, conforme o contrato. A tela orienta confirmar o e-mail e entrar. A chave UUIDv7 é mantida nas tentativas do mesmo formulário e renovada quando os dados mudam.
- `GET /eu` real da BE-05 fornece perfis/contexto e assinatura. A sessão persistida permite abrir o shell antes da rede; falha de comunicação não destrói dados offline. Logout/401 definitivo usam a limpeza de MOB-05.
- Mensagens locais são escolhidas por `codigo`; textos de erro do servidor não são apresentados. Recuperação usa resposta genérica tanto para e-mail existente quanto inexistente. O link recebido contém token de uso único; a tela permite informar esse token para redefinir a senha. Integração automática de links ainda não existe nesta tarefa.
- O UUID da instalação fica em preferências de plataforma, separado dos tokens e preservado no logout. Não é identificador de hardware nem contém dado clínico. Android usa SharedPreferences com backup do app desativado; iOS usa NSUserDefaults; desktop usa Preferences do usuário. Tokens continuam exclusivamente no cofre de MOB-05.

## Termos de uso

Os documentos oficiais e contratos exigem `aceitouTermos`/`versaoTermos`, mas não publicam texto jurídico nem URL/versão oficial para exibição. Os JSONs são exemplos fictícios e não servem como termos reais.

`App(termosCadastro = TermosCadastro(versao, texto))` é o ponto explícito de configuração, encaminhado pela DI. A versão exibida e enviada são a mesma. Quando não configurado, a tela explica a indisponibilidade e não permite fabricar aceite/cadastro. Os entrypoints atuais não injetam termos porque não há publicação aprovada. Testes visuais usam conteúdo rotulado fictício; esse conteúdo não é configuração do produto. Publicar o texto/versionamento aprovado e injetá-lo é pendência de conteúdo antes de liberar cadastro para usuários reais.

## Design e verificação

Entrar segue `Entrar.dc.html`, corrigindo “Sou nutricionista” para abrir cadastro. Cadastro, recuperação e escolha seguem `telas.md`/`componentes.md`, pois DES-01 não publicou seus artboards. Formulários centralizam a área de leitura em telas maiores e usam rolagem/IME padding em telas pequenas. Previews compacto/expandido, testes nos três tamanhos e fonte 200% acompanham a tarefa. Leitores TalkBack/VoiceOver exigem conferência em aparelho/plataforma; semântica de senha, campos, ações e erros é coberta nos testes desktop.

Os cinco documentos comuns foram copiados da develop aprovada do backend `ac026c0b3a6933424453941f8ad85811006101ba` (BE-05, que inclui BE-04). Os 49 JSONs e manifesto foram preservados e conferidos pelos hashes dos objetos Git. Os testes de repositório usam MockEngine com exemplos oficiais; o app usa endpoints reais.
