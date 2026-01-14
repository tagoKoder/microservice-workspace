$spec = "node_modules/@tagokoder/bff-openapi/openapi/bff.openapi.yaml"
$outDir = "internal/api/rest/gen/openapi"
New-Item -Force -ItemType Directory $outDir | Out-Null

oapi-codegen -generate "types,chi-server,strict-server,spec" `
  -o "$outDir/openapi.gen.go" `
  -package openapi `
  $spec
