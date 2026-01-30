param()

$EnvName = "prod"
$RepoRoot = Resolve-Path "$PSScriptRoot/../.."
. "$PSScriptRoot/helpers.ps1"

$config = Load-EnvConfig -RepoRoot $RepoRoot -Env $EnvName

Deploy-Stack -RepoRoot $RepoRoot -Env $EnvName -StackDir "00-foundation" -ParamsFile "infra/params/prod/00-foundation.json"
Deploy-Stack -RepoRoot $RepoRoot -Env $EnvName -StackDir "10-network"   -ParamsFile "infra/params/prod/10-network.json"
Deploy-Stack -RepoRoot $RepoRoot -Env $EnvName -StackDir "20-data"      -ParamsFile "infra/params/prod/20-data.json"
Deploy-Stack -RepoRoot $RepoRoot -Env $EnvName -StackDir "30-identity-cognito" -ParamsFile "infra/params/prod/30-identity-cognito.json"

Deploy-Stack -RepoRoot $RepoRoot -Env $EnvName -StackDir "70-authz-avp" -ParamsFile "infra/params/prod/70-authz-avp.json"
& "$PSScriptRoot/70-apply-avp-schema-policies.ps1" -Env $EnvName -RepoRoot $RepoRoot

Deploy-Stack -RepoRoot $RepoRoot -Env $EnvName -StackDir "80-messaging" -ParamsFile "infra/params/prod/80-messaging.json"

Deploy-Stack -RepoRoot $RepoRoot -Env $EnvName -StackDir "50-compute-ecs" -ParamsFile "infra/params/prod/50-compute-ecs.json"
Deploy-Stack -RepoRoot $RepoRoot -Env $EnvName -StackDir "60-audit-observability" -ParamsFile "infra/params/prod/60-audit-observability.json"
