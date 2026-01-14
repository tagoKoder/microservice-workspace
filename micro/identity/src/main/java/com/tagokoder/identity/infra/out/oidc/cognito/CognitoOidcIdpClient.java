package com.tagokoder.identity.infra.out.oidc.cognito;

import com.tagokoder.identity.application.OidcProperties;
import com.tagokoder.identity.domain.port.out.OidcIdpClientPort;
import com.tagokoder.identity.infra.out.oidc.OidcDiscoveryClient;
import com.tagokoder.identity.infra.out.oidc.ProviderMetadata;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Component
public class CognitoOidcIdpClient implements OidcIdpClientPort {

    private final OidcProperties props;
    private final OidcDiscoveryClient discovery;
    private final WebClient client;

    public CognitoOidcIdpClient(OidcProperties props, OidcDiscoveryClient discovery, WebClient.Builder builder) {
        this.props = props;
        this.discovery = discovery;
        this.client = builder.build();
    }

    @Override
    public String buildAuthorizationUrl(String state, String nonce, String codeChallenge, String redirectUri) {
        var meta = discovery.getMetadata();

        // Cognito soporta /oauth2/authorize (Hosted UI / Managed Login)
        return UriComponentsBuilder.fromUriString(meta.authorization_endpoint())
                .queryParam("client_id", props.getClientId())
                .queryParam("response_type", "code")
                .queryParam("scope", props.getScope())
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", state)
                .queryParam("nonce", nonce)
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", "S256")
                .build(true)
                .toUriString();
    }

    @Override
    public TokenResult exchangeCodeForTokens(String code, String codeVerifier, String redirectUri) {
        var meta = discovery.getMetadata();

        var form = BodyInserters.fromFormData("grant_type", "authorization_code")
                .with("client_id", props.getClientId())
                .with("code", code)
                .with("redirect_uri", redirectUri)
                .with("code_verifier", codeVerifier);

        // Si el client tiene secret, usa Basic Auth (evitas secret en body/logs)
        WebClient.RequestHeadersSpec<?> req = client.post()
                .uri(meta.token_endpoint())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form);

        if (props.getClientSecret() != null && !props.getClientSecret().isBlank()) {
            req = req.headers(h -> h.setBasicAuth(props.getClientId(), props.getClientSecret()));
        }

        var resp = req.retrieve()
                .bodyToMono(TokenEndpointResponse.class)
                .block();

        return new TokenResult(resp.access_token(), resp.refresh_token(), resp.expires_in(), resp.id_token());
    }

    @Override
    public TokenResult refreshTokens(String refreshToken) {
        var meta = discovery.getMetadata();

        var form = BodyInserters.fromFormData("grant_type", "refresh_token")
                .with("client_id", props.getClientId())
                .with("refresh_token", refreshToken);

        WebClient.RequestHeadersSpec<?> req = client.post()
                .uri(meta.token_endpoint())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form);

        if (props.getClientSecret() != null && !props.getClientSecret().isBlank()) {
            req = req.headers(h -> h.setBasicAuth(props.getClientId(), props.getClientSecret()));
        }

        var resp = req.retrieve()
                .bodyToMono(TokenEndpointResponse.class)
                .block();

        return new TokenResult(resp.access_token(), resp.refresh_token(), resp.expires_in(), resp.id_token());
    }

    @Override
    public void revokeRefreshToken(String refreshToken) {
        var meta = discovery.getMetadata();
        if (meta.revocation_endpoint() == null || meta.revocation_endpoint().isBlank()) return;

        var form = BodyInserters.fromFormData("token", refreshToken)
                .with("token_type_hint", "refresh_token")
                .with("client_id", props.getClientId());

        WebClient.RequestHeadersSpec<?> req = client.post()
                .uri(meta.revocation_endpoint())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form);

        if (props.getClientSecret() != null && !props.getClientSecret().isBlank()) {
            req = req.headers(h -> h.setBasicAuth(props.getClientId(), props.getClientSecret()));
        }

        req.retrieve().toBodilessEntity().block();
    }

    @Override
    public UserInfoResult fetchUserInfo(String accessToken) {
        var meta = discovery.getMetadata();

        var resp = client.get()
                .uri(meta.userinfo_endpoint())
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(UserInfoResponse.class)
                .block();

        // Cognito userInfo normalmente trae sub/email/username; grupos suelen venir en token claim.
        return new UserInfoResult(
                resp.sub(),
                resp.email(),
                resp.name(),
                List.of()
        );
    }

    record TokenEndpointResponse(String access_token, String refresh_token, long expires_in, String token_type, String id_token) {}
    record UserInfoResponse(String sub, String email, String name, String username) {}
}
