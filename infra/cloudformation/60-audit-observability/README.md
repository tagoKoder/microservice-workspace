# 60-audit-observability

Incluye:
- EventBridge bus de auditoría por ambiente + archive.
- CloudTrail multi-region a S3 logs.
- (Opcional) WAF logs a CloudWatch Logs (log group debe iniciar con aws-waf-logs-).
- Alarmas base para ALB y ECS (requieren AlbArn, EcsClusterName y ServiceName).

Notas:
- Si tu región de compute no es us-east-1 y tu WAF es CLOUDFRONT, WAF logging típicamente se maneja en us-east-1.
- CloudTrail Data Events (S3) es opcional; puede generar volumen/costo.
