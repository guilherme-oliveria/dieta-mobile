#!/usr/bin/env bash
# Teste de ponta a ponta da web (A4): sobe Postgres 18, backend e a web de produção, e roda o Playwright (Chromium) contra eles.
# Uso: ops/e2e-wsl.sh [argumentos do playwright test]   (ex.: ops/e2e-wsl.sh -g isolamento)
# Variáveis: DIETA_BACKEND_DIR (padrão ../dieta-backend), E2E_SEM_BUILD=1 (reaproveita a web já construída),
#            E2E_MANTER=1 (não derruba backend e banco no fim), E2E_PG_PORTA (padrão 5433).
# Resultado: e2e/relatorio (HTML), build/e2e-wsl.txt (resumo) e build/e2e-*.log. Nada disso vai para o Git.
set -uo pipefail
cd "$(dirname "$0")/.."
raiz=$PWD
backend=${DIETA_BACKEND_DIR:-$raiz/../dieta-backend}
pg_porta=${E2E_PG_PORTA:-5433}
mkdir -p build
resumo=build/e2e-wsl.txt
echo "commit: $(git rev-parse --short HEAD) ($(git rev-parse --abbrev-ref HEAD))" | tee "$resumo"
echo "backend: $(git -C "$backend" rev-parse --short HEAD) ($(git -C "$backend" rev-parse --abbrev-ref HEAD))" | tee -a "$resumo"
echo "início: $(date -Is)" | tee -a "$resumo"

[ -d "$backend" ] || { echo "Backend não encontrado em $backend (defina DIETA_BACKEND_DIR)." | tee -a "$resumo"; exit 2; }
if ! command -v java >/dev/null && [ -s "$HOME/.sdkman/bin/sdkman-init.sh" ]; then source "$HOME/.sdkman/bin/sdkman-init.sh"; fi
[ -d "$HOME/.local/node/bin" ] && export PATH="$HOME/.local/node/bin:$PATH"
# Bibliotecas do Chromium instaladas sem root (apt-get download + dpkg -x): ver e2e/README.md.
libs_chromium="$HOME/.local/chromium-libs/usr/lib/x86_64-linux-gnu"
[ -d "$libs_chromium" ] && export LD_LIBRARY_PATH="$libs_chromium:${LD_LIBRARY_PATH:-}"
for ferramenta in java node npm docker curl; do
	command -v "$ferramenta" >/dev/null || { echo "Falta '$ferramenta' no PATH (veja e2e/README.md)." | tee -a "$resumo"; exit 2; }
done

pid_backend=""; pid_web=""
limpar() {
	[ -n "$pid_web" ] && kill "$pid_web" 2>/dev/null
	if [ -n "$pid_backend" ] && [ -z "${E2E_MANTER:-}" ]; then kill -- -"$pid_backend" 2>/dev/null; fi
	if [ -z "${E2E_MANTER:-}" ]; then docker compose -f e2e/compose.e2e.yaml down -v >/dev/null 2>&1; fi
}
trap limpar EXIT

echo "== Postgres 18 (porta $pg_porta, banco novo)"
export E2E_PG_PORTA=$pg_porta
docker compose -f e2e/compose.e2e.yaml down -v >/dev/null 2>&1
docker compose -f e2e/compose.e2e.yaml up -d --wait >build/e2e-postgres.log 2>&1 || { echo "Postgres não subiu (build/e2e-postgres.log)" | tee -a "$resumo"; exit 1; }

if [ -z "${E2E_SEM_BUILD:-}" ]; then
	echo "== Web (wasmJsBrowserDistribution)"
	./gradlew :webApp:wasmJsBrowserDistribution --max-workers=2 >build/e2e-web.log 2>&1 || { echo "Build da web falhou (build/e2e-web.log)" | tee -a "$resumo"; exit 1; }
fi

echo "== Backend (bootRun com dados de exemplo, perfil dev)"
(
	cd "$backend"
	export DB_URL="jdbc:postgresql://localhost:$pg_porta/dietamais" SPRING_DOCKER_COMPOSE_ENABLED=false
	# A web e a API ficam na mesma origem (o cookie do refresh depende disso); o link do convite precisa ser HTTPS (o app recusa outro esquema).
	export WEB_ORIGEM=http://localhost:8081 CORS_ORIGENS=http://localhost:8081 EMAIL_URL_CONVITE=https://localhost:8081/convite
	# A suíte faz dezenas de logins do mesmo IP; o limite padrão (10 por janela) travaria os cenários seguintes.
	export DIETA_AUTH_LOGIN_LIMITE_IP=1000
	exec setsid ./gradlew bootRun --args=--dieta.dev.dados-exemplo=true --max-workers=2
) >build/e2e-backend.log 2>&1 &
pid_backend=$!
for _ in $(seq 1 180); do
	curl -sf http://localhost:8080/actuator/health >/dev/null && break
	kill -0 "$pid_backend" 2>/dev/null || { echo "Backend caiu (build/e2e-backend.log)" | tee -a "$resumo"; exit 1; }
	sleep 2
done
curl -sf http://localhost:8080/actuator/health >/dev/null || { echo "Backend não ficou saudável em 6 min (build/e2e-backend.log)" | tee -a "$resumo"; exit 1; }

echo "== Servidor da web (porta 8081, /api para o backend)"
(cd e2e && exec node servidor-web.mjs) >build/e2e-servidor-web.log 2>&1 &
pid_web=$!
sleep 1

echo "== Playwright"
cd e2e
[ -d node_modules ] || npm install --no-audit --no-fund >../build/e2e-npm.log 2>&1 || { echo "npm install falhou (build/e2e-npm.log)" | tee -a "../$resumo"; exit 1; }
npx playwright install chromium >>../build/e2e-npm.log 2>&1
npx playwright test "$@"
codigo=$?
cd "$raiz"
echo "fim: $(date -Is)" | tee -a "$resumo"
[ "$codigo" -eq 0 ] && echo "RESULTADO: OK" | tee -a "$resumo" || echo "RESULTADO: FALHOU (relatório em e2e/relatorio)" | tee -a "$resumo"
exit "$codigo"
