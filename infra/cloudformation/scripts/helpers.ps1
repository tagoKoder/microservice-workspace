Set-StrictMode -Version Latest
$PSNativeCommandUseErrorActionPreference = $false

function Resolve-RepoRoot {
  # Partimos desde ...\infra\cloudformation\scripts
  $cur = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path  # ...\infra\cloudformation

  while ($true) {
    if ( (Test-Path (Join-Path $cur "infra\config")) -or (Test-Path (Join-Path $cur ".git")) ) {
      return $cur
    }

    $parent = Split-Path -Parent $cur
    if ($parent -eq $cur -or [string]::IsNullOrWhiteSpace($parent)) {
      throw "No pude detectar RepoRoot subiendo desde: $PSScriptRoot. No encontré infra\config ni .git."
    }

    $cur = $parent
  }
}



function Load-EnvConfig {
  param([string]$RepoRoot, [string]$Env)
  $cfgPath = Join-Path $RepoRoot "infra/config/$Env.psd1"
  if (!(Test-Path $cfgPath)) { throw "Missing config: $cfgPath" }
  return Import-PowerShellDataFile $cfgPath
}

function Assert-Command {
  param([Parameter(Mandatory=$true)][string]$Name)
  $cmd = Get-Command $Name -ErrorAction SilentlyContinue
  if (-not $cmd) { throw "No se encontró el comando requerido: $Name" }
}

function Get-AwsProfileRegion {
  param([hashtable]$Cfg)

  $profile = $null
  $region  = $null

  foreach ($k in @("AwsProfile","AWSProfile","Profile")) {
    if ($Cfg.ContainsKey($k) -and $Cfg[$k]) { $profile = $Cfg[$k]; break }
  }
  foreach ($k in @("AwsRegion","AWSRegion","Region")) {
    if ($Cfg.ContainsKey($k) -and $Cfg[$k]) { $region = $Cfg[$k]; break }
  }

  # Fallbacks: si no hay en config, usa entorno/AWS config default
  if (-not $profile) { $profile = "default" }
  if (-not $region)  { $region  = $env:AWS_REGION }
  if (-not $region)  { $region  = $env:AWS_DEFAULT_REGION }
  if (-not $region)  { $region  = "us-east-1" } # último fallback

  return @{ Profile = $profile; Region = $region }
}

function Invoke-Aws {
  param(
    [Parameter(Mandatory=$true)][string]$Profile,
    [Parameter(Mandatory=$true)][string]$Region,
    [Parameter(Mandatory=$true)][string[]]$Args,
    [switch]$AllowFailure
  )

  $base = @("--profile", $Profile, "--region", $Region)

  # Captura stdout+stderr
  $out = & aws @base @Args 2>&1
  $exit = $LASTEXITCODE

  if (-not $AllowFailure -and $exit -ne 0) {
    throw "AWS CLI falló (exit=$exit) -> aws $($Args -join ' ')`n--- output ---`n$out"
  }

  return @{ Exit = $exit; Output = $out }
}


function Read-ParamsJsonAsOverrides {
  param([Parameter(Mandatory=$true)][string]$ParamsPath)

  if (-not (Test-Path $ParamsPath)) {
    throw "Params JSON no existe: $ParamsPath"
  }

  $raw = Get-Content $ParamsPath -Raw | ConvertFrom-Json
  $pairs = @()

  # Soporta 2 formatos:
  # A) {"ParamA":"x","ParamB":123}
  # B) [{"ParameterKey":"ParamA","ParameterValue":"x"}, ...]
  if ($raw -is [System.Collections.IEnumerable] -and $raw.Count -gt 0 -and $raw[0].PSObject.Properties.Name -contains "ParameterKey") {
    foreach ($p in $raw) {
      if ($null -eq $p.ParameterValue) { continue }
      $pairs += ("{0}={1}" -f $p.ParameterKey, $p.ParameterValue)
    }
    return $pairs
  }

  foreach ($p in $raw.PSObject.Properties) {
    $name = $p.Name
    $val  = $p.Value
    if ($null -eq $val) { continue }

    if ($val -is [System.Array]) {
      $val = ($val -join ",")
    }
    $pairs += ("{0}={1}" -f $name, $val)
  }

  return $pairs
}

function Get-StackStatus {
  param(
    [string]$Profile,
    [string]$Region,
    [string]$StackName
  )

  $json = aws --profile $Profile --region $Region cloudformation describe-stacks --stack-name $StackName 2>$null
  if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($json)) { return $null }

  $obj = $json | ConvertFrom-Json
  return $obj.Stacks[0].StackStatus
}

function Print-StackFailureSummary {
  param(
    [string]$Profile,
    [string]$Region,
    [string]$StackName,
    [int]$Max = 30
  )

  $status = Get-StackStatus -Profile $Profile -Region $Region -StackName $StackName
  if ($status) {
    Write-Host "StackStatus: $status" -ForegroundColor Yellow
  } else {
    Write-Host "StackStatus: (no disponible / stack no existe aún)" -ForegroundColor Yellow
  }

  $eventsJson = aws --profile $Profile --region $Region cloudformation describe-stack-events --stack-name $StackName --max-items 100 2>$null
  if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($eventsJson)) {
    Write-Host "No pude leer stack-events para $StackName (puede no existir o no hay permisos)." -ForegroundColor Yellow
    return
  }

  $events = ($eventsJson | ConvertFrom-Json).StackEvents

  $interesting = $events |
    Where-Object {
      $_.ResourceStatus -match 'FAILED|ROLLBACK|DELETE_FAILED' -or
      ($_.ResourceStatusReason -and $_.ResourceStatusReason.Trim().Length -gt 0)
    } |
    Select-Object -First $Max Timestamp, LogicalResourceId, ResourceType, ResourceStatus, ResourceStatusReason

  Write-Host "=== Eventos relevantes (top $Max) ===" -ForegroundColor Cyan
  $interesting | Format-Table -AutoSize | Out-String | Write-Host

  # “Root cause” probable: primer evento FAILED con reason
  $root = $events |
    Where-Object { $_.ResourceStatus -match 'FAILED' -and $_.ResourceStatusReason } |
    Select-Object -First 1 Timestamp, LogicalResourceId, ResourceType, ResourceStatusReason

  if ($root) {
    Write-Host "=== Causa probable (primer FAILED) ===" -ForegroundColor Magenta
    $root | Format-List | Out-String | Write-Host
  }
}

function Remove-FailedCreateStackIfNeeded {
  param(
    [string]$Profile,
    [string]$Region,
    [string]$StackName
  )

  $status = Get-StackStatus -Profile $Profile -Region $Region -StackName $StackName
  if (-not $status) { return }

  # Solo limpieza segura de CREATE fallido
  if ($status -in @("ROLLBACK_COMPLETE","CREATE_FAILED")) {
    Write-Host "🧹 Eliminando stack fallido ($status): $StackName" -ForegroundColor Yellow
    Invoke-Aws -Profile $Profile -Region $Region -Args @("cloudformation","delete-stack","--stack-name",$StackName)
    Invoke-Aws -Profile $Profile -Region $Region -Args @("cloudformation","wait","stack-delete-complete","--stack-name",$StackName)
    Write-Host "🧹 Stack eliminado: $StackName" -ForegroundColor Green
  }
}

function Preflight {
  param([hashtable]$Cfg)

  Assert-Command -Name "aws"

  $ar = Get-AwsProfileRegion -Cfg $Cfg
  $profile = $ar.Profile
  $region  = $ar.Region

  Write-Host "AWS Profile: $profile | Region: $region" -ForegroundColor Gray

  $res = Invoke-Aws -Profile $profile -Region $region -Args @("sts","get-caller-identity") -AllowFailure
  if ($res.Exit -ne 0) {
    throw "No pude ejecutar sts get-caller-identity (exit=$($res.Exit)). Output:`n$($res.Output)"
  }

  $whoObj = ($res.Output | Out-String | ConvertFrom-Json)
  Write-Host "AWS Account: $($whoObj.Account) | Arn: $($whoObj.Arn)" -ForegroundColor Gray
}

function Deploy-Stack {
  param(
    [Parameter(Mandatory=$true)][string]$RepoRoot,
    [Parameter(Mandatory=$true)][string]$Env,
    [Parameter(Mandatory=$true)][string]$StackDir,
    [Parameter(Mandatory=$true)][string]$ParamsFile,
    [switch]$PreserveFailedStack,      # --disable-rollback (solo debug)
    [switch]$AutoDeleteFailedCreate    # auto delete si ROLLBACK_COMPLETE/CREATE_FAILED
  )

  $cfg = Load-EnvConfig -RepoRoot $RepoRoot -Env $Env
  $ar  = Get-AwsProfileRegion -Cfg $cfg
  $profile = $ar.Profile
  $region  = $ar.Region

  $stackName = "$($cfg.ProjectName)-$Env-$StackDir"
  $template  = Join-Path $RepoRoot "infra/cloudformation/$StackDir/template.yml"
  $params    = Join-Path $RepoRoot $ParamsFile

  if (!(Test-Path $template)) { throw "Missing template: $template" }
  if (!(Test-Path $params))   { throw "Missing params: $params" }

  $overrides = Read-ParamsJsonAsOverrides -ParamsPath $params

  Write-Host "`n--- Deploy stack: $stackName ---" -ForegroundColor Green

  $args = @(
    "cloudformation","deploy",
    "--stack-name",$stackName,
    "--template-file",$template,
    "--capabilities","CAPABILITY_NAMED_IAM","CAPABILITY_AUTO_EXPAND",
    "--parameter-overrides"
  ) + $overrides + @("--no-fail-on-empty-changeset")

  if ($PreserveFailedStack) {
    $args += @("--disable-rollback")
  }

  # Ejecuta y captura exit code
  $res = Invoke-Aws -Profile $profile -Region $region -Args $args -AllowFailure
  if ($res.Exit -ne 0) {
    Write-Host "AWS output:`n$($res.Output)" -ForegroundColor Yellow
    Print-StackFailureSummary -Profile $profile -Region $region -StackName $stackName -Max 25

    if ($AutoDeleteFailedCreate) {
      Remove-FailedCreateStackIfNeeded -Profile $profile -Region $region -StackName $stackName
    }

    throw "Deploy failed: $stackName"
  }

  Write-Host "✅ OK: $stackName" -ForegroundColor Green
}
