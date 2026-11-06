#!/bin/bash
# The jfx build passes the file to sign as last argument
exec /usr/bin/codesign --force --options runtime --timestamp --sign "$MACOS_DEVELOPER_ID_APPLICATION_CERTIFICATE_NAME" "${@: -1}"
