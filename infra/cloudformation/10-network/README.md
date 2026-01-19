# 10-network

Crea una VPC aislada por ambiente:
- 2 subnets públicas (ALB/NAT)
- 2 subnets privadas (ECS tasks / RDS / Redis)
- NAT 1 o 2 (param)
- VPC endpoints recomendados: S3 (gateway), ECR API/DKR, Logs, Secrets Manager, STS
- Security Groups base (se cablean en 50-compute-ecs)

Outputs exportados:
- VpcId, CIDR, SubnetIds, RouteTables, SG ids.
