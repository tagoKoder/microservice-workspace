param()

. "$PSScriptRoot\helpers.ps1"

$env = "dev"
$cfg = Load-EnvConfig -Env $env

& "$PSScriptRoot\00-bootstrap-aws.ps1" -Env $env

$root = Resolve-RepoRoot

$stack00 = ("{0}-{1}-00-foundation" -f $cfg.ProjectName, $env)
$stack10 = ("{0}-{1}-10-network" -f $cfg.ProjectName, $env)

& "$PSScriptRoot\01-deploy-stack.ps1" -Env $env `
  -StackName $stack00 `
  -TemplatePath "infra\cloudformation\00-foundation\template.yml" `
  -ParamsPath  "infra\params\dev\00-foundation.json"

& "$PSScriptRoot\01-deploy-stack.ps1" -Env $env `
  -StackName $stack10 `
  -TemplatePath "infra\cloudformation\10-network\template.yml" `
  -ParamsPath  "infra\params\dev\10-network.json"
