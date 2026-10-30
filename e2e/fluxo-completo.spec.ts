// A4: fluxo de ponta a ponta da web contra o backend real (Postgres 18 + dados de exemplo do perfil dev).
//
// Nutri cadastra e convida a paciente, a paciente aceita, a nutri monta e publica a dieta, a paciente vê o Hoje e marca "Comi".
// Depois, uma segunda nutri não enxerga nada da primeira (RN de isolamento: dado de outra nutri = 404).
//
// Cada fase abre a página do zero e entra de novo. Motivos (achados da A4, ver docs/arquitetura/web.md):
//  - depois que uma folha ou diálogo do Compose fecha, a árvore de semantics do DOM continua mostrando a folha;
//  - recarregar a página perde a sessão na web (o AmbienteWeb não chama restaurarSessao()).
import { expect, test, type Browser, type Page } from "@playwright/test";
import { abrir, botao, campo, clicar, digitar, entrar, NUTRI_EXEMPLO, SENHA_EXEMPLO, tocar, veTexto } from "./apoio.ts";

const sufixo = Date.now().toString(36);
const paciente = { nome: `Paciente ${sufixo}`, email: `p${sufixo}@exemplo.dev`, senha: "Senha-de-Teste-E2E-2026" };
const nutriB = { nome: `Nutri B ${sufixo}`, email: `nutri.b.${sufixo}@exemplo.dev`, senha: "Senha-de-Teste-E2E-2026", crn: "CRN-3 12345" };
let codigoConvite = "";

const contextoPt = (browser: Browser) =>
  browser.newContext({ locale: "pt-BR", timezoneId: "America/Sao_Paulo", viewport: { width: 1280, height: 900 } });

/** Página nova (sem estado do Compose) já logada como a nutri de exemplo. */
async function nutriNaLista(page: Page) {
  await entrar(page, NUTRI_EXEMPLO);
  await expect(page.getByRole("heading", { name: "Início" })).toBeAttached({ timeout: 30_000 });
  await tocar(page, "Pacientes");
  await expect(page.getByRole("heading", { name: "Pacientes" })).toBeAttached();
}

async function abrirFicha(page: Page, nome: string) {
  await clicar(page, page.getByRole("button", { name: nome }));
  await expect(page.getByRole("heading", { name: nome })).toBeAttached();
}

test.describe.configure({ mode: "serial" });

test("a web carrega e a árvore de semantics chega ao DOM", async ({ page }) => {
  await abrir(page);
  await expect(page.getByRole("heading", { name: "Dieta+" })).toBeAttached();
  await expect(campo(page, "E-mail")).toBeAttached();
  await expect(campo(page, "Senha")).toBeAttached();
  await expect(botao(page, "Entrar")).toBeAttached();
});

test("senha errada não entra e mostra o erro", async ({ page }) => {
  await entrar(page, NUTRI_EXEMPLO, "senha-errada-123");
  await veTexto(page, "E-mail ou senha inválidos.");
  await expect(botao(page, "Entrar")).toBeAttached();
});

test("nutri cadastra a paciente", async ({ page }) => {
  await nutriNaLista(page);
  await tocar(page, "Novo paciente");
  await digitar(page, "Nome do paciente", paciente.nome);
  await digitar(page, "E-mail (opcional)", paciente.email);
  await digitar(page, "Nascimento (dd/mm/aaaa)", "10/05/1990");
  await tocar(page, "Feminino");
  const criado = page.waitForResponse((r) => r.url().endsWith("/api/v1/pacientes") && r.request().method() === "POST");
  await tocar(page, "Cadastrar paciente");
  expect((await criado).status()).toBe(201);
});

test("nutri gera o convite e vê o código", async ({ page }) => {
  await nutriNaLista(page);
  await abrirFicha(page, paciente.nome);
  await expect(page.getByText("Aguardando aceite do convite").first()).toBeAttached();
  await tocar(page, "Gerar convite");
  const texto = await page.getByText(/Código: [A-Z0-9]{8}/).first().textContent({ timeout: 20_000 });
  codigoConvite = texto!.replace("Código:", "").trim();
  expect(codigoConvite).toMatch(/^[A-Z0-9]{8}$/);
  await veTexto(page, new RegExp(`/convite/${codigoConvite}`));
});

test("paciente aceita o convite e cria a conta", async ({ browser }) => {
  const ctx = await contextoPt(browser);
  const page = await ctx.newPage();
  await abrir(page);
  await tocar(page, "Tenho um código da minha nutri");
  await digitar(page, "Código ou link do convite", codigoConvite);
  await tocar(page, "Consultar convite");
  await expect(page.getByRole("heading", { name: "Dra. Luciana Exemplo" })).toBeAttached({ timeout: 30_000 });
  await tocar(page, /Li e autorizo/);
  await tocar(page, "Criar minha conta");
  await digitar(page, "Nome completo", paciente.nome);
  await digitar(page, "E-mail", paciente.email);
  await digitar(page, "Senha", paciente.senha);
  await tocar(page, "Li e aceito os termos de uso");
  const aceite = page.waitForResponse((r) => r.url().includes("/aceitar") && r.request().method() === "POST");
  await tocar(page, "Criar conta e aceitar convite");
  const resposta = await aceite;
  expect(resposta.status(), `aceite do convite: ${await resposta.text()}`).toBe(200);
  await expect(page.getByRole("heading", { name: "Hoje" })).toBeAttached({ timeout: 30_000 });
  await veTexto(page, /Sua nutri está montando sua dieta/);
  await ctx.close();
});

test("nutri monta e publica a dieta da paciente", async ({ page }) => {
  await nutriNaLista(page);
  await abrirFicha(page, paciente.nome);
  await expect(page.getByText("Vínculo ativo").first()).toBeAttached();
  await tocar(page, "Dieta");
  await tocar(page, "Nova dieta");
  await expect(page.getByRole("heading", { name: "Montar dieta" })).toBeAttached({ timeout: 20_000 });
  await digitar(page, "Título da dieta", "Dieta E2E");
  await tocar(page, "Adicionar refeição");
  await digitar(page, "Nome da refeição", "Café da manhã");
  // Horário cedo: a refeição já passou em qualquer hora de execução, então o "Comi" aparece sempre.
  await clicar(page, campo(page, "Horário (HH:MM)"));
  await page.keyboard.press("Control+A");
  await page.keyboard.type("00:05");
  await tocar(page, "Adicionar alimento");
  await tocar(page, /Arroz branco cozido/);
  await veTexto(page, /Refeição: 80,00 kcal/);
  await tocar(page, "Salvar rascunho");
  await veTexto(page, "Rascunho salvo.");
  await tocar(page, "Publicar");
  await veTexto(page, "Publicar esta dieta?");
  const publicada = page.waitForResponse((r) => /dietas\/.+\/publicar/.test(r.url()) && r.request().method() === "POST");
  await tocar(page, "Publicar");
  expect((await publicada).status()).toBeLessThan(300);
});

test("paciente vê o Hoje com a dieta e marca Comi", async ({ browser }) => {
  const ctx = await contextoPt(browser);
  const page = await ctx.newPage();
  await entrar(page, paciente.email, paciente.senha);
  await expect(page.getByRole("heading", { name: "Hoje" })).toBeAttached({ timeout: 30_000 });
  await veTexto(page, /Café da manhã/);
  const comi = page.waitForResponse((r) => r.request().method() !== "GET" && r.url().includes("/api/v1/") && !r.url().includes("/auth/"));
  await tocar(page, "Comi");
  expect((await comi).status()).toBeLessThan(300);
  await veTexto(page, "Feita");
  await expect(botao(page, "Comi")).toHaveCount(0);
  await ctx.close();
});

test("isolamento: outra nutri não enxerga paciente nem dieta da primeira", async ({ browser, playwright }) => {
  const api = await playwright.request.newContext({ baseURL: "http://localhost:8081" });
  const dispositivo = (nome: string) => ({ id: crypto.randomUUID(), plataforma: "DESKTOP", nomeAparelho: nome });

  // Nutri B só existe aqui: o cadastro de nutri na web está bloqueado (termos de uso do app = null), então vai pela API.
  const cadastro = await api.post("/api/v1/auth/cadastro-nutri", {
    headers: { "Idempotency-Key": crypto.randomUUID() },
    data: { nome: nutriB.nome, email: nutriB.email, senha: nutriB.senha, crn: nutriB.crn, aceitouTermos: true, versaoTermos: "2026-10", dispositivo: dispositivo("e2e") },
  });
  expect(cadastro.status(), await cadastro.text()).toBeLessThan(300);

  const token = async (email: string, senha: string) => {
    const r = await api.post("/api/v1/auth/login", { headers: { "Idempotency-Key": crypto.randomUUID() }, data: { email, senha, dispositivo: dispositivo("e2e") } });
    expect(r.status(), await r.text()).toBe(200);
    return (await r.json()).accessToken as string;
  };
  const autorizacao = (t: string) => ({ Authorization: `Bearer ${t}` });

  const tokenA = await token(NUTRI_EXEMPLO, SENHA_EXEMPLO);
  const lista = await api.get("/api/v1/pacientes?pagina=0&tamanho=50", { headers: autorizacao(tokenA) });
  const dadosA = await lista.json();
  const itens: Array<{ id: string; nome: string }> = dadosA.itens ?? dadosA.conteudo ?? dadosA;
  const alvo = itens.find((p) => p.nome === paciente.nome);
  expect(alvo, "a paciente da nutri A precisa existir").toBeTruthy();

  const tokenB = await token(nutriB.email, nutriB.senha);
  const doOutro = await api.get(`/api/v1/pacientes/${alvo!.id}`, { headers: autorizacao(tokenB) });
  expect(doOutro.status(), "dado de outra nutri deve ser 404, nunca 403").toBe(404);
  const listaB = await api.get("/api/v1/pacientes?pagina=0&tamanho=50", { headers: autorizacao(tokenB) });
  expect(await listaB.text()).not.toContain(paciente.nome);

  // Na tela, a nutri B vê a lista dela, sem ninguém da nutri A.
  const ctx = await contextoPt(browser);
  const page = await ctx.newPage();
  await entrar(page, nutriB.email, nutriB.senha);
  await expect(page.getByRole("heading", { name: "Início" })).toBeAttached({ timeout: 30_000 });
  await tocar(page, "Pacientes");
  await expect(page.getByRole("heading", { name: "Pacientes" })).toBeAttached();
  await veTexto(page, /0 pacientes ocupados de 5/);
  for (const nome of [paciente.nome, "Maria Exemplo", "Ana Exemplo", "João Exemplo"]) await expect(page.getByText(nome)).toHaveCount(0);
  await ctx.close();
  await api.dispose();
});
