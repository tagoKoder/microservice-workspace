# 80-messaging

Objetivo:
- Agregar mensajería de negocio (tareas) sin mezclar con auditoría.
- Patrón: EventBridge (bus de dominio) + SQS (durable) + DLQ + KMS.

Flujo implementado:
- ledger (publisher) publica evento `ledger.journal.posted` a un bus por ambiente.
- Regla EventBridge enruta al target SQS `ledger-journal-posted-<env>`.
- account (consumer) hace long-poll ReceiveMessage y procesa.

Seguridad:
- SQS cifrado con CMK (KMS) + rotación.
- Key policy incluye permisos para EventBridge cuando el target SQS es cifrado.
- Queue policy permite SendMessage solo desde la regla.

Outputs:
- DomainEventBusName/Arn
- LedgerJournalPostedQueueUrl/Arn
- KMS key Arn
- Rule Arn
