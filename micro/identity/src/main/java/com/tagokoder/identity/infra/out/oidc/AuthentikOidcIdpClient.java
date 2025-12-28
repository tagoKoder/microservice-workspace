package com.tagokoder.identity.infra.out.oidc;


import com.tagokoder.identity.application.OidcProperties;
import com.tagokoder.identity.domain.port.out.OidcIdpClientPort;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class AuthentikOidcIdpClient implements OidcIdpClientPort {

    private final OidcProperties props;
    private final WebClient client;

    public AuthentikOidcIdpClient(OidcProperties props, WebClient.Builder builder) {
        this.props = props;
        this.client = builder.build();
    }

    @Override
    public String buildAuthorizationUrl(String state, String codeChallenge, String redirectUri) {
        String scope = "openid profile email offline_access ak_groups";

        if (props.isUsePar()) {
            // PAR: mandamos el payload al endpoint /par y usamos request_uri
            var parResp = client.post()
                    .uri(props.getParUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData("client_id", props.getClientId())
                            .with("response_type", "code")
                            .with("scope", scope)
                            .with("redirect_uri", redirectUri)
                            .with("state", state)
                            .with("code_challenge", codeChallenge)
                            .with("code_challenge_method", "S256"))
                    .retrieve()
                    .bodyToMono(ParResponse.class)
                    .block();

            String base = props.getAuthUrl();
            String q = "client_id=" + enc(props.getClientId()) +
                    "&request_uri=" + enc(parResp.request_uri());
            return base + "?" + q;
        }

        String base = props.getAuthUrl();

        String query = "client_id=" + enc(props.getClientId()) +
                "&response_type=code" +
                "&scope=" + enc(scope) +
                "&redirect_uri=" + enc(redirectUri) +
                "&state=" + enc(state) +
                "&code_challenge=" + enc(codeChallenge) +
                "&code_challenge_method=S256";

        return base + "?" + query;
    }

    @Override
    public TokenResult exchangeCodeForTokens(String code, String codeVerifier, String redirectUri) {
        var body = BodyInserters.fromFormData("grant_type", "authorization_code")
                .with("code", code)
                .with("redirect_uri", redirectUri)
                .with("client_id", props.getClientId())
                .with("client_secret", props.getClientSecret())
                .with("code_verifier", codeVerifier);

        var resp = client.post()
                .uri(props.getTokenUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .bodyToMono(TokenEndpointResponse.class)
                .block();

        return new TokenResult(resp.access_token(), resp.refresh_token(), resp.expires_in());
    }

    @Override
    public UserInfoResult fetchUserInfo(String accessToken) {
        var resp = client.get()
                .uri(props.getUserInfoUrl())
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(UserInfoResponse.class)
                .block();

        return new UserInfoResult(
                resp.sub(),
                resp.email(),
                resp.name(),
                resp.ak_groups() != null ? List.copyOf(resp.ak_groups()) : List.of()
        );
    }

    private String enc(String v) {
        return URLEncoder.encode(v, StandardCharsets.UTF_8);
    }

    // Records de respuesta

    record ParResponse(String request_uri, long expires_in) {}
    record TokenEndpointResponse(String access_token, String refresh_token, long expires_in,
                                 String token_type, String id_token) {}
    record UserInfoResponse(String sub, String email, String name, java.util.List<String> ak_groups) {}
}
