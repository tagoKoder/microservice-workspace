param(
  [Parameter(Mandatory=$true)][string]$Env
)

$cfg = Import-PowerShellDataFile "infra/config/$Env.psd1"

$region     = $cfg.Region
$stackName  = $cfg.StackName
$template   = "infra/cloudformation/00-foundation/template.yaml"
$paramsFile = "infra/params/$Env/00-foundation.json"

$changeSetName = "cs-00-foundation-{0}" -f (Get-Date -Format yyyyMMddHHmmss)

Write-Host "Region:     $region"
Write-Host "StackName:  $stackName"
Write-Host "Template:   $template"
Write-Host "Params:     $paramsFile"
Write-Host "ChangeSet:  $changeSetName"

# 1) Detectar si el stack existe
$exists = $false
try {
  aws cloudformation describe-stacks --region $region --stack-name $stackName | Out-Null
  $exists = $true
} catch {
  $exists = $false
}

$csType = if ($exists) { "UPDATE" } else { "CREATE" }

Write-Host "Change set type: $csType"

# 2) Crear Change Set
aws cloudformation create-change-set `
  --region $region `
  --stack-name $stackName `
  --change-set-name $changeSetName `
  --change-set-type $csType `
  --template-body ("file://{0}" -f (Resolve-Path $template)) `
  --parameters ("file://{0}" -f (Resolve-Path $paramsFile)) `
  --capabilities CAPABILITY_NAMED_IAM `
  --tags `
    app=imaginarybank `
    system=banking `
    env=$($cfg.Env) `
    owner=$($cfg.Owner) `
    cost_center=$($cfg.CostCenter) `
    data_classification=$($cfg.DataClassification) `
    managed_by=cloudformation `
    compliance=$($cfg.Compliance) `
    repo=$($cfg.Repo)

# 3) Esperar a que el change set esté listo
aws cloudformation wait change-set-create-complete `
  --region $region `
  --stack-name $stackName `
  --change-set-name $changeSetName

# 4) Mostrar resumen del change set
aws cloudformation describe-change-set `
  --region $region `
  --stack-name $stackName `
  --change-set-name $changeSetName `
  --output table

# 5) Ejecutar change set
aws cloudformation execute-change-set `
  --region $region `
  --stack-name $stackName `
  --change-set-name $changeSetName

# 6) Esperar finalización
if ($csType -eq "CREATE") {
  aws cloudformation wait stack-create-complete --region $region --stack-name $stackName
} else {
  aws cloudformation wait stack-update-complete --region $region --stack-name $stackName
}

# 7) Imprimir outputs
aws cloudformation describe-stacks `
  --region $region `
  --stack-name $stackName `
  --query "Stacks[0].Outputs" `
  --output table
