# 40-edge (S3 privado + CloudFront OAC + WAF + Route53)

Crea por ambiente:
- S3 privado para Angular (sin acceso público)
- CloudFront Distribution con OAC (S3 solo accesible vía CloudFront)
- WAFv2 WebACL (Scope CLOUDFRONT) con managed rules + rate limit
- ResponseHeadersPolicy (HSTS, XFO, XCTO, CSP mínimo)
- Route53 record: app.<dominio> -> CloudFront

Requisitos:
- Certificado ACM para app.<dominio> en us-east-1
- Este stack debe desplegarse en us-east-1 si incluye WAF Scope CLOUDFRONT

Despliegue de frontend:
1) Build Angular
2) aws s3 sync dist/... s3://<WebBucketName> --delete
3) Invalidate CloudFront: aws cloudfront create-invalidation --distribution-id <id> --paths "/*"
