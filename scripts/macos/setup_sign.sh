#!/bin/bash
set -e

if [ -z "$MACOS_DEVELOPER_ID_APPLICATION_CERTIFICATE" ]; then
  echo "MACOS_DEVELOPER_ID_APPLICATION_CERTIFICATE is not set, the libraries are not signed"
  exit 0
fi

KEYCHAIN="$RUNNER_TEMP/codesign.keychain-db"
KEYCHAIN_PASSWORD=$(openssl rand -base64 24)
echo "$MACOS_DEVELOPER_ID_APPLICATION_CERTIFICATE" | base64 --decode > "$RUNNER_TEMP/certificate.p12"
security create-keychain -p "$KEYCHAIN_PASSWORD" "$KEYCHAIN"
security set-keychain-settings -lut 21600 "$KEYCHAIN"
security unlock-keychain -p "$KEYCHAIN_PASSWORD" "$KEYCHAIN"
security import "$RUNNER_TEMP/certificate.p12" -k "$KEYCHAIN" -P "$MACOS_DEVELOPER_ID_APPLICATION_CERTIFICATE_PASSWORD" -T /usr/bin/codesign
security set-key-partition-list -S apple-tool:,apple:,codesign: -s -k "$KEYCHAIN_PASSWORD" "$KEYCHAIN"
security list-keychains -d user -s "$KEYCHAIN" $(security list-keychains -d user | sed 's/"//g')

echo "SIGN_PROPS=-PcodeSignCmd=$(cd "$(dirname "$0")" && pwd)/sign.sh -PcodeSignArgs=-" >> "$GITHUB_ENV"
