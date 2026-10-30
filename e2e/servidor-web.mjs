// Servidor estático da web (Compose/wasmJs) para o e2e: serve o build de produção, devolve index.html para qualquer caminho
// (como o Caddy com try_files) e encaminha /api e /legal ao backend, mantendo uma origem só (o cookie do refresh depende disso).
import http from "node:http";
import { createReadStream, existsSync, statSync } from "node:fs";
import { extname, join, normalize } from "node:path";

const porta = Number(process.env.E2E_WEB_PORTA ?? 8081);
const backend = new URL(process.env.E2E_BACKEND_URL ?? "http://localhost:8080");
const raiz = process.env.E2E_WEB_DIST ?? "../webApp/build/dist/wasmJs/productionExecutable";

const tipos = {
  ".html": "text/html; charset=utf-8", ".js": "text/javascript", ".css": "text/css", ".wasm": "application/wasm",
  ".json": "application/json", ".map": "application/json", ".txt": "text/plain", ".png": "image/png", ".svg": "image/svg+xml",
  ".ttf": "font/ttf", ".otf": "font/otf", ".woff2": "font/woff2",
};

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
  res.writeHead(200, { "content-type": tipos[extname(arquivo)] ?? "application/octet-stream" });
  createReadStream(arquivo).pipe(res);
}).listen(porta, "127.0.0.1", () => console.log(`web em http://localhost:${porta} (backend ${backend.origin})`));
