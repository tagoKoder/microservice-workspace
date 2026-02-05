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
  $RepoRootGuess = (Resolve-Path (Join-Path $PSScriptRoot "..\..\..")).Path  # sube scripts -> cloudformation -> infra -> repo
  $ConfigPath = Join-Path $RepoRootGuess "infra/config/$Env.psd1"
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

$RepoRootGuess = (Resolve-Path (Join-Path $PSScriptRoot "..\..\..")).Path
$schemaPath  = Join-Path $RepoRootGuess "infra/authz/cedar/schema.json"
$policiesDir = Join-Path $RepoRootGuess "infra/authz/cedar/policies"


if (!(Test-Path $schemaPath)) { throw "schema.json no encontrado: $schemaPath" }
if (!(Test-Path $policiesDir)) { throw "policies dir no encontrado: $policiesDir" }

function Invoke-AwsCli([string[]]$ArgsList) {
  $cmd = @("aws.exe", "--no-cli-pager", "--region", $region)
  if (-not [string]::IsNullOrWhiteSpace($profile)) { $cmd += @("--profile", $profile) }
  $cmd += $ArgsList

  Write-Host (">> " + ($cmd -join " "))
  $out = & $cmd[0] $cmd[1..($cmd.Count-1)] 2>&1
  if ($LASTEXITCODE -ne 0) {
    throw "aws.exe failed (exit=$LASTEXITCODE): $out"
  }
  return ($out | Out-String)
}


# Get PolicyStoreId from CloudFormation outputs
$policyStoreId = (Invoke-AwsCli @("cloudformation", "describe-stacks", "--stack-name", $PolicyStoreStackName, "--query", "Stacks[0].Outputs[?OutputKey=='PolicyStoreId'].OutputValue | [0]", "--output", "text")).Trim()
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

$tmpSchemaFull = (Resolve-Path $tmpSchema).Path
$tmpSchemaUri  = "file:///" + ($tmpSchemaFull -replace "\\","/")
Invoke-AwsCli @("verifiedpermissions", "put-schema", "--cli-input-json", $tmpSchemaUri) | Out-Null
Start-Sleep -Seconds 3

# 2) Turn validation STRICT (recommended after schema exists)
Invoke-AwsCli @("verifiedpermissions", "update-policy-store", "--policy-store-id", $policyStoreId, "--validation-settings", "mode=STRICT") | Out-Null
Start-Sleep -Seconds 3

# 3) Optionally delete all policies (DEV convenience)
if ($ReplaceAllPolicies) {
  $ids = Invoke-AwsCli @("verifiedpermissions", "list-policies", "--policy-store-id", $policyStoreId, "--query", "policies[].policyId", "--output", "text")
  if (-not [string]::IsNullOrWhiteSpace($ids)) {
    $ids.Split(" ", [System.StringSplitOptions]::RemoveEmptyEntries) | ForEach-Object {
      Invoke-AwsCli @("verifiedpermissions", "delete-policy", "--policy-store-id", $policyStoreId, "--policy-id", $_) | Out-Null
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
  
  $tmpPolFull = (Resolve-Path $tmpPol).Path
  $tmpPolUri  = "file:///" + ($tmpPolFull -replace "\\","/")
  Invoke-AwsCli @("verifiedpermissions", "create-policy", "--cli-input-json", $tmpPolUri) | Out-Null

  Start-Sleep -Seconds 1
}

Write-Host "OK: schema + policies aplicadas. (Env=$Env)"
