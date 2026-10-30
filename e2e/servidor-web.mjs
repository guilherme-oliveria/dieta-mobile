// Servidor estático da web (Compose/wasmJs) para o e2e: serve o build de produção como o Caddy da produção
// (gzip, ETag + `Cache-Control: no-cache`, index.html para qualquer caminho) e encaminha /api e /legal ao backend,
// mantendo uma origem só (o cookie do refresh depende disso).
import http from "node:http";
import { createHash } from "node:crypto";
import { readFileSync, existsSync, statSync } from "node:fs";
import { extname, join, normalize } from "node:path";
import { gzipSync } from "node:zlib";

const porta = Number(process.env.E2E_WEB_PORTA ?? 8081);
const backend = new URL(process.env.E2E_BACKEND_URL ?? "http://localhost:8080");
const raiz = process.env.E2E_WEB_DIST ?? "../webApp/build/dist/wasmJs/productionExecutable";

const tipos = {
  ".html": "text/html; charset=utf-8", ".js": "text/javascript", ".css": "text/css", ".wasm": "application/wasm",
  ".json": "application/json", ".map": "application/json", ".txt": "text/plain", ".png": "image/png", ".svg": "image/svg+xml",
  ".ttf": "font/ttf", ".otf": "font/otf", ".woff2": "font/woff2",
};
const comprimiveis = new Set([".html", ".js", ".css", ".wasm", ".json", ".map", ".txt", ".svg", ".ttf", ".otf"]);
const cache = new Map();

function carregar(arquivo, ext) {
  const chave = arquivo;
  const mtime = statSync(arquivo).mtimeMs;
  const guardado = cache.get(chave);
  if (guardado && guardado.mtime === mtime) return guardado;
  const bruto = readFileSync(arquivo);
  // Nível 5 é o padrão do `encode gzip` do Caddy.
  const gz = comprimiveis.has(ext) ? gzipSync(bruto, { level: 5 }) : null;
  const novo = { mtime, bruto, gz, etag: `"${createHash("sha1").update(bruto).digest("hex").slice(0, 16)}"` };
  cache.set(chave, novo);
  return novo;
}

function encaminhar(req, res) {
  const proxy = http.request({ host: backend.hostname, port: backend.port, path: req.url, method: req.method, headers: req.headers }, (resp) => {
    res.writeHead(resp.statusCode ?? 502, resp.headers);
    resp.pipe(res);
  });
  proxy.on("error", () => { res.writeHead(502); res.end("backend indisponível"); });
  req.pipe(proxy);
}

http.createServer((req, res) => {
  const caminho = decodeURIComponent((req.url ?? "/").split("?")[0]);
  if (caminho.startsWith("/api/") || caminho.startsWith("/legal/")) return encaminhar(req, res);
  let arquivo = normalize(join(raiz, caminho));
  if (!arquivo.startsWith(normalize(raiz))) { res.writeHead(403); return res.end(); }
  if (!existsSync(arquivo) || statSync(arquivo).isDirectory()) arquivo = join(raiz, "index.html");
  const ext = extname(arquivo);
  const { bruto, gz, etag } = carregar(arquivo, ext);
  const cabecalhos = { "content-type": tipos[ext] ?? "application/octet-stream", "cache-control": "no-cache", etag, vary: "Accept-Encoding" };
  if (req.headers["if-none-match"] === etag) { res.writeHead(304, cabecalhos); return res.end(); }
  if (gz && /\bgzip\b/.test(String(req.headers["accept-encoding"] ?? ""))) {
    res.writeHead(200, { ...cabecalhos, "content-encoding": "gzip", "content-length": gz.length });
    return res.end(gz);
  }
  res.writeHead(200, { ...cabecalhos, "content-length": bruto.length });
  res.end(bruto);
}).listen(porta, "127.0.0.1", () => console.log(`web em http://localhost:${porta} (backend ${backend.origin})`));
