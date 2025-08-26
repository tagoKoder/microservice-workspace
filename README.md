# microservice-workspace

# Generación del código base y gRPC para role.proto
protoc -I=protobuf --go_out=genproto/go/example --go_opt=paths=source_relative protobuf/example.proto
protoc -I=protobuf --go-grpc_out=genproto/go/example --go-grpc_opt=paths=source_relative protobuf/example.proto


# Go
protoc -I=protobuf --go_out=genproto/go/identity --go_opt=paths=source_relative protobuf/identity.proto
protoc -I=protobuf --go-grpc_out=genproto/go/identity --go-grpc_opt=paths=source_relative protobuf/identity.proto




0) Nombres fijos que usaremos

Sistemas (providers/apps): admin-web, patient-app (luego system3, system4…).

Grupos de acceso en IdP: access:admin-web, access:patient-app, …

Flows:

clinic-authn (Authentication passwordless, compartible)

clinic-authz-admin (Authorization para admin-web)

clinic-authz-patient (Authorization para patient-app)

enroll-staff (Enrollment staff por invitación)

enroll-patient (Enrollment público pacientes)

1) AuthenTik – Grupos y Policies (bloqueo por sistema)
1.1 Crea los grupos de acceso (una vez)

Directory → Groups → Create

access:admin-web

access:patient-app
(futuro: access:system3, etc.)

1.2 Authorization Flow por sistema (niega si no está en el grupo)
admin-web

Flows → Create

Name: clinic-authorization-admin

Slug: clinic-authz-admin

Designation: Authorization

Policies (bind):

Expression Policy → Create:

Name: allow-admin-web-by-group

Expression:

return ak_user_in_group(request, "access:admin-web")


Bind esta Policy al flow clinic-authz-admin (Order 0, sin negate).

patient-app

Flows → Create

Name: clinic-authorization-patient

Slug: clinic-authz-patient

Designation: Authorization

Policies (bind):

Expression Policy:

return ak_user_in_group(request, "access:patient-app")


Resultado: si el usuario no está en el grupo correcto, AuthenTik no emite token para esa app.

1.3 Asigna los Authorization Flows a los Providers

Applications → Providers:

Provider admin-web → Authorization Flow = clinic-authz-admin

Provider patient-app (cuando lo crees) → Authorization Flow = clinic-authz-patient

(El Authentication Flow passwordless lo vemos en el paso 2.3.)

2) AuthenTik – Flows de Enrollment y Authentication
2.1 Enrollment staff (por invitación)

Flows → Create

Name: clinic-staff-enrollment

Slug: enroll-staff

Designation: Enrollment

Stages (en orden):

Invitation Stage

Single-use: ON

Expires: lo que prefieras (p.ej. 7d)

Identification Stage

Campos: Correo y Nombre de usuario

Password stage: vacío

Avanzado:

Case-insensitive: ON

Pretend user exists: ON

Show matched user: OFF (puedes ON si quieres UX más amable)

WebAuthn Authenticator Setup Stage

User verification: REQUIRED

Attestation: NONE

Attachment: platform y cross-platform

Resident key: PREFERRED (o REQUIRED si vas a “usernameless” estricto)

User Write Stage

Create inactive: OFF

Email verification: OFF

Update existing: ON

(Opcional) 5) User Login Stage.

Este flow solo crea la cuenta en el IdP; la asignación de grupos la hará tu backend en el paso 3, cuando reciba el evento.

2.2 Enrollment paciente (self-service)

Flows → Create

Name: clinic-patient-enrollment

Slug: enroll-patient

Designation: Enrollment

Stages (en orden):

Identification Stage (mismos ajustes que arriba)

WebAuthn Authenticator Setup Stage (mismos ajustes)

(Opcional) Prompt Stage para pedir cédula ahora.

Si prefieres simplicidad, sáltalo y recoge cédula en tu app móvil al primer uso.

User Write Stage (Update existing: ON)

Igual que staff: el grupo access:patient-app lo asigna tu backend al recibir el evento.

2.3 Authentication (passwordless, compartible)

Flows → Create

Name: clinic-authentication

Slug: clinic-authn

Designation: Authentication

Stages:

Identification Stage (igual config)

Authenticator Validation Stage

Allowed: WebAuthn (TOTP sólo si quieres respaldo)

User verification: REQUIRED

Enroll if no device: OFF

User Login Stage

Asigna este Authentication Flow a tus providers (admin-web y el futuro patient-app).

3) AuthenTik → Backend Identity (evento webhook simple)

Esto te evita “carreras”: en cuanto se crea/actualiza el user en el IdP, tu backend hace UPSERT y sincroniza grupos access:*.

3.1 Notification Transport (Webhook)

Events → Notification Transports → Create

Type: Webhook

Name: identity-upsert-transport

URL: https://<tu-identity>/events/authentik-user-upsert

Method: POST

Headers: Authorization: Bearer <adminTokenDeConfianza>

Content-Type: application/json

3.2 Notification Rule

Events → Notification Rules → Create

Name: on-user-upsert

Transport: identity-upsert-transport

Triggers: User created y User updated (si quieres, también Login)

Listo. Cada alta/actualización dispara a tu backend.

4) Backend Identity – lo mínimo que debes implementar
4.1 /events/authentik-user-upsert (UPSERT + sync)

Qué recibes (ejemplo típico):

{
  "issuer": "https://idp/application/o/admin-web/",
  "sub": "uuid-idp",
  "email": "user@clinic.com",
  "flow": "enroll-staff" | "enroll-patient" | "login",
  "ak_uuid": "uuid-interno-authentik",
  "extra": { "target_role": "doctor" } // si staff por invitación
}


Qué haces:

UPSERT person y user_account(issuer, sub) (idempotente).

Si flow=enroll-staff:

Valida regla “un solo staff” (si ya tiene otro staff incompatible → marca la invitación como inválida y no asignes nada).

Asigna rol (person_role) y crea doctor_info/lab_info si aplica.

Si flow=enroll-patient:

Crea patient_info (o déjalo pendiente si recogerás cédula en la app).

syncGroups(person_id):

Calcula sistemas permitidos por su(s) rol(es) desde tu tabla app_system_role_access.

Añade en AuthenTik los grupos access:* necesarios y remueve los que ya no correspondan.

Este “sync” es una sola llamada (o 2) al IdP por alta/cambio. No hay llamadas por request.

4.2 /invitations (staff)

Recibe {email, target_role}.

Valida en tu BD que no tenga staff incompatible.

Crea la invitación en tu BD y opcional crea la Invitation en AuthenTik apuntando al Flow enroll-staff.

Envía el link: https://<idp>/if/flow/enroll-staff/?itoken=...

(Si tu versión permite adjuntar grupo en la Invitation, úsalo; si no, igual lo añade tu sync tras el evento.)

4.3 syncGroups(person_id) (esqueleto)

allowed = SELECT systems permitidos FROM app_system_role_access JOIN person_role …

Llama a la API admin de AuthenTik para:

obtener grupos actuales del usuario (filtra access:*)

toAdd = allowed - current, toRemove = current - allowed

addUserToGroups(ak_uuid, toAdd) / removeUserFromGroups(ak_uuid, toRemove)

Implementa esta función una vez; la llamas desde upsert y cada vez que cambies un rol desde tu panel admin.

5) Gateway – nada raro

Valida solo tokens de AuthenTik (iss) y que el aud sea el esperado por esa app/API.

¡Y ya! No necesitas llamar a Identity por cada request (el IdP ya bloqueó).

6) ¿Y si NO quieres webhook? (Plan B con tu /link)

Puedes empezar sin webhook y usar tu /link en el primer login:

Mantén los Authorization Flows desactivados (para que el IdP sí emita token).

Gateway → en el primer request (o cuando no hay cache) llama a /link:

UPSERT persona + cuenta (como ya haces).

Si quieres, ahí mismo puedes hacer syncGroups (pero ya no hace falta para ese sistema porque el token llegó).

Para no llamar siempre, cachea (iss,sub,system) → allowed 5–15 min.

👉 Sacrificas el bloqueo temprano (IdP puede emitir token para un sistema que no corresponde, pero tu Gateway lo rechazará). Es más simple y puedes migrar a webhook + Authorization Flow más adelante.

Checklist (en 1 hora lo dejas andando)

Crea grupos access:admin-web y access:patient-app.

Crea Authorization Flows clinic-authz-admin y clinic-authz-patient con la Expression Policy de grupo.

Asigna esos flows a los providers.

Configura Notification Transport (Webhook) + Rule (user created/updated).

Backend:

Implementa /events/authentik-user-upsert (UPSERT + syncGroups).

Implementa /invitations (valida reglas y genera link de enroll-staff).

Implementa syncGroups(person_id) (llamando al IdP).

Prueba: invita un doctor → completa el enrollment → verifica que el evento llegó, se asignó rol y quedó en access:admin-web. Intenta entrar a admin-web: token OK. Un paciente que vaya a admin-web: sin token.

