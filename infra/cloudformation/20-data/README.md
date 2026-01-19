# 20-data

Incluye (por ambiente):
- RDS Postgres privado (subnets privadas)
- Security Group DB: permite 5432 solo desde sg-micros
- Secrets Manager:
  - bank/<env>/db/identity  (DB_DSN)
  - bank/<env>/db/account   (DB_DSN)
  - bank/<env>/db/ledger    (DB_DSN)
- Bucket S3 KYC privado (cifrado + deny no-TLS)

Notas:
- El RDS usa DeletionPolicy: Snapshot.
- Los secrets contienen DB_DSN listo para inyectarse como env var por ECS.
- El control fino del acceso al bucket KYC (prefijos staging/final) se alinea con los TaskRoles del stack 50 cuando ya estén definitivos.
