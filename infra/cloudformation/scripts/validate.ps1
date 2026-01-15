param(
  [Parameter(Mandatory=$true)][string]$Env
)

$cfg = Import-PowerShellDataFile "infra/config/$Env.psd1"
$template = "infra/cloudformation/00-foundation/template.yaml"

Write-Host "Validating template: $template"

# cfn-lint (opcional)
$lint = Get-Command cfn-lint -ErrorAction SilentlyContinue
if ($lint) {
  cfn-lint $template
} else {
  Write-Host "cfn-lint not found (optional). Skipping."
}

# AWS validate-template (sí o sí)
aws cloudformation validate-template `
  --region $cfg.Region `
  --template-body ("file://{0}" -f (Resolve-Path $template))
