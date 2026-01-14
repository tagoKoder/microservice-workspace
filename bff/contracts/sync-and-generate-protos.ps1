$ErrorActionPreference = "Stop"
& "$PSScriptRoot\sync-protos.ps1"
buf generate "$PSScriptRoot\proto" --template "$PSScriptRoot\buf.gen.yaml"
Write-Host "==> OK: genproto actualizado en internal/genproto"
