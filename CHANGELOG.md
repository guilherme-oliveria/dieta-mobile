# Changelog — dieta-mobile

Formato [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/), versões [SemVer](https://semver.org/lang/pt-BR/). Cada versão é um marco de `docs/arquitetura/plano-de-execucao.md` (seção 3). Comum ao `dieta-backend` (mesmos números de versão).

> **Estado em 29/10/2026:** ainda não existe tag nem `release/`; tudo abaixo está na `develop`. Veja "Para o Guilherme" no `CHANGELOG.md` do backend sobre como cortar as releases.

## [Não lançado] — pendente

- **Web:** cadastro de nutri bloqueado (termos de uso não configurados no `main.kt`); árvore de acessibilidade desatualizada depois que folha ou diálogo fecha; autofill de senha não funciona (campos do Compose não são `<input>`). Detalhes e medições em `docs/arquitetura/web.md`.
- MOB-18b Ações Comi/Troquei/Pulei na notificação, MOB-27b push no iOS, MOB-29b escrita offline de dietas, listas e modelos, MOB-31b pacotes do desktop no CI, MOB-33b envio da logo, MOB-35a beta interno (assinatura de release, Play Console, TestFlight).
- MOB-46 Publicação da web na VPS (depende do BE-32b).
- Conferir com leitor de tela real (NVDA, VoiceOver, TalkBack), Safari e Firefox (G-14).

## [1.3.0] — M9: necessidades e protocolos (em andamento)

- MOB-52 Passo "Necessidades" da avaliação (método do GET, nível de atividade, objetivo, gestante/lactante, protocolos das metas, dislipidêmico) com GEB, GET, VET, rosca e metas ao vivo; avaliações salvas reabrem com as escolhas. Textos antigos com `%%` corrigidos.
- MOB-53 "Meus protocolos" na conta da nutri (dobras e massa, salvo online com `If-Match`); a avaliação nova já vem com o padrão dela.

## [1.2.0] — M8: avaliação de adulto completa

- MOB-47 Biblioteca de gráficos (rosca, linhas com faixa, medidor, régua de faixas, "ver como tabela"). MOB-48 `dominio/calculos` com os mesmos vetores do backend (prévia ao vivo, offline).
- MOB-49 Paciente vê a avaliação inteira (perímetros, dobras, composição, parecer). MOB-50 Avaliação em passos com resultados ao vivo (nutri).
- MOB-51 "Meus resultados" do paciente: semáforos com texto, rosca de composição, meta de peso e evolução com faixa saudável.
- Pendente: crianças e adolescentes mostram só um aviso (onda 22); o app ainda não lê o protocolo padrão da nutri (MOB-53); textos antigos com `%%` aparecem duplicados na tela.

## [1.1.0] — M7: web no navegador (nutri e paciente)

- MOB-40 Alvo `wasmJs` e build da web no CI. MOB-41 `ApiDieta` sem banco local (`ArmazemSessao`). MOB-42 Sessão no navegador: access em memória, refresh por cookie, `X-Plataforma: WEB`.
- MOB-43 Telas da nutri pela API (pacientes, montador de dieta, avaliações, lista de substituição, alimentos, agenda, conta, painel, avisos, anamnese, evolução). Medido na A5: 5,5 MB gzip no primeiro carregamento, 7 s em 4G rápido, 31 s em 4G lento.
- MOB-44 Telas do paciente (Hoje, Dieta, Evolução, Compras, Perfil, anamnese, avisos). MOB-45 URL e botão voltar do navegador, link direto do convite, atalhos Alt+1 a Alt+5; acessibilidade medida com axe-core (A6): controles com nome, títulos, foco visível nos botões, contraste.
- Sessão: recarregar a página (F5) mantém a sessão pelo refresh do cookie, com tela de carregando; sem rede avisa e não desloga (`e2e/sessao-f5.spec.ts`); queda de rede do navegador agora vira falha de comunicação.
- Correções: aceite de convite pela web (`fuso` `SYSTEM` recusado pelo backend), checkbox das compras e switch do perfil sem nome acessível, título da aba Evolução, dia selecionado parecendo desabilitado.
- Teste de ponta a ponta com Playwright (`e2e/`, `ops/e2e-wsl.sh`).

## [1.0.0] — M6: assinatura, PDF, privacidade

- MOB-32 Assinatura. MOB-33 PDF da dieta e da avaliação (link assinado aberto no navegador do sistema). MOB-34 Privacidade (exportar e excluir dados). MOB-35 Aviso de atualização do app (`426 VERSAO_APP_OBSOLETA`).

## [0.5.0] — M5: nutri offline com conflitos

- MOB-29 Escrita offline da nutri (consultas, avaliações, cadastro de paciente, alimentos). MOB-30 Tela de conflito e lista de pendências. MOB-31 Desktop para consultório (sync a cada 5 min com a janela na bandeja, menu, atalhos).

## [0.4.0] — M4: avaliação, anamnese, agenda e push

- MOB-22 Avaliação e histórico. MOB-23 Evolução com registro de peso. MOB-24 Anamnese da nutri. MOB-25 Anamnese do paciente. MOB-26 Agenda de consultas. MOB-27 Push e central de avisos (Android e comum). MOB-28 Painel da nutri.

## [0.3.0] — M3: rotina do paciente

- MOB-16 Envio da fila. MOB-17 Check-in (Comi/Troquei/Pulei) e água. MOB-18 Lembretes locais. MOB-19 Perfil e preferências. MOB-20 Lista de compras. MOB-21 Agenda do celular (`.ics`).

## [0.2.0] — M2: dieta montada, publicada e vista pela paciente

- MOB-10 Alimentos da nutri. MOB-11 Lista de substituição do paciente. MOB-12 Montador de dieta. MOB-13 Publicar e duplicar. MOB-14 Hoje e Dieta. MOB-15 Trocar alimento. MOB-36 Perfil da nutri.

## [0.1.0] — M1: conta, paciente e convite

- MOB-01 Dependências e estrutura. MOB-02 Tipografia e componentes. MOB-03 Navegação e shell adaptativo. MOB-04 Banco local e fila. MOB-05 Cliente da API e sessão. MOB-06 Entrar e cadastro da nutri. MOB-07 Pacientes da nutri. MOB-08 Aceitar convite. MOB-09 Sync de leitura.

## Checklist de loja (preenchido até onde dá sem contas)

| Item | Estado |
| --- | --- |
| Nome "Dieta+", cores e fonte do design | Pronto |
| `426 VERSAO_APP_OBSOLETA` → tela "atualize o app" | Pronto |
| Ícones, splash e capturas de tela | Falta (depende do design final e do Guilherme) |
| Assinatura de release (keystore, secrets no CI) | Falta (G-06; MOB-35a) |
| Google Play Console, teste interno | Falta (conta do Guilherme) |
| TestFlight / App Store Connect | Falta (Apple Developer e Mac, G-06) |
| Política de privacidade publicada e texto de consentimento | Falta (Guilherme; sugerida revisão jurídica) |
| Regras das lojas para assinatura contratada fora do app (S4) | Falta conferir |
| Push no iOS (APNs, Firebase) | Falta (MOB-27b) |
| Acessibilidade com leitor de tela real | Falta (G-14); medição por axe-core feita para a web |
