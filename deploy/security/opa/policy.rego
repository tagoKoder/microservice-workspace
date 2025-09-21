package envoy.authz
default allow = false

allow {
  # Datos de la request
  method := input.attributes.request.http.method
  path   := input.attributes.request.http.path
  host   := input.attributes.request.http.headers[":authority"]

  # Claims verificados por jwt_authn (payload disponible vía metadata)
  jwt := input.metadata_context["envoy.filters.http.jwt_authn"]["jwt_payload"]

  subject := {
    "sub": jwt.sub,
    "tenant": jwt.tenant_id,               # mapea este claim en Authentik
    "dept": jwt.department,
    "attrs": jwt
  }

  resource := {
    "host": host,
    "path": path,
    "tenant": input.attributes.request.http.headers["x-tenant-id"],
    "owner": input.attributes.request.http.headers["x-owner-id"],
    "type": resource_type(path)
  }

  action := action_of(path, method)

  ctx := {
    "ip": input.attributes.source.address.address,
    "hour": time.hour(time.now_ns())
  }

  same_tenant(subject, resource)
  authorize(subject, action, resource, ctx)
}

same_tenant(s, r) { s.tenant == r.tenant }

authorize(s, a, r, ctx) {
  a == "ou.read"
  startswith(r.path, "/api/ou/")
} else {
  a == "ou.write"
  r.owner == s.sub
  s.dept == "operations"
  ctx.hour >= 6
  ctx.hour < 22
}

resource_type(path) = "ou" { startswith(path, "/api/ou/") }
resource_type(path) = "unknown" { not startswith(path, "/api/ou/") }

action_of(path, method) = "ou.read" { startswith(path, "/api/ou/"); method == "GET" }
action_of(path, method) = "ou.write" { startswith(path, "/api/ou/"); method != "GET" }
