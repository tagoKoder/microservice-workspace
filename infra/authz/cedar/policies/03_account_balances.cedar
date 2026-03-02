permit(principal, action == ImaginaryBank::Action::"accounts:balances_read", resource)
when { principal.customer_id == resource.owner_customer_id };

permit(principal, action == ImaginaryBank::Action::"accounts:limits_patch", resource)
when { principal.customer_id == resource.owner_customer_id };