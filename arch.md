# 🏗️ Logical Architecture: Domains and Microservices

## 🎨 Frontend

- **Web App (Angular/React)**: consumes the BFF and displays entitlements.

---

## 🛡️ Edge Layer (PEP)

- **Envoy Gateway**:  
  - `jwt_authn`: validates OIDC/AuthN  
  - `ext_authz`: delegates authorization to OPA (ABAC/AuthZ)
  - Zero Trust principle: per-request verification
  - Reference: *NIST Technical Publications*

---

## 🔐 Identity

- **Authentik (OIDC IdP)**:  
  - Issues JWTs with claims: `sub`, `acr`, `tenant`, `amr`, `scope`  
  - Envoy validates via JWKS  
  - OPA uses claims for ABAC decisions  
  - ZTA requires identity + policy enforcement per access  
  - Reference: *NIST Security Center*

---

## 🧩 BFF / Entitlements (Optional)

- **BFF-Entitlements (Node/Go)**:  
  - Endpoint `/me/entitlements` queries OPA for UX (show/hide features)  
  - Real security enforcement remains in Envoy/OPA

---

## 🏦 Retail Banking Domains (Minimal Realistic Example)

### 👤 Customers (Spring Boot)
- Customer onboarding, basic KYC (simulated), minimal personal data (for LINDDUN)
- API:
  - `POST /customers`
  - `GET /customers/{id}`

### 💰 Accounts (Go)
- Checking/savings accounts, balances, simulated IBAN/CLABE
- API:
  - `GET /accounts?customerId=...`
  - `GET /accounts/{id}/balance`
  - `GET /accounts/{id}/transactions`

### 💸 Payments (Go)
- Internal/external transfers (Open Banking PIS-like), payment orders
- API:
  - `POST /payments/transfer` (synchronous)
  - `POST /payments/bill` (asynchronous via RabbitMQ)
- Events:
  - `PaymentRequested`
  - `PaymentSettled`
  - `PaymentFailed`

### 💳 Cards (Spring Boot)
- Masked card numbers, PAN tokenization (avoids full PCI scope)
- API:
  - `POST /cards/tokenize`
  - `POST /cards/authorize` (simulated)

### 📚 Ledger (Go)
- Idempotent accounting entries; source of truth for balances (simple double-entry)
- API (gRPC):
  - `LedgerService/BookEntry`
  - `GetBalance`

### 📣 Notifications (Go)
- Simulated Email/SMS (Mailhog)
- Event:
  - `PaymentSettled → SendEmail`

### 🕵️ Audit (Spring Boot)
- Centralized logging of OPA decisions and business audit events
- API:
  - `POST /audit/decision`
  - `POST /audit/event`

---

## 🔄 Business Flows to Demonstrate Security

### A) Internal Transfer (Synchronous)

1. Frontend calls `POST /payments/transfer`
2. Envoy validates JWT → sends S-A-R-C to OPA:
   - `subject.tenant`
   - `action=payment:transfer`
   - `resource.accountId=...`
   - `context.amount=...`
3. OPA ABAC evaluates (deny-by-default)
4. Payments validates business logic → calls Ledger (gRPC)
5. Envoy (east-west) repeats `ext_authz → OPA`
6. Accounts updates projections and returns balance
7. Notifications triggers email (optional)

#### Suggested ABAC Attributes:
- **Subject**: `tenant`, `customerId`, `assurance_level (acr)`
- **Resource**: `accountId.tenant`, `account.ownerId`
- **Context**: `amount`, `channel`, `business_hours`, `riskScore`

#### Example Rule:
Allow if:
- Same `tenant`
- `customerId` is owner or authorized
- `amount < 10k` or `assurance_level >= 2`
- Outside business hours → require high MFA  
Reference: *NIST SP 800-204B*

---

### B) Bill Payment (Asynchronous with RabbitMQ)

1. Frontend calls `POST /payments/bill`
2. Envoy/OPA → Payments publishes `PaymentRequested`
3. Payments orchestrates:
   - Accounts reserves funds
   - Ledger books entries
   - Notifications sends receipt
4. Payments emits `PaymentSettled`
5. BFF reports status via `GET /jobs/{id}`

✅ Demonstrates ABAC in both HTTP/gRPC and messaging  
Reference: *NIST Technical Publications*

---

## 📡 Minimal APIs (For Testing and ASVS)

### Accounts
- `GET /accounts?customerId=`
- `GET /accounts/{id}/balance`
- `GET /accounts/{id}/transactions?from=&to=`

### Payments
- `POST /payments/transfer`  
  Body: `{fromAccount, toAccount, amount, currency}`
- `POST /payments/bill`  
  Body: `{fromAccount, billerId, amount}`

### Ledger (gRPC)
- `BookEntry({accountId, amount, type})`
- `GetBalance({accountId})`

### BFF
- `GET /me/entitlements`
- `GET /jobs/{id}`

### Audit
- `POST /audit/decision`
- `POST /audit/event`

---

## 🔐 Where Security Controls Apply (Zero Trust)

- **AuthN in Envoy**: JWT filter against Authentik (issuer/JWKS)
- **AuthZ in OPA**: ABAC (S-A-R-C). Proxy enforces allow/deny; business logic contains no access rules  
Reference: *NIST Technical Publications*

### Optional: mTLS Between Services
- For service mesh migration, NIST 800-204A/B recommends mTLS + proxy-based authorization  
Reference: *NIST Security Center*

---

## ✅ Verification and Compliance

- **ASVS v5**: Use V2, V4, V9, V13 as checklist
- **PCI DSS 4.0**: Tokenize and avoid real PAN; if simulated, cite standard and define API scope

---

## 📊 Data and Attributes for ABAC

- **OIDC Claims (JWT)**: `sub`, `tenant`, `acr`, `amr`, `scope (ais:read, pis:write)`
- **Resource Attributes**: `account.ownerId`, `account.tenant`, `account.status`
- **Context**: `channel`, `ip_range`, `business_hours`, `amount`
- **Rego Policies**: deny-by-default; rules by tenant + ownership + acr + amount thresholds + time

---

## 🧪 Threat Scenarios → Controls → Evidence

### STRIDE on Transfer Flow

| Threat            | Control Applied                        | Evidence                         |
|-------------------|----------------------------------------|----------------------------------|
| Spoofing          | Invalid token → Envoy 401 (ASVS V2)    | Envoy logs                       |
| Tampering         | Modified body → JWT signature + logic  | 400 + business logs              |
| Repudiation       | Audit trail with `sub`, `action`, etc. | Audit service logs               |
| Info Disclosure   | ABAC + `scope=ais:read`                | OPA denials                      |
| DoS               | Rate-limit in Envoy                    | 429 metrics                      |
| Elevation of Priv | ABAC: low acr + high amount → deny     | OPA decision logs                |

### LINDDUN on Customers/Accounts

- Linkability/Identifiability → data minimization (`customerId`)
- Consent/scope → evidence: scopes + policies

---

## 📈 Evaluation Metrics (For Thesis or Paper)

- **Performance**: latency p50/p95/p99 and throughput baseline vs ABAC active
- **Security (ASVS)**: % of controls met; true denies in negative tests
- **Traceability**: 100% of routes pass through OPA (request ↔ decision log)
- **Sector Compliance**: If simulating cards, show tokenization and PCI DSS v4.0 references

---
