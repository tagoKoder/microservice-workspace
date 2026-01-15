@{
  AppName      = "imabank"
  SystemName   = "banking"
  Env          = "dev"
  Region       = "us-east-1"
  RegionShort  = "use1"
  Repo         = "imaginarybank"

  Owner        = "thesis"
  CostCenter   = "thesis"
  Compliance   = "asvs-l3"
  DataClassification = "confidential"

  # CloudFormation
  StackId      = "00-foundation"
  StackName    = "imabank-dev-use1-00-foundation"
  NetworkStackName = "imabank-dev-use1-10-network"
  EksClusterName   = "imabank-dev-eks"
  IdentityStackName = "imabank-dev-use1-30-identity-cognito"
  CognitoDomainPrefix = "imabank-dev-use1-auth"  # debe ser único en la región
  PublicAppUrl = "http://localhost:4200"         # luego lo cambias a https://TU_FQDN
  AuditStackName = "imabank-dev-use1-60-audit"
  AuditEventBusName = "imabank-dev-use1-audit-bus"
  AuditFirehoseName = "imabank-dev-use1-audit-firehose"
  AuditTopicName = "imabank-dev-use1-audit-topic"
  AuditQueueName = "imabank-dev-use1-audit-queue"

}
