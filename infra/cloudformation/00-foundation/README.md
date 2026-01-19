# 00-foundation

Crea la base por ambiente:
- KMS keys: kms-s3, kms-secrets, (opcional) kms-logs
- S3 bucket base de logs (privado, cifrado KMS, con retención)
- S3 bucket de artifacts (opcional)
- IAM role de despliegue (deployer) para estandarizar despliegues posteriores

Notas:
- Buckets y keys quedan con Retain para evitar borrado accidental.
- El role deployer usa AdministratorAccess por simplicidad de tesis; si quieres, lo reducimos luego.
