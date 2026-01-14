$ref = Get-Content "$PSScriptRoot\proto.ref.txt" -Raw
$ref = $ref.Trim()

Write-Host "==> Exportando protos desde: $ref"

$dest = Join-Path $PSScriptRoot "..\src\main\proto"
Write-Host "==> Destino: $dest"

if (!(Test-Path $dest)) { New-Item -ItemType Directory -Force -Path $dest | Out-Null }

buf export $ref --output $dest

Write-Host "DONE (protos exportados)."
