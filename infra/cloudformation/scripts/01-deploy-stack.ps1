param(
  [Parameter(Mandatory=$true)][ValidateSet("dev","prod")] [string]$Env,
  [Parameter(Mandatory=$true)][string]$StackDir,
  [Parameter(Mandatory=$true)][string]$ParamsFile,

  [switch]$PreserveFailedStack,      # usa --disable-rollback para debug
  [switch]$AutoDeleteFailedCreate    # borra stacks en ROLLBACK_COMPLETE/CREATE_FAILED antes de redeploy
)

$ErrorActionPreference = "Stop"
$PSNativeCommandUseErrorActionPreference = $false

. "$PSScriptRoot/helpers.ps1"

$RepoRoot = Resolve-RepoRoot
$config   = Load-EnvConfig -RepoRoot $RepoRoot -Env $Env

try {
  Preflight -Cfg $config

  Deploy-Stack `
    -RepoRoot $RepoRoot `
    -Env $Env `
    -StackDir $StackDir `
    -ParamsFile $ParamsFile `
    -PreserveFailedStack:$PreserveFailedStack `
    -AutoDeleteFailedCreate:$AutoDeleteFailedCreate

  Write-Host "`n🎉 Deploy OK: $($config.ProjectName)-$Env-$StackDir" -ForegroundColor Green
}
catch {
  Write-Host "`n🛑 Deploy detenido por error:" -ForegroundColor Red
  Write-Host "=== ErrorRecord ===" -ForegroundColor Yellow
  ($_ | Format-List * -Force | Out-String) | Write-Host

  if ($_.Exception) {
    Write-Host "=== Exception ===" -ForegroundColor Yellow
    Write-Host ("Type: {0}" -f $_.Exception.GetType().FullName) -ForegroundColor Yellow
    Write-Host ("Message: {0}" -f $_.Exception.Message) -ForegroundColor Yellow
    Write-Host ($_.Exception.StackTrace) -ForegroundColor Yellow
  }

  if ($_.ScriptStackTrace) {
    Write-Host "=== ScriptStackTrace ===" -ForegroundColor Yellow
    Write-Host $_.ScriptStackTrace -ForegroundColor Yellow
  }

  exit 1
}
