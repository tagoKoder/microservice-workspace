param(
  [Parameter(Mandatory=$true)][string]$Env,
  [Parameter(Mandatory=$true)][ValidateSet("00-foundation","10-network", "40-identity-cognito")][string]$StackId
)

$cfg = Import-PowerShellDataFile "infra/config/$Env.psd1"

$region = $cfg.Region

$stackName = switch ($StackId) {
  "00-foundation" { $cfg.StackName } # tu config actual
  "10-network"    { $cfg.NetworkStackName }
}

$template   = "infra/cloudformation/$StackId/template.yaml"
$paramsFile = "infra/params/$Env/$StackId.json"

$changeSetName = "cs-$StackId-{0}" -f (Get-Date -Format yyyyMMddHHmmss)

Write-Host "Region:     $region"
Write-Host "StackName:  $stackName"
Write-Host "Template:   $template"
Write-Host "Params:     $paramsFile"
Write-Host "ChangeSet:  $changeSetName"

$exists = $false
try {
  aws cloudformation describe-stacks --region $region --stack-name $stackName | Out-Null
  $exists = $true
} catch { $exists = $false }

$csType = if ($exists) { "UPDATE" } else { "CREATE" }

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

aws cloudformation wait change-set-create-complete `
  --region $region `
  --stack-name $stackName `
  --change-set-name $changeSetName

aws cloudformation describe-change-set `
  --region $region `
  --stack-name $stackName `
  --change-set-name $changeSetName `
  --output table

aws cloudformation execute-change-set `
  --region $region `
  --stack-name $stackName `
  --change-set-name $changeSetName

if ($csType -eq "CREATE") {
  aws cloudformation wait stack-create-complete --region $region --stack-name $stackName
} else {
  aws cloudformation wait stack-update-complete --region $region --stack-name $stackName
}

aws cloudformation describe-stacks `
  --region $region `
  --stack-name $stackName `
  --query "Stacks[0].Outputs" `
  --output table
