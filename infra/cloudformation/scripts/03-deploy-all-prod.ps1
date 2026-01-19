param()

. "$PSScriptRoot\helpers.ps1"

$env = "prod"
$cfg = Load-EnvConfig -Env $env

& "$PSScriptRoot\00-bootstrap-aws.ps1" -Env $env

$stack00 = ("{0}-{1}-00-foundation" -f $cfg.ProjectName, $env)
$stack10 = ("{0}-{1}-10-network" -f $cfg.ProjectName, $env)

& "$PSScriptRoot\01-deploy-stack.ps1" -Env $env `
  -StackName $stack00 `
  -TemplatePath "infra\cloudformation\00-foundation\template.yml" `
  -ParamsPath  "infra\params\prod\00-foundation.json"

& "$PSScriptRoot\01-deploy-stack.ps1" -Env $env `
  -StackName $stack10 `
  -TemplatePath "infra\cloudformation\10-network\template.yml" `
  -ParamsPath  "infra\params\prod\10-network.json"
