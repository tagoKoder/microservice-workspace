permit(
  principal,
  action == ImaginaryBank::Action::"accounts:read",
  resource
)
when {
  principal.customer_id == resource.owner_customer_id
};
