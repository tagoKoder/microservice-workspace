param(
  [Parameter(Mandatory=$true)]
  [ValidateSet("dev","prod")]
  [string]$Env,

  [string]$ConfigPath = "",
  [string]$PolicyStoreStackName = "",

  [switch]$ReplaceAllPolicies,
  [switch]$DebugSteps
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

function Stamp([string]$msg) {
  $ts = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss.fff")
  Write-Host "[$ts] $msg"
}

function Strip-Utf8Bom([string]$s) {
  if ([string]::IsNullOrEmpty($s)) { return $s }
  # U+FEFF
  if ($s.Length -gt 0 -and [int][char]$s[0] -eq 65279) {
    return $s.Substring(1)
  }
  return $s
}

function Write-Utf8NoBom([string]$Path, [string]$Text) {
  $enc = New-Object System.Text.UTF8Encoding($false)
  [System.IO.File]::WriteAllText($Path, $Text, $enc)
}

# Resolve repo root + config
$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..\..")).Path
if ([string]::IsNullOrWhiteSpace($ConfigPath)) {
  $ConfigPath = Join-Path $RepoRoot "infra/config/$Env.psd1"
} elseif (-not [System.IO.Path]::IsPathRooted($ConfigPath)) {
  $ConfigPath = (Resolve-Path $ConfigPath).Path
}

if (!(Test-Path $ConfigPath)) { throw "Config no encontrado: $ConfigPath" }
$config  = Import-PowerShellDataFile $ConfigPath

$project = $config.ProjectName
if ([string]::IsNullOrWhiteSpace($project)) { $project = "imaginarybank" }

$profile = $config.AwsProfile
$region  = $config.Region
if ([string]::IsNullOrWhiteSpace($region)) { throw "Region vacía en config: $ConfigPath" }

if ([string]::IsNullOrWhiteSpace($PolicyStoreStackName)) {
  $PolicyStoreStackName = "$project-$Env-70-authz-avp"
}

$schemaPath  = Join-Path $RepoRoot "infra/authz/cedar/schema.json"
$policiesDir = Join-Path $RepoRoot "infra/authz/cedar/policies"
if (!(Test-Path $schemaPath))  { throw "schema.json no encontrado: $schemaPath" }
if (!(Test-Path $policiesDir)) { throw "policies dir no encontrado: $policiesDir" }

function Invoke-AwsCli([string[]]$ArgsList) {
  $cmd = @(
    "aws.exe",
    "--no-cli-pager",
    "--no-cli-auto-prompt",
    "--cli-connect-timeout", "10",
    "--cli-read-timeout", "120",
    "--region", $region
  )
  if (-not [string]::IsNullOrWhiteSpace($profile)) { $cmd += @("--profile", $profile) }
  $cmd += $ArgsList

  Write-Host (">> " + ($cmd -join " "))

  $prev = $ErrorActionPreference
  $ErrorActionPreference = "Continue"
  try {
    $out = (& $cmd[0] $cmd[1..($cmd.Count-1)] 2>&1 | Out-String)
  } finally {
    $ErrorActionPreference = $prev
  }

  if ($LASTEXITCODE -ne 0) {
    throw "aws.exe failed (exit=$LASTEXITCODE):`n$out"
  }
  return $out
}

Stamp "ENV=$Env | Region=$region | Profile=$profile"
Stamp "ConfigPath=$ConfigPath"
Stamp "RepoRoot=$RepoRoot"
Stamp "PolicyStoreStackName=$PolicyStoreStackName"
Stamp "schemaPath=$schemaPath"

$cedarFiles = Get-ChildItem -Path $policiesDir -Filter "*.cedar"
Stamp ("policiesDir=$policiesDir | cedar files=" + $cedarFiles.Count)

# PolicyStoreId from CFN
Stamp "STEP A: Fetching PolicyStoreId from CFN outputs..."
$policyStoreId = (Invoke-AwsCli @(
  "cloudformation","describe-stacks",
  "--stack-name",$PolicyStoreStackName,
  "--query","Stacks[0].Outputs[?OutputKey=='PolicyStoreId'].OutputValue | [0]",
  "--output","text"
)).Trim()
if ([string]::IsNullOrWhiteSpace($policyStoreId) -or $policyStoreId -eq "None") {
  throw "No pude obtener PolicyStoreId desde el stack: $PolicyStoreStackName"
}
Stamp "PolicyStoreId=$policyStoreId"

# ---------- PUT SCHEMA ----------
Stamp "STEP 1: Reading schema.json..."
$schemaRaw = Get-Content -Raw -Encoding UTF8 -Path $schemaPath
$schemaRaw = Strip-Utf8Bom $schemaRaw

# Escape schema JSON -> string literal JSON (cedarJson requiere string)
Add-Type -AssemblyName System.Web.Extensions
$js = New-Object System.Web.Script.Serialization.JavaScriptSerializer
$schemaEscaped = $js.Serialize($schemaRaw)  # => "...." con escapes

# Definition file for --definition
$tmpSchemaDef = Join-Path $env:TEMP ("avp-schema-def-$Env.json")
$schemaDefJson = "{""cedarJson"":$schemaEscaped}"
Write-Utf8NoBom -Path $tmpSchemaDef -Text $schemaDefJson
$tmpSchemaDefUri = "file://" + ((Resolve-Path $tmpSchemaDef).Path -replace "\\","/")

Stamp "STEP 2: put-schema..."
Invoke-AwsCli @(
  "verifiedpermissions","put-schema",
  "--policy-store-id",$policyStoreId,
  "--definition",$tmpSchemaDefUri
) | Out-Null
Stamp "STEP 2 OK: put-schema done"
Start-Sleep -Seconds 2

# STRICT mode
Stamp "STEP 3: update-policy-store validation STRICT..."
Invoke-AwsCli @(
  "verifiedpermissions","update-policy-store",
  "--policy-store-id",$policyStoreId,
  "--validation-settings","mode=STRICT"
) | Out-Null
Stamp "STEP 3 OK: STRICT set"
Start-Sleep -Seconds 1

# Optionally delete all
if ($ReplaceAllPolicies) {
  Stamp "STEP 4: list-policies (for deletion)..."
  $lp = Invoke-AwsCli @(
    "verifiedpermissions","list-policies",
    "--policy-store-id",$policyStoreId,
    "--output","json"
  ) | ConvertFrom-Json

  $ids = @()
  if ($lp -and $lp.policies) {
    $ids = $lp.policies | ForEach-Object { $_.policyId } | Where-Object { $_ } | ForEach-Object { $_.Trim() }
  }

  if ($ids.Count -gt 0) {
    Stamp ("STEP 4 OK: deleting policies count=" + $ids.Count)
    foreach ($id in $ids) {
      if ($DebugSteps) { Stamp "Deleting policyId=$id" }
      Invoke-AwsCli @("verifiedpermissions","delete-policy","--policy-store-id",$policyStoreId,"--policy-id",$id) | Out-Null
    }
  } else {
    Stamp "STEP 4: No policies to delete."
  }


  Start-Sleep -Seconds 1
}

# ---------- CREATE POLICIES ----------
Stamp "STEP 5: Creating policies from *.cedar ..."
foreach ($f in $cedarFiles) {
  $name = $f.BaseName

  $stmt = Get-Content -Raw -Encoding UTF8 -Path $f.FullName
  $stmt = Strip-Utf8Bom $stmt

  if ($DebugSteps) {
    Stamp ("Policy file=" + $f.Name + " | chars=" + $stmt.Length)
    Stamp ("FirstCharCode=" + ([int][char]$stmt[0]))
  }

  # Build ONLY the definition object for --definition
  $defObj = @{
    static = @{
      description = "policy:$name"
      statement   = $stmt
    }
  }

  $defJson = $defObj | ConvertTo-Json -Depth 20 -Compress
  $tmpDef  = Join-Path $env:TEMP ("avp-policy-def-$Env-$name.json")
  Write-Utf8NoBom -Path $tmpDef -Text $defJson
  $tmpDefUri = "file://" + ((Resolve-Path $tmpDef).Path -replace "\\","/")

  Stamp "Creating policy: $name"
  Invoke-AwsCli @(
    "verifiedpermissions","create-policy",
    "--policy-store-id",$policyStoreId,
    "--definition",$tmpDefUri
  ) | Out-Null
}

Stamp "DONE ✅ schema + policies aplicadas. (Env=$Env)"
