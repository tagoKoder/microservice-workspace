# 30-identity-cognito

Crea por ambiente:
- Cognito User Pool
- Domain prefix (Hosted UI / Managed Login)
- App Client para el BFF (Authorization Code + PKCE)

Callback:
- https://api.<dominio>/api/v1/auth/oidc/callback

Logout redirect:
- https://app.<dominio>/

Outputs exportados:
- CognitoIssuerUri
- CognitoClientId
- CognitoHostedUiBaseUrl / endpoints authorize/token/logout
- Discovery URL (.well-known/openid-configuration)
