param(
  [Parameter(Mandatory=$true)][ValidateSet("dev","prod")] [string]$Env,
  [Parameter(Mandatory=$true)][string]$StackName,
  [Parameter(Mandatory=$true)][string]$TemplatePath,
  [Parameter(Mandatory=$true)][string]$ParamsPath
)

. "$PSScriptRoot\helpers.ps1"

$cfg = Load-EnvConfig -Env $Env
$profile = $cfg.AwsProfile
$region  = $cfg.Region

$root = Resolve-RepoRoot

# Permite pasar rutas relativas desde repo root
$tpl = (Resolve-Path (Join-Path $root $TemplatePath)).Path
$par = (Resolve-Path (Join-Path $root $ParamsPath)).Path

Deploy-CfnStack `
  -Profile $profile `
  -Region $region `
  -StackName $StackName `
  -TemplatePath $tpl `
  -ParamsPath $par `
  -ProjectName $cfg.ProjectName `
  -Env $Env

Write-Host "OK: $StackName"
