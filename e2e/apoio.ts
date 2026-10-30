// Apoio do e2e da web (Compose/wasmJs).
//
// O app é desenhado num <canvas>, mas a árvore de semantics do Compose chega ao DOM como elementos posicionados sobre o canvas
// (dentro de um shadow root aberto, que o Playwright atravessa): `role`, `aria-label` e texto. Por isso os seletores são
// getByRole/getByLabel/getByText. Dois cuidados:
//  - o canvas fica por cima e "intercepta" o clique: clicamos nas coordenadas do elemento (page.mouse), nunca com locator.click();
//  - os campos de texto são <div contenteditable>: nada de fill(); clica e digita pelo teclado.
import { expect, type Locator, type Page } from "@playwright/test";

export const SENHA_EXEMPLO = "teste123";
export const NUTRI_EXEMPLO = "nutri@exemplo.dev";

/** Abre a web e espera o Compose publicar a primeira árvore de semantics. */
export async function abrir(page: Page, caminho = "/") {
  await page.goto(caminho);
  await expect(page.getByRole("heading").first()).toBeVisible({ timeout: 60_000 });
}

async function caixa(loc: Locator) {
  await expect(loc.first()).toBeAttached({ timeout: 20_000 });
  const c = await loc.first().boundingBox();
  if (!c) throw new Error(`Sem posição para ${loc}`);
  return c;
}

/** Rola (roda do mouse sobre o canvas) até o elemento caber na janela e devolve o ponto central. */
async function trazer(page: Page, loc: Locator) {
  const vp = page.viewportSize()!;
  for (let i = 0; i < 25; i++) {
    const c = await caixa(loc);
    // Sem layout (caixa 0x0) é elemento fora da janela: o Compose só posiciona o que está perto da área visível.
    const semLayout = c.width === 0 && c.height === 0;
    const dentro = !semLayout && c.y >= 0 && c.y + c.height <= vp.height && c.x >= 0 && c.x + c.width <= vp.width;
    if (dentro) return { x: c.x + c.width / 2, y: c.y + c.height / 2 };
    // Folhas ancoradas embaixo rolam com a roda sobre elas; o ponto fica no meio da janela.
    await page.mouse.move(vp.width / 2, vp.height * 0.7);
    await page.mouse.wheel(0, !semLayout && c.y < 0 ? -300 : 300);
    await page.waitForTimeout(250);
  }
  throw new Error(`Não consegui trazer ${loc} para a janela`);
}

export async function clicar(page: Page, loc: Locator) {
  const { x, y } = await trazer(page, loc);
  await page.mouse.click(x, y);
  await page.waitForTimeout(150);
}

export const botao = (page: Page, nome: string | RegExp) => page.getByRole("button", { name: nome, exact: typeof nome === "string" });
export const campo = (page: Page, nome: string | RegExp) => page.getByRole("textbox", { name: nome, exact: typeof nome === "string" });

export async function digitar(page: Page, nome: string | RegExp, texto: string) {
  await clicar(page, campo(page, nome));
  await page.keyboard.type(texto, { delay: 5 });
}

export async function tocar(page: Page, nome: string | RegExp) {
  await clicar(page, botao(page, nome));
}

export async function veTexto(page: Page, texto: string | RegExp, timeout = 20_000) {
  await expect(page.getByText(texto).first()).toBeAttached({ timeout });
}

export async function entrar(page: Page, email: string, senha = SENHA_EXEMPLO) {
  await abrir(page);
  await digitar(page, "E-mail", email);
  await digitar(page, "Senha", senha);
  await tocar(page, "Entrar");
}

export async function sair(page: Page) {
  await tocar(page, "Sair");
  await expect(botao(page, "Entrar")).toBeAttached({ timeout: 20_000 });
}

/** Toca no botão até ele sair da árvore (diálogos animam ao abrir e um clique durante a animação erra o alvo). */
export async function tocarAteSumir(page: Page, nome: string | RegExp, tentativas = 6) {
  for (let i = 0; i < tentativas; i++) {
    if ((await botao(page, nome).count()) === 0) return;
    await page.waitForTimeout(500);
    if ((await botao(page, nome).count()) === 0) return;
    await clicar(page, botao(page, nome)).catch(() => {});
    await page.waitForTimeout(700);
  }
  await expect(botao(page, nome)).toHaveCount(0);
}
