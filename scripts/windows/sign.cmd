@echo off
rem The jfx build passes the file to sign as second argument. --skip-signed keeps the signature of the Microsoft runtime DLLs
"%USERPROFILE%\.dotnet\tools\azuresigntool.exe" sign --skip-signed -kvu "%AZURE_KEY_VAULT_URI%" -kvt "%AZURE_TENANT_ID%" -kvi "%AZURE_CLIENT_ID%" -kvs "%AZURE_CLIENT_SECRET%" -kvc "%AZURE_CERT_NAME%" -tr http://timestamp.globalsign.com/tsa/r45standard -td sha256 -fd sha256 "%~2"
