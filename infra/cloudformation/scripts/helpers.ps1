Set-StrictMode -Version Latest

function Resolve-RepoRoot {
  $here = Split-Path -Parent $PSScriptRoot
  return (Resolve-Path $here).Path
}

function Load-EnvConfig {
  param(
    [Parameter(Mandatory=$true)][ValidateSet("dev","prod")] [string]$Env
  )
  $root = Resolve-RepoRoot
  $cfgPath = Join-Path $root ("infra\config\{0}.psd1" -f $Env)
  if (-not (Test-Path $cfgPath)) {
    throw "No existe config: $cfgPath"
  }
  return Import-PowerShellDataFile -Path $cfgPath
}

function Assert-Command {
  param([Parameter(Mandatory=$true)][string]$Name)
  $cmd = Get-Command $Name -ErrorAction SilentlyContinue
  if (-not $cmd) { throw "No se encontró el comando requerido: $Name" }
}

function Read-ParamsJsonAsOverrides {
  param([Parameter(Mandatory=$true)][string]$ParamsPath)

  if (-not (Test-Path $ParamsPath)) {
    throw "Params JSON no existe: $ParamsPath"
  }

  $obj = Get-Content $ParamsPath -Raw | ConvertFrom-Json

  $pairs = @()
  foreach ($p in $obj.PSObject.Properties) {
    $name = $p.Name
    $val = $p.Value

    if ($null -eq $val) { continue }

    # Convertir arrays a string separado por coma (si algún día lo necesitas)
    if ($val -is [System.Array]) {
      $val = ($val -join ",")
    }

    $pairs += ("{0}={1}" -f $name, $val)
  }

  return $pairs
}

function Invoke-Aws {
  param(
    [Parameter(Mandatory=$true)][string]$Profile,
    [Parameter(Mandatory=$true)][string]$Region,
    [Parameter(Mandatory=$true)][string[]]$Args
  )
  $base = @("--profile", $Profile, "--region", $Region)
  & aws @base @Args
  if ($LASTEXITCODE -ne 0) {
    throw "AWS CLI falló (exit=$LASTEXITCODE) -> aws $($Args -join ' ')"
  }
}

function Deploy-CfnStack {
  param(
    [Parameter(Mandatory=$true)][string]$Profile,
    [Parameter(Mandatory=$true)][string]$Region,
    [Parameter(Mandatory=$true)][string]$StackName,
    [Parameter(Mandatory=$true)][string]$TemplatePath,
    [Parameter(Mandatory=$true)][string]$ParamsPath,
    [Parameter(Mandatory=$true)][string]$ProjectName,
    [Parameter(Mandatory=$true)][ValidateSet("dev","prod")] [string]$Env
  )

  if (-not (Test-Path $TemplatePath)) { throw "Template no existe: $TemplatePath" }

  $overrides = Read-ParamsJsonAsOverrides -ParamsPath $ParamsPath

  $tags = @(
    ("project={0}" -f $ProjectName),
    ("env={0}" -f $Env)
  )

  $args = @(
    "cloudformation", "deploy",
    "--stack-name", $StackName,
    "--template-file", $TemplatePath,
    "--capabilities", "CAPABILITY_NAMED_IAM",
    "--no-fail-on-empty-changeset"
  )

  # Parameter overrides
  $args += "--parameter-overrides"
  $args += $overrides

  # Stack tags (complementario)
  $args += "--tags"
  $args += $tags

  Write-Host "Desplegando stack: $StackName"
  Invoke-Aws -Profile $Profile -Region $Region -Args $args
}
