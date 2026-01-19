# 50-compute-ecs (mínimo: solo BFF)

Incluye por ambiente:
- ECS Cluster (Fargate)
- ALB público (80 -> redirect 443, y 443 con ACM cert)
- Target Group + Health Check
- ECS Service BFF en subnets privadas
- Log Group CloudWatch (/bank/<project>/<env>/bff)
- Route53 RecordSet: api.<dominio> -> ALB

Requisitos:
- Stack 10-network desplegado (exports: VpcId, PublicSubnetIds, PrivateSubnetIds, AlbSecurityGroupId, BffSecurityGroupId)
- Imagen BFF en ECR (Entregable 2)

Verificación:
- https://api.<dominio><HealthCheckPath> debe responder 200-399
