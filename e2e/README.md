# e2e — teste de ponta a ponta da web

Playwright (Chromium) contra o **backend real** (Postgres 18 + dados de exemplo do perfil `dev`) e a **web de produção** (`wasmJsBrowserDistribution`). Não usa mock.

## Rodar

No WSL, com o `dieta-backend` ao lado (`../dieta-backend`):

```bash
ops/e2e-wsl.sh                      # sobe tudo, roda o fluxo e derruba tudo
ops/e2e-wsl.sh -g isolamento        # só um cenário
E2E_SEM_BUILD=1 ops/e2e-wsl.sh      # reaproveita a web já construída
E2E_MANTER=1 ops/e2e-wsl.sh         # deixa backend e banco no ar para investigar
```

O script cria um Postgres 18 próprio (`e2e/compose.e2e.yaml`, porta 5433, `tmpfs`), então **não toca no banco de desenvolvimento** e cada execução começa de um banco novo. Saídas: `e2e/relatorio/` (HTML), `build/e2e-wsl.txt` (resumo) e `build/e2e-*.log`.

Requisitos: Java 25, Node 24, Docker. Sem `sudo`, o Chromium do Playwright precisa de `libnspr4`, `libnss3` e `libasound2t64`: `apt-get download` dos três `.deb`, `dpkg -x` em `~/.local/chromium-libs` (o script já põe essa pasta no `LD_LIBRARY_PATH`). Com `sudo`: `npx playwright install-deps chromium`.

## O que o fluxo cobre (`fluxo-completo.spec.ts`)

1. A web abre e a árvore de semantics chega ao DOM.
2. Senha errada não entra.
3. Nutri (`nutri@exemplo.dev`) cadastra a paciente e gera o convite (código de 8 caracteres).
4. Paciente consulta o convite, aceita o consentimento e cria a conta.
5. Nutri monta a dieta (refeição + alimento do catálogo), salva o rascunho e publica.
6. Paciente entra, vê o Hoje com a refeição e marca **Comi** (aparece "Feita").
7. Isolamento: uma segunda nutri recebe `404` (não `403`) ao pedir a paciente da primeira e não a vê na lista, pela API e pela tela.

## Como a web é testada (a árvore de acessibilidade vira o DOM)

O app é desenhado em `<canvas>`, mas o Compose publica a árvore de `semantics` como elementos HTML **dentro de um shadow root aberto**, com `role`, `aria-label` e texto. O Playwright atravessa o shadow DOM, então os seletores são `getByRole` e `getByText` (ver `apoio.ts`). Particularidades que o `apoio.ts` resolve:

- o canvas fica por cima e intercepta o clique: o `apoio.ts` clica nas coordenadas do elemento (`page.mouse`);
- campos de texto são `<div contenteditable>`: nada de `fill()`; clica e digita pelo teclado;
- elemento fora da janela chega com caixa 0×0: o `apoio.ts` rola com a roda do mouse até ele aparecer;
- o Chromium headless do Linux precisa de `locale: "pt-BR"` (sem isso o Compose quebra com "Incorrect locale information provided").

## Achados da A4

Estão em `docs/arquitetura/web.md`, seção "Achados do e2e (A4)": árvore de semantics desatualizada depois que folhas e diálogos fecham, sessão perdida ao recarregar, cadastro de nutri bloqueado na web, e o link de convite `dietamais://` que o app recusa. Por causa dos dois primeiros, **cada fase do spec abre a página do zero e entra de novo**.
