permit (
    principal,
    action == ImaginaryBank::Action::"accounts:read",
    resource
)
when { context.customer_id == resource.owner_customer_id };