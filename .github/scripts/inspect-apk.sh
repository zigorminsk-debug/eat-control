#!/usr/bin/env bash
#
# Проверяет содержимое собранного APK:
#  - манифест: версия, разрешения, компоненты;
#  - наличие справочной страницы в assets;
#  - схемы подписи.
#
set -euo pipefail

APK="${1:?нужно указать путь к APK}"
BUILD_TOOLS_VERSION="${BUILD_TOOLS_VERSION:-35.0.0}"
ANDROID_SDK="${ANDROID_HOME:-/usr/local/lib/android/sdk}"
AAPT2="${ANDROID_SDK}/build-tools/${BUILD_TOOLS_VERSION}/aapt2"
APKSIGNER="${ANDROID_SDK}/build-tools/${BUILD_TOOLS_VERSION}/apksigner"

echo "### APK: $(basename "$APK") ($(stat -c%s "$APK") байт)"

if [ -x "$AAPT2" ]; then
  echo "--- манифест ---"
  "$AAPT2" dump badging "$APK" | grep -E "^package:|^application-label:|^uses-permission|^launchable-activity|sdkVersion|targetSdkVersion" || true
fi

echo "--- состав APK ---"
unzip -l "$APK" | grep -E "assets/|\.dex|META-INF/.*\.(RSA|SF|MF)" || true

if ! unzip -l "$APK" | grep -q "assets/help/gazy-i-vzdutie.html"; then
  echo "::error::страница справки не попала в assets APK"
  exit 1
fi
if ! unzip -l "$APK" | grep -q "classes.dex"; then
  echo "::error::в APK нет classes.dex"
  exit 1
fi

if [ -x "$APKSIGNER" ]; then
  echo "--- схемы подписи ---"
  "$APKSIGNER" verify -v "$APK" | grep -E "Verified using|Number of signers" || true
fi

echo "::notice::содержимое APK проверено"
