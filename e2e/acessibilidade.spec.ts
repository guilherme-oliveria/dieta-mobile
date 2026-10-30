// A6 (MOB-45): trava de acessibilidade da web. Roda o axe-core na árvore de semantics que o Compose publica no DOM.
//
// O que falha o teste: qualquer violação do axe fora da lista abaixo (controle sem nome, falta de h1/main/landmark, etc.).
// As violações da lista vêm do próprio Compose para web (1.12), não do código do app, e não dá para corrigir daqui:
//  - aria-required-attr: o Compose emite role="heading" sem aria-level;
//  - aria-required-children: LazyColumn emite role="list" e os itens saem como botões, sem role="listitem";
//  - aria-roles: o <canvas> sai com role="generic".
import AxeBuilder from "@axe-core/playwright";
import { expect, test, type Page } from "@playwright/test";
import { abrir, clicar, entrar, NUTRI_EXEMPLO, tocar } from "./apoio.ts";

const DO_COMPOSE = new Set(["aria-required-attr", "aria-required-children", "aria-roles"]);

async function conferir(page: Page, tela: string) {
  await page.waitForTimeout(1200);
  const { violations } = await new AxeBuilder({ page }).analyze();
  const nossas = violations.filter((v) => !DO_COMPOSE.has(v.id));
  expect(nossas.map((v) => `${v.id} (${v.nodes.length}): ${v.help}`), `axe em "${tela}"`).toEqual([]);
  // Toda tela tem ao menos um título (heading) e nenhum botão, campo ou aba fica sem nome acessível.
  const resumo = await page.evaluate(() => {
    const semNome: string[] = [];
    let titulos = 0;
    const visitar = (n: Element) => {
      if (n.shadowRoot) [...n.shadowRoot.children].forEach(visitar);
      const role = n.getAttribute("role");
      if (role === "heading") titulos++;
      if (role && ["button", "textbox", "tab", "checkbox", "switch", "link"].includes(role) && !(n.getAttribute("aria-label") || n.textContent || "").trim()) semNome.push(role);
      [...n.children].forEach(visitar);
    };
    visitar(document.body);
    return { titulos, semNome };
  });
  expect(resumo.titulos, `títulos em "${tela}"`).toBeGreaterThan(0);
  expect(resumo.semNome, `controles sem nome em "${tela}"`).toEqual([]);
}

test("telas de entrada: Entrar, código do convite e cadastro de nutri", async ({ page }) => {
  await abrir(page);
  await conferir(page, "Entrar");
  await tocar(page, "Tenho um código da minha nutri");
  await conferir(page, "Convite: código");
  await tocar(page, "Voltar para entrar");
  await tocar(page, "Sou nutricionista");
  await conferir(page, "Cadastro de nutri");
});

test("nutri: início, pacientes, agenda, alimentos, conta e ficha", async ({ page }) => {
  test.setTimeout(240_000);
  await entrar(page, NUTRI_EXEMPLO);
  await expect(page.getByRole("heading", { name: "Início" })).toBeAttached({ timeout: 30_000 });
  await conferir(page, "Início");
  for (const aba of ["Pacientes", "Agenda", "Alimentos", "Conta"]) {
    await tocar(page, aba);
    await conferir(page, aba);
  }
  await tocar(page, "Pacientes");
  await clicar(page, page.getByRole("button", { name: /Maria Exemplo/ }));
  for (const aba of ["Resumo", "Avaliações", "Evolução", "Dieta", "Lista", "Anamnese"]) {
    await tocar(page, aba);
    await conferir(page, `Ficha: ${aba}`);
  }
});

test("nutri: montador de dieta", async ({ page }) => {
  await entrar(page, NUTRI_EXEMPLO);
  await expect(page.getByRole("heading", { name: "Início" })).toBeAttached({ timeout: 30_000 });
  await tocar(page, "Pacientes");
  await clicar(page, page.getByRole("button", { name: /Maria Exemplo/ }));
  await tocar(page, "Dieta");
  await tocar(page, "Nova dieta");
  await expect(page.getByRole("heading", { name: "Montar dieta" })).toBeAttached({ timeout: 20_000 });
  await tocar(page, "Adicionar refeição");
  await conferir(page, "Montador de dieta");
});

test("paciente: hoje, dieta, compras, evolução e perfil", async ({ page }) => {
  test.setTimeout(240_000);
  await entrar(page, "maria@exemplo.dev");
  await expect(page.getByRole("heading", { name: "Hoje" })).toBeAttached({ timeout: 30_000 });
  await conferir(page, "Hoje");
  for (const aba of ["Dieta", "Compras", "Evolução", "Perfil"]) {
    await tocar(page, aba);
    await conferir(page, aba);
  }
});

