// A5 (MOB-43): medições da web no Chromium. Só roda com E2E_MEDIR=1 (não faz parte do fluxo normal):
//   E2E_MEDIR=1 ops/e2e-wsl.sh medicoes
// Escreve build/medicoes-web.json e imprime as tabelas.
import { expect, test, type Browser } from "@playwright/test";
import { mkdirSync, readdirSync, readFileSync, statSync, writeFileSync } from "node:fs";
import { join } from "node:path";
import { brotliCompressSync, gzipSync } from "node:zlib";
import { abrir, campo } from "./apoio.ts";

test.skip(!process.env.E2E_MEDIR, "medições só com E2E_MEDIR=1");

const dist = process.env.E2E_WEB_DIST ?? "../webApp/build/dist/wasmJs/productionExecutable";
const resultado: Record<string, unknown> = { data: new Date().toISOString() };

test("tamanho do bundle (bruto, gzip -9, gzip -5 como o Caddy, brotli)", async () => {
  const linhas: Array<Record<string, string | number>> = [];
  const ler = (dir: string, rel = ""): void => {
    for (const nome of readdirSync(dir)) {
      const caminho = join(dir, nome);
      if (statSync(caminho).isDirectory()) ler(caminho, join(rel, nome));
      else if (!/\.(map|txt)$/.test(nome)) {
        const bruto = readFileSync(caminho);
        linhas.push({ arquivo: join(rel, nome), bruto: bruto.length, gzip9: gzipSync(bruto, { level: 9 }).length, gzip5: gzipSync(bruto, { level: 5 }).length, brotli: brotliCompressSync(bruto).length });
      }
    }
  };
  ler(dist);
  const total = (k: string) => linhas.reduce((s, l) => s + (l[k] as number), 0);
  const carregamento = linhas.filter((l) => /\.(wasm|js|html|css)$/.test(l.arquivo as string) && !/composeResources/.test(l.arquivo as string));
  const soma = (k: string) => carregamento.reduce((s, l) => s + (l[k] as number), 0);
  resultado.bundle = { arquivos: linhas, primeiroCarregamento: { bruto: soma("bruto"), gzip9: soma("gzip9"), gzip5: soma("gzip5"), brotli: soma("brotli") }, totalComRecursos: { bruto: total("bruto"), gzip9: total("gzip9") } };
  console.table(linhas.filter((l) => (l.bruto as number) > 50_000).map((l) => ({ ...l })));
  console.log("primeiro carregamento (wasm+js+html+css):", resultado.bundle);
});

const perfis = [
  { nome: "sem limite (loopback)", latencia: 0, descida: -1, subida: -1 },
  { nome: "4G rápido (9 Mbps, 165 ms)", latencia: 165, descida: (9 * 1000 * 1000) / 8, subida: (1.5 * 1000 * 1000) / 8 },
  { nome: "4G lento (1,6 Mbps, 150 ms)", latencia: 150, descida: (1.6 * 1000 * 1000) / 8, subida: (750 * 1000) / 8 },
];

async function abertura(browser: Browser, perfil: (typeof perfis)[number]) {
  const ctx = await browser.newContext({ locale: "pt-BR", timezoneId: "America/Sao_Paulo", viewport: { width: 390, height: 844 } });
  const page = await ctx.newPage();
  const cdp = await ctx.newCDPSession(page);
  await cdp.send("Network.enable");
  await cdp.send("Network.emulateNetworkConditions", { offline: false, latency: perfil.latencia, downloadThroughput: perfil.descida, uploadThroughput: perfil.subida });
  let bytes = 0;
  cdp.on("Network.loadingFinished", (e: { encodedDataLength: number }) => { bytes += e.encodedDataLength; });
  const medir = async () => {
    bytes = 0;
    const t0 = Date.now();
    await page.goto("/", { waitUntil: "commit" });
    await expect(page.getByRole("heading", { name: "Dieta+" })).toBeAttached({ timeout: 180_000 });
    await expect(campo(page, "E-mail")).toBeAttached();
    return { ate_formulario_ms: Date.now() - t0, bytes_transferidos: bytes };
  };
  const fria = await medir();
  const morna = await medir(); // mesma sessão do navegador: cache HTTP com revalidação (ETag/304), como em produção
  await ctx.close();
  return { fria, morna };
}

for (const perfil of perfis) {
  test(`abertura até o formulário de entrar: ${perfil.nome}`, async ({ browser }) => {
    test.setTimeout(400_000);
    const r = await abertura(browser, perfil);
    (resultado.abertura as Record<string, unknown> | undefined) ??= {};
    (resultado.abertura as Record<string, unknown>)[perfil.nome] = r;
    console.log(perfil.nome, JSON.stringify(r));
  });
}

test("autofill de senha: atributos dos campos de entrar", async ({ page }) => {
  await abrir(page);
  await expect(campo(page, "Senha")).toBeAttached();
  const info = await page.evaluate(() => {
    const achados: Array<{ tag: string; role: string | null; ariaLabel: string | null; type: string | null; autocomplete: string | null; name: string | null; contenteditable: string | null }> = [];
    let inputs = 0, forms = 0, passwordInputs = 0;
    const visitar = (n: Element) => {
      if (n.shadowRoot) [...n.shadowRoot.children].forEach(visitar);
      if (n.tagName === "INPUT") { inputs++; if ((n as HTMLInputElement).type === "password") passwordInputs++; }
      if (n.tagName === "FORM") forms++;
      if (n.getAttribute("role") === "textbox") achados.push({ tag: n.tagName, role: n.getAttribute("role"), ariaLabel: n.getAttribute("aria-label"), type: n.getAttribute("type"), autocomplete: n.getAttribute("autocomplete"), name: n.getAttribute("name"), contenteditable: n.getAttribute("contenteditable") });
      [...n.children].forEach(visitar);
    };
    visitar(document.body);
    return { inputs, passwordInputs, forms, campos: achados };
  });
  resultado.autofill = info;
  console.log("autofill:", JSON.stringify(info));
});

test.afterAll(() => {
  mkdirSync("../build", { recursive: true });
  writeFileSync("../build/medicoes-web.json", JSON.stringify(resultado, null, 2));
});
