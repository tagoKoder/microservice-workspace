permit(
  principal,
  action == ImaginaryBank::Action::"payments:create",
  resource
)
when {
  principal.customer_id == resource.owner_customer_id
  && (
    resource.amount <= 500
    || context.mfa_verified == true
  )
};
