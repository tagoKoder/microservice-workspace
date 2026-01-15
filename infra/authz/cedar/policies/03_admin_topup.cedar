permit(
  principal,
  action == Action::"admin:sandbox:topup",
  resource
)
when {
  principal.roles.contains("admin")
};
