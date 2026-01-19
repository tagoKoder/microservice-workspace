param(
  [Parameter(Mandatory=$true)][ValidateSet("dev","prod")] [string]$Env
)

. "$PSScriptRoot\helpers.ps1"

Assert-Command -Name "aws"

$cfg = Load-EnvConfig -Env $Env
$profile = $cfg.AwsProfile
$region  = $cfg.Region

Write-Host "Bootstrap AWS ($Env) -> profile=$profile region=$region"

Invoke-Aws -Profile $profile -Region $region -Args @("sts","get-caller-identity")
Write-Host "OK: AWS CLI y credenciales funcionan."
