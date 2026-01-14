package envoy.authz

default allow := false

http := input.attributes.request.http
method := upper(http.method)
path := http.path

hdr(name) := v {
  vs := http.headers[name]
  count(vs) > 0
  v := vs[0]
} else := ""

roles := [r | r := trim(split(hdr("x-roles"), ",")[_])]
scopes := [s | s := trim(split(hdr("x-scopes"), " ")[_])]
sub := hdr("x-sub")
customer_id := hdr("x-customer-id")
purpose := hdr("x-purpose")

caller := hdr("x-caller-service")     # ej: bff, accounts, ops
target := hdr("x-target-service")     # bff, identity, accounts, ops, payments_ledger_grpc

has_role(r) { r != ""; roles[_] == r }
has_scope(s) { s != ""; scopes[_] == s }

is_authenticated {
  sub != ""
}

# =========================
# 1) BFF (entrypoint)
# =========================

bff_public_onboarding {
  target == "bff"
  startswith(path, "/bff/onboarding/")
  method == "POST"
}

bff_private {
  target == "bff"
  startswith(path, "/bff/")
  not bff_public_onboarding
}

# =========================
# 2) Identity (REST)
# =========================

identity_onboarding_calls {
  target == "identity"
  startswith(path, "/onboarding/")
  method == "POST"
  caller == "bff"
}

identity_whoami {
  target == "identity"
  path == "/identity/whoami"
  method == "GET"
  caller == "bff"
  is_authenticated
}

# =========================
# 3) Accounts (REST)
# =========================

accounts_read {
  target == "accounts"
  (startswith(path, "/accounts") or startswith(path, "/customers"))
  method == "GET"
  caller == "bff"
  is_authenticated
  has_role("ROLE_customer") or has_role("ROLE_support") or has_role("ROLE_admin")
}

accounts_write {
  target == "accounts"
  (startswith(path, "/accounts") or startswith(path, "/customers"))
  (method == "POST" or method == "PATCH")
  caller == "bff"
  is_authenticated
  has_role("ROLE_customer") or has_role("ROLE_support") or has_role("ROLE_admin")
}

# =========================
# 4) Ops (REST)
# =========================

ops_audit_write {
  target == "ops"
  startswith(path, "/audit/events")
  method == "POST"
  caller == "bff" or caller == "payments_ledger_grpc" or caller == "accounts" or caller == "identity"
}

ops_notifications_write {
  target == "ops"
  startswith(path, "/notifications/events")
  method == "POST"
  caller == "bff" or caller == "payments_ledger_grpc" or caller == "accounts" or caller == "identity"
}

# =========================
# 5) Payments/Ledger (gRPC)
# =========================

# Ejemplos de paths gRPC:
# /banking.payments.PaymentService/CreatePayment
# /banking.payments.PaymentService/GetPayment
# /banking.ledger.LedgerService/Topup
# /banking.ledger.LedgerService/GetActivity

grpc_target {
  target == "payments_ledger_grpc"
}

grpc_create_payment {
  grpc_target
  path == "/banking.payments.PaymentService/CreatePayment"
  caller == "bff"
  is_authenticated
  has_role("ROLE_customer")
  has_scope("payments.write")
  customer_id != ""
}

grpc_get_payment {
  grpc_target
  path == "/banking.payments.PaymentService/GetPayment"
  caller == "bff"
  is_authenticated
  has_role("ROLE_customer") or has_role("ROLE_support") or has_role("ROLE_admin")
}

grpc_topup_admin {
  grpc_target
  path == "/banking.ledger.LedgerService/Topup"
  caller == "bff"
  is_authenticated
  has_role("ROLE_admin")
  has_scope("ledger.topup")
}

grpc_ledger_activity {
  grpc_target
  path == "/banking.ledger.LedgerService/GetActivity"
  caller == "bff"
  is_authenticated
  has_role("ROLE_customer") or has_role("ROLE_support") or has_role("ROLE_admin")
}

# =========================
# 6) Coreografía (service-to-service)
# =========================
# En Docker es "demo". En Istio se reemplaza por identidad mTLS (source.principal).
internal_service_call {
  caller != ""         # viene desde un servicio
  target != ""         # se sabe el target
  # restringe que solo servicios internos se llamen entre sí
  caller == "bff" or caller == "accounts" or caller == "identity" or caller == "ops" or caller == "payments_ledger_grpc"
}

# =========================
# Allow rules
# =========================

# BFF: permitir onboarding público
allow { bff_public_onboarding }

# BFF privado: requiere autenticación (el BFF puede añadir x-sub al salir de Authentik)
allow {
  bff_private
  is_authenticated
}

# Identity onboarding (sin token final aún)
allow { identity_onboarding_calls }

# Identity whoami
allow { identity_whoami }

# Accounts
allow { accounts_read }
allow { accounts_write }

# Ops
allow { ops_audit_write }
allow { ops_notifications_write }

# gRPC payments/ledger
allow { grpc_create_payment }
allow { grpc_get_payment }
allow { grpc_topup_admin }
allow { grpc_ledger_activity }

# fallback interno controlado (si quieres endurecer, elimínalo y define rutas explícitas)
allow {
  internal_service_call
  # NO permitir que callers externos entren aquí:
  caller != ""
}
