// A sessão da web sobrevive ao F5: o access token fica só em memória e o refresh vive no cookie HttpOnly. Ao recarregar, o app
// renova pelo cookie (POST /auth/refresh) enquanto mostra "Carregando", e só cai em Entrar se o cookie não valer mais.
import { expect, test, type Browser } from "@playwright/test";
import { abrir, botao, entrar, NUTRI_EXEMPLO, sair, SENHA_EXEMPLO, tocar, veTexto } from "./apoio.ts";

const contexto = (browser: Browser) => browser.newContext({ locale: "pt-BR", timezoneId: "America/Sao_Paulo", viewport: { width: 1280, height: 900 } });

async function recarregar(page: import("@playwright/test").Page) {
  await page.reload();
  await expect(page.getByRole("heading").first().or(botao(page, "Entrar"))).toBeAttached({ timeout: 60_000 });
}

test.describe.configure({ mode: "serial" });

test("nutri continua logada depois do F5", async ({ browser }) => {
  const ctx = await contexto(browser);
  const page = await ctx.newPage();
  await entrar(page, NUTRI_EXEMPLO);
  await expect(page.getByRole("heading", { name: "Início" })).toBeAttached({ timeout: 30_000 });
  const refresh = page.waitForResponse((r) => r.url().endsWith("/api/v1/auth/refresh") && r.request().method() === "POST");
  await recarregar(page);
  expect((await refresh).status()).toBe(200);
  await expect(page.getByRole("heading", { name: "Início" })).toBeAttached({ timeout: 30_000 });
  await expect(botao(page, "Entrar")).toHaveCount(0);
  await tocar(page, "Pacientes");
  await expect(page.getByRole("heading", { name: "Pacientes" })).toBeAttached();
  await ctx.close();
});

test("paciente continua logada depois do F5", async ({ browser }) => {
  const ctx = await contexto(browser);
  const page = await ctx.newPage();
  await entrar(page, "maria@exemplo.dev", SENHA_EXEMPLO);
  await expect(page.getByRole("heading", { name: "Hoje" })).toBeAttached({ timeout: 30_000 });
  await recarregar(page);
  await expect(page.getByRole("heading", { name: "Hoje" })).toBeAttached({ timeout: 30_000 });
  await expect(botao(page, "Entrar")).toHaveCount(0);
  await ctx.close();
});

test("durante o refresh aparece Carregando, não a tela de Entrar piscando", async ({ browser }) => {
  const ctx = await contexto(browser);
  const page = await ctx.newPage();
  await entrar(page, NUTRI_EXEMPLO);
  await expect(page.getByRole("heading", { name: "Início" })).toBeAttached({ timeout: 30_000 });
  await page.route("**/api/v1/auth/refresh", async (rota) => { await new Promise((r) => setTimeout(r, 5000)); await rota.continue(); });
  await page.reload();
  await page.waitForTimeout(3500);
  await expect(botao(page, "Entrar"), "a tela de Entrar não pode aparecer enquanto o refresh não respondeu").toHaveCount(0);
  await veTexto(page, "Carregando");
  await expect(page.getByRole("heading", { name: "Início" })).toBeAttached({ timeout: 30_000 });
  await ctx.close();
});

test("sair de verdade continua deslogando depois do F5", async ({ browser }) => {
  const ctx = await contexto(browser);
  const page = await ctx.newPage();
  await entrar(page, NUTRI_EXEMPLO);
  await expect(page.getByRole("heading", { name: "Início" })).toBeAttached({ timeout: 30_000 });
  await sair(page);
  await recarregar(page);
  await expect(botao(page, "Entrar")).toBeAttached({ timeout: 30_000 });
  await expect(page.getByRole("heading", { name: "Início" })).toHaveCount(0);
  await ctx.close();
});

test("sem cookie (primeira visita) vai direto para Entrar", async ({ browser }) => {
  const ctx = await contexto(browser);
  const page = await ctx.newPage();
  await abrir(page);
  await expect(botao(page, "Entrar")).toBeAttached();
  await ctx.close();
});

test("sem rede ou API fora do ar no refresh: avisa, não desloga, e o próximo F5 restaura", async ({ browser }) => {
  const ctx = await contexto(browser);
  const page = await ctx.newPage();
  await entrar(page, NUTRI_EXEMPLO);
  await expect(page.getByRole("heading", { name: "Início" })).toBeAttached({ timeout: 30_000 });

  await page.route("**/api/v1/auth/refresh", (rota) => rota.abort("connectionfailed"));
  await recarregar(page);
  await expect(botao(page, "Entrar")).toBeAttached({ timeout: 30_000 });
  await veTexto(page, "Não foi possível conectar. Confira sua internet e tente novamente.");

  await page.unroute("**/api/v1/auth/refresh");
  await page.route("**/api/v1/auth/refresh", (rota) => rota.fulfill({ status: 503, contentType: "application/problem+json", body: '{"type":"about:blank","title":"Indisponível","status":503,"codigo":"INDISPONIVEL","campos":[]}' }));
  await recarregar(page);
  await veTexto(page, "Não foi possível conectar. Confira sua internet e tente novamente.");

  await page.unroute("**/api/v1/auth/refresh");
  await recarregar(page);
  await expect(page.getByRole("heading", { name: "Início" })).toBeAttached({ timeout: 30_000 });
  await ctx.close();
});
