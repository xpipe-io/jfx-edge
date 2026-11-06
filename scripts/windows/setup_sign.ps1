if (-not $env:AZURE_KEY_VAULT_URI) {
    Write-Host "AZURE_KEY_VAULT_URI is not set, the libraries are not signed"
    exit 0
}

dotnet tool install --global --version 7.0.1 AzureSignTool

echo "SIGN_PROPS=-PcodeSignCmd=$PSScriptRoot\sign.cmd -PcodeSignArgs=-" >> $env:GITHUB_ENV
