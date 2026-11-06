#!/bin/bash
# Usage: scripts/macos/notarize.sh <jfx checkout>
set -e

if [ -z "$MACOS_DEVELOPER_ID_APPLICATION_CERTIFICATE" ]; then
  echo "MACOS_DEVELOPER_ID_APPLICATION_CERTIFICATE is not set, the libraries are not notarized"
  exit 0
fi

mkdir "$RUNNER_TEMP/notarize"
find "$1/modules" -path "*/build/module-lib/*.dylib" -exec cp {} "$RUNNER_TEMP/notarize" \;
ditto -c -k --keepParent "$RUNNER_TEMP/notarize" "$RUNNER_TEMP/notarize.zip"
xcrun notarytool submit "$RUNNER_TEMP/notarize.zip" --wait \
  --apple-id "$MACOS_NOTARIZATION_APPLE_ID" \
  --password "$MACOS_NOTARIZATION_APP_SPECIFIC_PASSWORD" \
  --team-id "$MACOS_NOTARIZATION_TEAM_ID"
