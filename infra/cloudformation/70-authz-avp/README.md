# 70-authz-avp

Crea un Policy Store por ambiente (dev/prod). Se crea en modo VALIDATION=OFF.
Luego ejecuta el script `infra/scripts/70-avp-apply-schema-policies.ps1` para:
1) put-schema desde `infra/authz_cedar/schema.json`
2) update-policy-store -> mode=STRICT
3) cargar policies desde `infra/authz_cedar/policies/*.cedar`

Notas:
- OFF→schema→STRICT es el flujo recomendado por AWS para evitar rechazos de policies cuando no hay schema. 
