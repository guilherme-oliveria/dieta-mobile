# MOB-06 — entrada e cadastro da nutri

**Objetivo:** substituir a entrada vazia por entrar, cadastro com CRN/termos, recuperação de senha e escolha de perfil, usando o cliente/sessão de MOB-05.

**Arquitetura:** telas sem estado recebem estado/callbacks de `AutenticacaoViewModel`; `AutenticacaoRepositorio` adapta os DTOs oficiais a ApiDieta. Senha permanece apenas na memória do ViewModel durante entrada/escolha; não usa SavedStateHandle. Identidade da instalação é UUID persistido em preferências de plataforma, separado das credenciais. O shell acompanha a sessão e descarta o estado anterior no logout.

**Fontes:** plano-de-acao.md MOB-06, mobile.md, contrato-fase-1.md (BE-05 aprovado, `ac026c0`, incluindo BE-04), artboard Entrar e componentes/telas/validacao do design. Cadastro/recuperação/escolha não têm artboards; seguem a descrição e tokens existentes. Cadastro não faz login automático, conforme contrato, apesar da descrição antiga de UI.

**Decisões de execução:** manter checkout exclusivo do mobile e branch própria; execução inline autorizada pelo fluxo contínuo, sem novos agentes. Termos jurídicos não existem nos documentos/configuração; conteúdo e versão serão injetáveis, sem inventar aceite e com cadastro indisponível quando ausentes. BE-05 entrou na develop durante a execução: integrar /eu real, testar com MockEngine oficial e abrir offline pelo contexto salvo.

## Etapas

- [x] Testar ViewModel/repositório: validação apenas de feedback, erros por código, sessão restaurada, múltiplos perfis, senha descartada ao sair/escolher, falha de rede e submissões concorrentes.
- [x] Implementar identidade da instalação e operações auth reais (cadastro, entrar, esqueci-senha e redefinir pelo token informado), sem mock em execução normal.
- [x] Implementar telas com strings, campo de senha protegido, carregamento/erro, rolagem, teclado e semântica; termos disponíveis para leitura antes do aceite.
- [x] Integrar DI/entrypoints/sessão/navegação e faixa e-mail não confirmado; /eu e assinatura vêm do servidor, sem limites constantes.
- [x] Testar navegação, campo de senha e layouts compacto/médio/expandido/fonte 200%; registrar capturas e limitações de leitor de tela.
- [ ] JVM/Android host, metadados, migrações e builds; atualizar plano/docs, abrir PR develop e acompanhar CI iOS/Android/desktop antes de pedir QA.

## Foco da revisão

- Resposta atrasada após voltar/cancelar não pode autenticar ou restaurar senha antiga.
- Escolha aceita somente os perfis conhecidos oferecidos pelo servidor; credenciais nunca entram na rota/URL ou persistência de formulário.
- Cadastro sem termos publicados não fabrica versão/aceite; erro de campo usa `codigo`, nunca `mensagem` remota.
- Recuperação não revela existência do e-mail e não persiste token/senha; falha transitória mantém sessão offline.
- Fonte 200% e teclado não cortam campos/ações; falha de cofre não libera shell.

## Evidência de execução

Testes iniciais falharam por classes ausentes antes da implementação. Regressões de chave de cadastro e identidade divergente em /eu foram RED e corrigidas. 81 testes JVM e 15 Android host (incluem testes comuns), metadados, migrações, APK debug e JAR desktop passaram; log ignorado .gradle/mob06-final.log. O teste do app com DI exige Main controlado no harness; sem isso a animação de restauração não estabilizava. Ruling: termos ausentes impedem somente cadastro, por não existir publicação aprovada; custo é o cadastro real aguardar conteúdo/configuração. Leitores de tela nativos e execução iOS aguardam plataforma/CI.
