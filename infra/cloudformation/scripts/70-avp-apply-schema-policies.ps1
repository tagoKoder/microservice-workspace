param(
  [Parameter(Mandatory=$true)]
  [ValidateSet("dev","prod")]
  [string]$Env,

  [string]$ConfigPath = "",

  [string]$PolicyStoreStackName = "",

  [switch]$ReplaceAllPolicies
)

$ErrorActionPreference = "Stop"

# Resolve config
if ([string]::IsNullOrWhiteSpace($ConfigPath)) {
  $ConfigPath = Join-Path $PSScriptRoot "..\config\$Env.psd1"
}
if (!(Test-Path $ConfigPath)) {
  throw "Config no encontrado: $ConfigPath"
}
$config = Import-PowerShellDataFile $ConfigPath

$project = $config.ProjectName
if ([string]::IsNullOrWhiteSpace($project)) { $project = "bank" }

$profile = $config.AwsProfile
$region  = $config.Region

if ([string]::IsNullOrWhiteSpace($PolicyStoreStackName)) {
  $PolicyStoreStackName = "$project-$Env-70-authz-avp"
}

$schemaPath  = Join-Path $PSScriptRoot "..\authz_cedar\schema.json"
$policiesDir = Join-Path $PSScriptRoot "..\authz_cedar\policies"

if (!(Test-Path $schemaPath)) { throw "schema.json no encontrado: $schemaPath" }
if (!(Test-Path $policiesDir)) { throw "policies dir no encontrado: $policiesDir" }

function Aws([string]$args) {
  $base = "aws --region $region"
  if (-not [string]::IsNullOrWhiteSpace($profile)) { $base += " --profile $profile" }
  $cmd = "$base $args"
  Write-Host ">> $cmd"
  Invoke-Expression $cmd
}

# Get PolicyStoreId from CloudFormation outputs
$policyStoreId = (Aws "cloudformation describe-stacks --stack-name $PolicyStoreStackName --query `"Stacks[0].Outputs[?OutputKey=='PolicyStoreId'].OutputValue | [0]`" --output text").Trim()
if ([string]::IsNullOrWhiteSpace($policyStoreId) -or $policyStoreId -eq "None") {
  throw "No pude obtener PolicyStoreId desde el stack: $PolicyStoreStackName"
}
Write-Host "PolicyStoreId = $policyStoreId"

# 1) Put schema (cedarJson) via cli-input-json
$schemaRaw = Get-Content -Raw -Path $schemaPath
$putSchemaInput = @{
  policyStoreId = $policyStoreId
  definition    = @{ cedarJson = $schemaRaw }
} | ConvertTo-Json -Depth 50

$tmpSchema = Join-Path $env:TEMP ("avp-put-schema-$Env.json")
$putSchemaInput | Out-File -FilePath $tmpSchema -Encoding utf8

Aws "verifiedpermissions put-schema --cli-input-json file://$tmpSchema" | Out-Null
Start-Sleep -Seconds 3

# 2) Turn validation STRICT (recommended after schema exists)
Aws "verifiedpermissions update-policy-store --policy-store-id $policyStoreId --validation-settings `"mode=STRICT`"" | Out-Null
Start-Sleep -Seconds 3

# 3) Optionally delete all policies (DEV convenience)
if ($ReplaceAllPolicies) {
  $ids = Aws "verifiedpermissions list-policies --policy-store-id $policyStoreId --query `"policies[].policyId`" --output text"
  if (-not [string]::IsNullOrWhiteSpace($ids)) {
    $ids.Split(" ", [System.StringSplitOptions]::RemoveEmptyEntries) | ForEach-Object {
      Aws "verifiedpermissions delete-policy --policy-store-id $policyStoreId --policy-id $_" | Out-Null
    }
    Start-Sleep -Seconds 2
  }
}

# 4) Create policies from *.cedar (one file = one static policy)
Get-ChildItem -Path $policiesDir -Filter "*.cedar" | ForEach-Object {
  $name = $_.BaseName
  $stmt = Get-Content -Raw -Path $_.FullName

  $createPolicyInput = @{
    policyStoreId = $policyStoreId
    definition    = @{
      static = @{
        description = "policy:$name"
        statement   = $stmt
      }
    }
  } | ConvertTo-Json -Depth 50

  $tmpPol = Join-Path $env:TEMP ("avp-create-policy-$Env-$name.json")
  $createPolicyInput | Out-File -FilePath $tmpPol -Encoding utf8

  Aws "verifiedpermissions create-policy --cli-input-json file://$tmpPol" | Out-Null
  Start-Sleep -Seconds 1
}

Write-Host "OK: schema + policies aplicadas. (Env=$Env)"
