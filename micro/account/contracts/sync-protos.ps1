$ErrorActionPreference = "Stop"

$REF = "buf.build/imaginarybank/bank:v0.0.4"
$OUT = Join-Path $PSScriptRoot "..\src\main\proto"

Write-Host "==> Exportando protos desde: $REF"
Write-Host "==> Destino: $OUT"

# Limpia solo lo que vas a exportar (opcional)
# Remove-Item -Recurse -Force (Join-Path $OUT "bank\accounts") -ErrorAction SilentlyContinue

buf export $REF --output $OUT --path bank/accounts/v1

Write-Host "DONE (protos exportados)."
