#!/usr/bin/env bash
#
# Проверяет, что APK подписан постоянным ключом проекта.
# Отпечаток берётся из keystore.properties (sha256=...).
# Скрипт завершается с ошибкой, если отпечаток не совпал или в APK
# отсутствует схема подписи v2/v3 (без них Android 11+ не установит пакет).
#
set -euo pipefail

APK="${1:?нужно указать путь к APK}"
BUILD_TOOLS_VERSION="${BUILD_TOOLS_VERSION:-35.0.0}"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
APKSIGNER="${ANDROID_HOME:-/usr/local/lib/android/sdk}/build-tools/${BUILD_TOOLS_VERSION}/apksigner"

if [ ! -x "$APKSIGNER" ]; then
  echo "::error::apksigner не найден: $APKSIGNER"
  exit 1
fi

# Приводим отпечаток к единому виду: только заглавные hex-символы без двоеточий.
EXPECTED="$(grep -oP '^sha256=\K.*' "$ROOT/keystore.properties" | tr -d ' \r' | tr -d ':' | tr '[:lower:]' '[:upper:]')"
if [ -z "$EXPECTED" ]; then
  echo "::error::в keystore.properties нет поля sha256"
  exit 1
fi

echo "--- apksigner verify --print-certs ---"
"$APKSIGNER" verify --print-certs "$APK"

ACTUAL="$("$APKSIGNER" verify --print-certs "$APK" \
  | grep -i "SHA-256 digest" | head -1 | awk '{print $NF}' | tr -d ' \r' | tr -d ':' | tr '[:lower:]' '[:upper:]')"

if [ -z "$ACTUAL" ]; then
  echo "::error::не удалось получить отпечаток сертификата"
  exit 1
fi

if [ "$ACTUAL" != "$EXPECTED" ]; then
  echo "::error::APK подписан другим ключом!"
  echo "  ожидалось: $EXPECTED"
  echo "  получено:  $ACTUAL"
  exit 1
fi
echo "::notice::отпечаток сертификата совпал: $ACTUAL"

# Схемы подписи: v2 обязателен для targetSdk 30+, v3 — для обновлений поверх.
SCHEMES="$("$APKSIGNER" verify -v "$APK" 2>/dev/null | grep -iE "Verified using v[0-9] scheme" || true)"
echo "$SCHEMES"
if ! echo "$SCHEMES" | grep -qi "v2 scheme"; then
  echo "::error::в APK нет схемы подписи v2 — Android 11+ его не установит"
  exit 1
fi

echo "::notice::APK подписан постоянным ключом (SHA-256 $ACTUAL)"
