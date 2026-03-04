permit (
    principal,
    action == ImaginaryBank::Action::"payments:create",
    resource
)
when { principal.customer_id == resource.source_account_owner_customer_id };