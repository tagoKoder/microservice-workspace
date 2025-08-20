# build-with-sentry.ps1
param(
  [string]$Token = $env:SENTRY_AUTH_TOKEN,   # si ya está en el entorno, lo reutiliza
  [string]$Org   = $env:SENTRY_ORG,          # opcional
  [string]$Project = $env:SENTRY_PROJECT,    # opcional
  [switch]$SkipTests
)

# Si no viene el token, pedirlo
if (-not $Token) {
  $Token = Read-Host -Prompt "Pega tu SENTRY_AUTH_TOKEN"
}

# Variables de entorno SOLO para este proceso/hijos (Maven)
$env:SENTRY_AUTH_TOKEN = $Token
if ($Org)     { $env:SENTRY_ORG = $Org }
if ($Project) { $env:SENTRY_PROJECT = $Project }

# Ejecuta el wrapper de Maven
$cmd = ".\mvnw.cmd"
$args = @()
if ($SkipTests) { $args += "-DskipTests" }
$args += @("clean","package")

& $cmd $args
exit $LASTEXITCODE
