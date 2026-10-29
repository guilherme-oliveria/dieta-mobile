#!/usr/bin/env bash
# Valida o app como o CI faz, no WSL do Guilherme: testes comuns, Android (host), build do app Android e da web.
# iOS só compila no Mac (fica para o CI). Uso: ops/validar-wsl.sh -> resultado em build/validacao-wsl.txt
set -uo pipefail
cd "$(dirname "$0")/.."
saida=build/validacao-wsl.txt
mkdir -p build
{
	echo "commit: $(git rev-parse --short HEAD) ($(git rev-parse --abbrev-ref HEAD))"
	echo "início: $(date -Is)"
} | tee "$saida"
falhou=0
for tarefa in ":shared:jvmTest" ":shared:testAndroidHostTest" ":androidApp:assembleDebug" ":webApp:wasmJsBrowserDistribution"; do
	echo "== $tarefa" | tee -a "$saida"
	./gradlew "$tarefa" --max-workers=2 >>build/validacao-wsl.log 2>&1 && echo "ok" | tee -a "$saida" || { echo "FALHOU (log em build/validacao-wsl.log)" | tee -a "$saida"; falhou=1; }
done
echo "fim: $(date -Is)" | tee -a "$saida"
[ "$falhou" -eq 0 ] && echo "RESULTADO: OK" | tee -a "$saida" || echo "RESULTADO: FALHOU" | tee -a "$saida"
exit "$falhou"
