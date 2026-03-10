# Imaginary Bank — Secure Microservices

## Overview

**Imaginary Bank** is a project focused on the design and implementation of a banking platform based on microservices, with a strong emphasis on **security, traceability, fine-grained authorization, and cloud-native engineering practices**.

The goal is not just to build functional APIs, but to demonstrate how to design a distributed system under **Zero Trust**, **defense in depth**, **structured auditing**, **ABAC/ReBAC authorization**, and controls aligned with standards such as **OWASP ASVS Level 3**.

This repository also serves as a **technical portfolio**, so it documents not only the codebase, but also the architecture, security decisions, and the way the components integrate with each other.

---

## Project goal

Design a secure architecture for a modern banking application that enables:

* centralized and secure authentication;
* decoupled and scalable authorization;
* clear separation between frontend, BFF, and microservices;
* end-to-end traceability with structured auditing;
* reusable security controls across services;
* cloud-native deployment on AWS through infrastructure as code.

---

## Security approach

This project was designed from the beginning with a **security-first** mindset. Key controls and decisions include:

* **BFF-first pattern**: the browser does not interact directly with internal microservices.
* **Authentication with Cognito/JWT** and validation through **JWKS**.
* **Centralized authorization with AWS Verified Permissions** using **Cedar** policies.
* **Deny-by-default** model to minimize accidental exposure.
* **Resource and ownership resolution** before authorization decisions.
* **Structured auditing** with a dedicated event standard (`AuditEvent JSON v1.0`).
* **Idempotency** for sensitive operations and prevention of dangerous retries.
* **Correlation ID** for cross-service traceability.
* **Secure error mapping**, avoiding internal detail leakage to clients.
* **Separation of concerns** across web layer, BFF, business logic, and authorization.

---

## High-level architecture

```mermaid
flowchart LR
    U[User / Angular Frontend] --> BFF[Go BFF]
    BFF --> ID[Identity Service]
    BFF --> ACC[Account Service]
    BFF --> LED[Ledger Service]

    BFF --> COG[Cognito]
    ID --> AVP[AWS Verified Permissions]
    ACC --> AVP
    LED --> AVP

    ID --> AUD[Audit / EventBridge / Logs]
    ACC --> AUD
    LED --> AUD

    INFRA[CloudFormation / AWS Infra] --> BFF
    INFRA --> ID
    INFRA --> ACC
    INFRA --> LED
```

### Main components

* **imaginarybank-web**: application frontend.
* **bff**: Backend for Frontend that centralizes validation, session handling, secure exposure policies, and orchestration.
* **micro/identity**: microservice focused on identity, onboarding, and authentication-related operations.
* **micro/account**: microservice for account domain logic.
* **micro/ledger**: microservice focused on transactions, payments, and credits.
* **infra**: infrastructure as code templates for networking, data, identity, compute, auditing, and observability.

---

## Repository structure

```text
microservice-workspace/
├── .aws/
├── .vscode/
├── bff/
├── imaginarybank-web/
├── infra/
├── micro/
│   ├── account/
│   ├── identity/
│   └── ledger/
├── docker-compose.yml
├── arch.md
└── README.md
```

### Folder breakdown

#### `bff/`

Contains the Backend for Frontend built to expose a controlled surface to the client. Responsibilities here include:

* session validation;
* secure identity propagation to microservices;
* security middleware;
* cookie/session control;
* idempotency and correlation IDs;
* translation and hardening of errors for the public layer.

#### `imaginarybank-web/`

Frontend application that consumes the BFF. This layer is intended to remain decoupled from the internal complexity of authorization and service-to-service communication.

#### `micro/identity/`

Service responsible for identity and onboarding flows, including integration with the authentication and authorization scheme defined for the platform.

#### `micro/account/`

Service that encapsulates account domain logic, resource ownership, authorization context resolution, and operational business rules.

#### `micro/ledger/`

Service for transactional operations, movements, payments, and credits, with strong emphasis on security, idempotency, and contextual authorization validation.

#### `infra/`

Defines the project infrastructure in a modular and reproducible way. This is where the base for networking, identity, compute, data, and observability in AWS is structured.

---

## Design principles applied

### 1. Security decoupled from business logic

Authorization is not hardcoded inside every use case. Instead, the platform builds a reusable layer to resolve:

* who the principal is;
* which action is being attempted;
* on which resource;
* under which context;
* which policy must be evaluated.

### 2. BFF as a control point

The BFF acts as the security boundary between the client and the microservices. This makes it possible to:

* reduce direct exposure;
* centralize session handling;
* apply uniform input/output rules;
* protect internal architectural details.

### 3. Observability and auditing as part of the design

This is not just about “adding logs.” The project is designed to produce consistent, auditable events for traceability, compliance, and later analysis.

### 4. Versioned infrastructure

Infrastructure is treated as code to ensure repeatability, reviewability, and traceability of changes.

---

## Technology stack

### Backend

* **Go** for the BFF and part of the microservices.
* **Java / Spring Boot** for domain services where appropriate.
* **gRPC** and/or internal service integration depending on the flow.
* **REST/OpenAPI** for the externally exposed BFF layer.

### Frontend

* **Angular** for the web interface.

### Security

* **Amazon Cognito** for authentication.
* **AWS Verified Permissions** for policy-based authorization.
* **Cedar** as the policy language.
* **JWT + JWKS** for identity validation.

### Infrastructure / Cloud

* **AWS CloudFormation** for infrastructure as code.
* **AWS ECS Fargate** as the primary cloud-native compute strategy.
* **EventBridge / logs / observability** for auditing and monitoring.

### Local development

* **Docker / Docker Compose** for local environments and integrated testing.

---

## Security flow summary

1. The user interacts with the frontend.
2. The frontend communicates with the **BFF**.
3. The BFF validates the session/token and applies input controls.
4. The BFF calls the corresponding microservice.
5. The microservice resolves the resource, ownership, and context.
6. **AWS Verified Permissions** is queried to decide `ALLOW` or `DENY`.
7. The operation proceeds or is blocked.
8. An audit event is emitted with the decision and relevant context.

---

## Infrastructure as code

The `infra/` folder follows a modular structure intended to separate responsibilities by layer, for example:

* foundation;
* networking;
* data;
* identity;
* edge;
* compute;
* audit/observability;
* authorization.

This approach allows the platform to evolve without mixing network, identity, data, and compute configuration in a single monolithic template.

---

## Local setup

> Note: exact environment variable names, profiles, and dependencies may evolve over time as the project grows.

### Prerequisites

* Docker
* Docker Compose
* Go
* Java
* Node.js / npm
* AWS CLI

### General steps

```bash
# 1. Clone the repository
git clone <repo-url>
cd microservice-workspace

# 2. Start local dependencies
docker compose up -d

# 3. Run BFF / microservices / frontend depending on each module
# Check the internal READMEs in each folder for module-specific commands
```

---

## What this project demonstrates as a portfolio

This repository is not meant to be just another CRUD demo. It is designed to demonstrate experience in:

* secure microservices architecture;
* cross-cutting security control design;
* integration between identity, authorization, and domain logic;
* AWS as a deployment platform;
* infrastructure as code;
* traceability, auditing, and observability;
* modular and scalable design for sensitive systems.

From a portfolio perspective, the value of the project lies in showing **architectural judgment**, **secure design**, **multi-stack integration capability**, and **alignment with security standards**.

---

## Project status

**Current status:** under active development and continuous evolution.

The decisions and components documented here represent the main architectural baseline of the project. Some modules may still be under refactoring or expansion as implementation progresses.

---

## Next steps

* complete internal documentation per microservice;
* publish technical diagrams per flow;
* add deployment guidance per environment;
* document abuse cases and mitigating controls;
* include evidence of security testing and validation.

---

## Author

**Santiago Tumbaco**
Computer Science Engineer
Master’s in Cybersecurity
Focused on backend engineering, distributed systems, cloud, and applied security.

---

## Final note

This project represents a practical proposal for how to build a distributed platform with security integrated from the design stage, combining modern architecture, decoupled services, and cloud infrastructure under principles of traceability, control, and scalability.
