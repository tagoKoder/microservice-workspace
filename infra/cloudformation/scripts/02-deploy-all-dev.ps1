param(
  [switch]$PreserveFailedStack,
  [switch]$AutoDeleteFailedCreate
)

$ErrorActionPreference = "Stop"

$EnvName = "dev"
. "$PSScriptRoot/helpers.ps1"

$RepoRoot = Resolve-RepoRoot
$config = Load-EnvConfig -RepoRoot $RepoRoot -Env $EnvName

try {
  Preflight -Cfg $config

  # Orden “en piedra”
  Deploy-Stack -RepoRoot $RepoRoot -Env $EnvName -StackDir "00-foundation" -ParamsFile "infra/params/dev/00-foundation.json" -PreserveFailedStack:$PreserveFailedStack -AutoDeleteFailedCreate:$AutoDeleteFailedCreate
  Deploy-Stack -RepoRoot $RepoRoot -Env $EnvName -StackDir "10-network"   -ParamsFile "infra/params/dev/10-network.json"   -PreserveFailedStack:$PreserveFailedStack -AutoDeleteFailedCreate:$AutoDeleteFailedCreate
  Deploy-Stack -RepoRoot $RepoRoot -Env $EnvName -StackDir "20-data"      -ParamsFile "infra/params/dev/20-data.json"      -PreserveFailedStack:$PreserveFailedStack -AutoDeleteFailedCreate:$AutoDeleteFailedCreate
  Deploy-Stack -RepoRoot $RepoRoot -Env $EnvName -StackDir "30-identity-cognito" -ParamsFile "infra/params/dev/30-identity-cognito.json" -PreserveFailedStack:$PreserveFailedStack -AutoDeleteFailedCreate:$AutoDeleteFailedCreate

  Deploy-Stack -RepoRoot $RepoRoot -Env $EnvName -StackDir "70-authz-avp" -ParamsFile "infra/params/dev/70-authz-avp.json" -PreserveFailedStack:$PreserveFailedStack -AutoDeleteFailedCreate:$AutoDeleteFailedCreate

  # Post-step AVP: si falla, corta aquí
  & "$PSScriptRoot/70-apply-avp-schema-policies.ps1" -Env $EnvName -RepoRoot $RepoRoot
  if ($LASTEXITCODE -ne 0) { throw "AVP apply schema/policies failed (exit=$LASTEXITCODE)" }

  Deploy-Stack -RepoRoot $RepoRoot -Env $EnvName -StackDir "80-messaging" -ParamsFile "infra/params/dev/80-messaging.json" -PreserveFailedStack:$PreserveFailedStack -AutoDeleteFailedCreate:$AutoDeleteFailedCreate

  Deploy-Stack -RepoRoot $RepoRoot -Env $EnvName -StackDir "50-compute-ecs" -ParamsFile "infra/params/dev/50-compute-ecs.json" -PreserveFailedStack:$PreserveFailedStack -AutoDeleteFailedCreate:$AutoDeleteFailedCreate
  Deploy-Stack -RepoRoot $RepoRoot -Env $EnvName -StackDir "60-audit-observability" -ParamsFile "infra/params/dev/60-audit-observability.json" -PreserveFailedStack:$PreserveFailedStack -AutoDeleteFailedCreate:$AutoDeleteFailedCreate

  Write-Host "`n🎉 Deploy DEV completo OK" -ForegroundColor Green
}
catch {
  Write-Host "`n🛑 Deploy DEV detenido por error:" -ForegroundColor Red
  Write-Host "   $($_.Exception.Message)" -ForegroundColor Red
  exit 1
}
