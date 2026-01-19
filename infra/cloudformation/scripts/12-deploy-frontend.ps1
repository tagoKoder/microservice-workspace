param(
  [Parameter(Mandatory=$true)][ValidateSet("dev","prod")] [string]$Env,
  [Parameter(Mandatory=$true)][string]$DistPath,
  [Parameter(Mandatory=$false)][string]$StackName = "",
  [switch]$SkipInvalidation
)

. "$PSScriptRoot\helpers.ps1"
Assert-Command -Name "aws"

$cfg = Load-EnvConfig -Env $Env
$profile = $cfg.AwsProfile
$region  = "us-east-1" # CloudFront/WAF stack recomendado en us-east-1

if ([string]::IsNullOrWhiteSpace($StackName)) {
  $StackName = "imaginarybank-$Env-40-edge"
}

# Obtener outputs
$stackJson = & aws --profile $profile --region $region cloudformation describe-stacks --stack-name $StackName | Out-String
if ($LASTEXITCODE -ne 0) { throw "No se pudo leer stack $StackName en $region" }
$st = (ConvertFrom-Json $stackJson).Stacks[0]
$out = @{}
foreach ($o in $st.Outputs) { $out[$o.OutputKey] = $o.OutputValue }

$bucket = $out["WebBucketName"]
$distId = $out["CloudFrontDistributionId"]

if (-not (Test-Path $DistPath)) { throw "DistPath no existe: $DistPath" }

Write-Host "Sync -> s3://$bucket"
& aws --profile $profile --region $region s3 sync $DistPath "s3://$bucket/" --delete
if ($LASTEXITCODE -ne 0) { throw "Fallo s3 sync" }

if (-not $SkipInvalidation) {
  Write-Host "Invalidate -> $distId"
  & aws --profile $profile --region $region cloudfront create-invalidation --distribution-id $distId --paths "/*" *> $null
  if ($LASTEXITCODE -ne 0) { throw "Fallo invalidation" }
}

Write-Host "OK: https://$($out["AppFqdn"])"
