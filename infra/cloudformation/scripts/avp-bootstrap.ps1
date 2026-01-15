param(
  [Parameter(Mandatory=$true)][string]$PolicyStoreName,
  [Parameter(Mandatory=$true)][string]$SchemaFile,
  [Parameter(Mandatory=$true)][string]$PoliciesDir
)

$ErrorActionPreference = "Stop"

Write-Host "Searching policy store: $PolicyStoreName"
$stores = aws verifiedpermissions list-policy-stores --query "policyStores[?policyStoreName=='$PolicyStoreName']" --output json | ConvertFrom-Json

if ($stores.Count -eq 0) {
  Write-Host "Creating policy store..."
  $created = aws verifiedpermissions create-policy-store --policy-store-name $PolicyStoreName --query "{id:policyStoreId,name:policyStoreName}" --output json | ConvertFrom-Json
  $policyStoreId = $created.id
} else {
  $policyStoreId = $stores[0].policyStoreId
}

Write-Host "PolicyStoreId: $policyStoreId"

Write-Host "Putting schema from $SchemaFile"
aws verifiedpermissions put-schema --policy-store-id $policyStoreId --definition file://$SchemaFile | Out-Null

Write-Host "Loading policies from $PoliciesDir"
Get-ChildItem -Path $PoliciesDir -Filter *.cedar | Sort-Object Name | ForEach-Object {
  $policyText = Get-Content $_.FullName -Raw

  Write-Host "Creating policy: $($_.Name)"
  aws verifiedpermissions create-policy `
    --policy-store-id $policyStoreId `
    --definition "{`"static`":{`"statement`":$([System.Text.Json.JsonSerializer]::Serialize($policyText))}}" | Out-Null
}

Write-Host "Done. PolicyStoreId=$policyStoreId"
