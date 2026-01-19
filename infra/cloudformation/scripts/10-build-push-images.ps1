param(
  [Parameter(Mandatory=$true)][ValidateSet("dev","prod")] [string]$Env,
  [Parameter(Mandatory=$false)][string]$Tag = "",
  [Parameter(Mandatory=$false)][string[]]$Services = @("bff","identity","account","ledger"),
  [switch]$NoCache,
  [switch]$NoPush
)

. "$PSScriptRoot\helpers.ps1"

Assert-Command -Name "aws"
Assert-Command -Name "docker"

$cfg = Load-EnvConfig -Env $Env
$profile = $cfg.AwsProfile
$region  = $cfg.Region
$project = $cfg.ProjectName

# Tag por defecto = git short SHA (si existe git), sino timestamp
if ([string]::IsNullOrWhiteSpace($Tag)) {
  $git = Get-Command git -ErrorAction SilentlyContinue
  if ($git) {
    try {
      $Tag = (& git rev-parse --short HEAD).Trim()
    } catch {
      $Tag = (Get-Date -Format "yyyyMMddHHmmss")
    }
  } else {
    $Tag = (Get-Date -Format "yyyyMMddHHmmss")
  }
}

# AccountId
$acctJson = & aws --profile $profile --region $region sts get-caller-identity | Out-String
if ($LASTEXITCODE -ne 0) { throw "No se pudo obtener sts get-caller-identity" }
$acct = (ConvertFrom-Json $acctJson).Account

# ECR login
Write-Host "ECR login ($acct / $region) ..."
$pwd = & aws --profile $profile --region $region ecr get-login-password
if ($LASTEXITCODE -ne 0) { throw "Fallo ecr get-login-password" }
$registry = "$acct.dkr.ecr.$region.amazonaws.com"
$pwd | docker login --username AWS --password-stdin $registry
if ($LASTEXITCODE -ne 0) { throw "Fallo docker login ECR" }

# Repo names según estándar (coinciden con params de foundation)
$repoMap = @{
  "bff"      = "$project-$Env-bff"
  "identity" = "$project-$Env-identity"
  "account"  = "$project-$Env-account"
  "ledger"   = "$project-$Env-ledger"
}

# Build definitions (ajusta si tus Dockerfile difieren)
$builds = @{
  "bff"      = @{ Context="bff";            Dockerfile="bff/Dockerfile" }
  "identity" = @{ Context="micro/identity"; Dockerfile="micro/identity/Dockerfile" }
  "account"  = @{ Context="micro/account";  Dockerfile="micro/account/Dockerfile" }
  "ledger"   = @{ Context="micro/ledger";   Dockerfile="micro/ledger/Dockerfile" }
}

# Validar repos existen
foreach ($svc in $Services) {
  if (-not $repoMap.ContainsKey($svc)) { throw "Servicio no soportado: $svc" }
  $repoName = $repoMap[$svc]
  & aws --profile $profile --region $region ecr describe-repositories --repository-names $repoName *> $null
  if ($LASTEXITCODE -ne 0) {
    throw "No existe el repo ECR '$repoName'. Primero actualiza/despliega 00-foundation con CreateEcrRepositories=true."
  }
}

$root = Resolve-RepoRoot
$outDir = Join-Path $root ("infra\out\{0}" -f $Env)
New-Item -ItemType Directory -Path $outDir -Force | Out-Null

$result = @{}

foreach ($svc in $Services) {
  $repoName = $repoMap[$svc]
  $def = $builds[$svc]

  $ctxPath = Join-Path $root $def.Context
  $dfPath  = Join-Path $root $def.Dockerfile

  if (-not (Test-Path $ctxPath)) { throw "Build context no existe: $ctxPath" }
  if (-not (Test-Path $dfPath))  { throw "Dockerfile no existe: $dfPath" }

  $imageUri = "$registry/$repoName:$Tag"
  $localTag = "$svc:$Tag"

  Write-Host "==> Build $svc ($localTag) -> $imageUri"

  $args = @("build", "-t", $localTag, "-f", $dfPath, $ctxPath)
  if ($NoCache) { $args = @("build","--no-cache","-t",$localTag,"-f",$dfPath,$ctxPath) }

  & docker @args
  if ($LASTEXITCODE -ne 0) { throw "Fallo docker build para $svc" }

  & docker tag $localTag $imageUri
  if ($LASTEXITCODE -ne 0) { throw "Fallo docker tag para $svc" }

  if (-not $NoPush) {
    Write-Host "==> Push $imageUri"
    & docker push $imageUri
    if ($LASTEXITCODE -ne 0) { throw "Fallo docker push para $svc" }
  } else {
    Write-Host "NoPush: omitido push para $svc"
  }

  $result[$svc] = @{
    repo = $repoName
    tag  = $Tag
    uri  = $imageUri
  }
}

# Guardar resultado para reuso en compute-ecs
$outFile = Join-Path $outDir "images.json"
($result | ConvertTo-Json -Depth 5) | Out-File -FilePath $outFile -Encoding utf8
Write-Host "OK. images.json generado: $outFile"
Write-Host ("Tag usado: {0}" -f $Tag)
