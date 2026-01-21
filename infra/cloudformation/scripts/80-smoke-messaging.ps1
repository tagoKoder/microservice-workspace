param(
  [Parameter(Mandatory=$true)]
  [ValidateSet("dev","prod")]
  [string]$Env,

  [Parameter(Mandatory=$true)]
  [string]$ProjectName,

  [Parameter(Mandatory=$true)]
  [string]$AwsProfile,

  [Parameter(Mandatory=$true)]
  [string]$Region
)

$ErrorActionPreference = "Stop"

$stackName = "$ProjectName-$Env-80-messaging"

Write-Host "Reading outputs from stack: $stackName"

$busName = aws cloudformation describe-stacks `
  --profile $AwsProfile --region $Region `
  --stack-name $stackName `
  --query "Stacks[0].Outputs[?OutputKey=='DomainEventBusName'].OutputValue" `
  --output text

$queueUrl = aws cloudformation describe-stacks `
  --profile $AwsProfile --region $Region `
  --stack-name $stackName `
  --query "Stacks[0].Outputs[?OutputKey=='LedgerJournalPostedQueueUrl'].OutputValue" `
  --output text

if (-not $busName -or -not $queueUrl) {
  throw "Missing outputs. busName='$busName' queueUrl='$queueUrl'"
}

Write-Host "BusName: $busName"
Write-Host "QueueUrl: $queueUrl"

$eventDetail = @{
  journal_id      = "test-journal-001"
  correlation_id  = "corr-test-001"
  occurred_at_utc = (Get-Date).ToUniversalTime().ToString("o")
} | ConvertTo-Json -Compress

$entries = @(
  @{
    Source       = "bank.ledger"
    DetailType   = "ledger.journal.posted"
    Detail       = $eventDetail
    EventBusName = $busName
  }
) | ConvertTo-Json -Compress

$tmp = New-TemporaryFile
$payloadPath = $tmp.FullName
Set-Content -Path $payloadPath -Value $entries -Encoding UTF8

Write-Host "Publishing test event to EventBridge..."
aws events put-events `
  --profile $AwsProfile --region $Region `
  --entries "file://$payloadPath" | Out-Host

Write-Host "Waiting a moment..."
Start-Sleep -Seconds 2

Write-Host "Receiving from SQS (long poll)..."
aws sqs receive-message `
  --profile $AwsProfile --region $Region `
  --queue-url $queueUrl `
  --max-number-of-messages 1 `
  --wait-time-seconds 10 `
  --attribute-names All `
  --message-attribute-names All | Out-Host

Write-Host "Done. If you received a message, routing is working."
