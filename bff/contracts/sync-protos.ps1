$ErrorActionPreference = "Stop"

$REF_FILE = Join-Path $PSScriptRoot "proto.ref.txt"
$REF = (Get-Content $REF_FILE | Select-Object -First 1).Trim()

$DEST = Join-Path $PSScriptRoot "proto"

Write-Host "==> Exportando protos desde: $REF"
Write-Host "==> Destino: $DEST"

# Limpia destino
if (Test-Path $DEST) {
  Remove-Item -Recurse -Force $DEST
}
New-Item -ItemType Directory -Force -Path $DEST | Out-Null

# Exporta del registry de buf
buf export $REF --output $DEST

Write-Host "==> OK: protos exportados"
