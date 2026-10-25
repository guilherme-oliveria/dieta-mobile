#!/bin/bash
set -euo pipefail

task_root="$(cd "$(dirname "$0")/.." && pwd)"
runtime="$(xcrun simctl list runtimes -j | /usr/bin/python3 -c 'import json,sys; r=[x for x in json.load(sys.stdin)["runtimes"] if x.get("isAvailable") and ".iOS-" in x["identifier"]]; print(sorted(r,key=lambda x:tuple(map(int,x["version"].split("."))))[-1]["identifier"] if r else "")')"
if [ -z "$runtime" ]; then
    echo "Runtime iOS indisponível para XCTest do cofre." >&2
    exit 1
fi
simulador="$(xcrun simctl create "DietaMaisCofre-$$" com.apple.CoreSimulator.SimDeviceType.iPhone-16 "$runtime")"
limpar_simulador() {
    resultado=$?
    trap - EXIT
    xcrun simctl shutdown "$simulador" >/dev/null 2>&1 || true
    xcrun simctl delete "$simulador" >/dev/null 2>&1 || true
    exit "$resultado"
}
trap limpar_simulador EXIT
xcrun simctl boot "$simulador"
xcrun simctl bootstatus "$simulador" -b

# O framework já é dependência da tarefa Gradle; não iniciar outro Gradle dentro do Xcode.
export OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED=YES
xcodebuild test -quiet \
    -project "$task_root/iosApp/iosApp.xcodeproj" -scheme TestesCofre \
    -destination "platform=iOS Simulator,id=$simulador" \
    -derivedDataPath "$task_root/shared/build/ios-keychain-host" \
    -resultBundlePath "$task_root/shared/build/keychain-host-$$.xcresult" \
    -test-timeouts-enabled YES -maximum-test-execution-time-allowance 180 \
    CODE_SIGN_IDENTITY=- CODE_SIGNING_ALLOWED=YES DEVELOPMENT_TEAM= \
    "CODE_SIGN_ENTITLEMENTS=$task_root/iosApp/iosAppTests/KeychainHost.entitlements" \
    "FRAMEWORK_SEARCH_PATHS=$task_root/shared/build/bin/iosSimulatorArm64/debugFramework" \
    "OTHER_LDFLAGS=-framework Shared -lsqlite3"
