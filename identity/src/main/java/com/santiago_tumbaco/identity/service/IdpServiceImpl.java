package com.santiago_tumbaco.identity.service;


import com.santiago_tumbaco.identity.domain.dto.PersonDto;
import com.santiago_tumbaco.identity.domain.dto.WhoAmIResult;
import com.santiago_tumbaco.identity.domain.model.Person;
import com.santiago_tumbaco.identity.domain.model.UserAccount;
import com.santiago_tumbaco.identity.domain.service.IdpService;
import com.santiago_tumbaco.identity.repository.PersonRepository;
import com.santiago_tumbaco.identity.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;

@Service
@Transactional
public class IdpServiceImpl implements IdpService {

  private final PersonRepository persons;
  private final UserAccountRepository accounts;
  private final RestTemplate http = new RestTemplate();

  @Value("${authentik.host}") String authHost;
  @Value("${authentik.adminToken}") String adminToken;
  @Value("${authentik.jwks}") String jwks;
  @Value("${authentik.issuer}") String issuer;

  private final JwtDecoder decoder;

  public IdpServiceImpl(PersonRepository p, UserAccountRepository a) {
    this.persons = p; this.accounts = a;
    NimbusJwtDecoder d = NimbusJwtDecoder.withJwkSetUri(jwks).build();
    d.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
    this.decoder = d;
  }
  // ----- VERIFY JWT TOKEN -----
  public Jwt verifyJWT(String rawJwt) {
    if (rawJwt == null || rawJwt.isBlank()) throw new BadCredentialsException("missing token");
    return decoder.decode(rawJwt);
  }

  // ----- LINK -----
  public WhoAmIResult linkFromIdToken(Jwt jwt) {
    String iss   = jwt.getClaimAsString("iss");
    String sub   = jwt.getClaimAsString("sub");
    String email = jwt.getClaimAsString("email");
    String uname = jwt.getClaimAsString("preferred_username");
    String akUUID= jwt.getClaimAsString("ak_uuid");
    String accountIdClaim = jwt.getClaimAsString("account_id");

    // upsert person
    Person p = persons.findByEmail(email).orElseGet(() -> {
      var np = new Person();
      np.setEmail(email);
      np.setName(uname != null ? uname : email);
      np.setCreatedAt(Instant.now());
      return persons.save(np);
    });

    // upsert user_account by (iss, sub)
    UserAccount ua = accounts.findByIdpIssuerAndIdpSub(iss, sub).orElseGet(() -> {
      var nu = new UserAccount();
      nu.setIdpIssuer(iss);
      nu.setIdpSub(sub);
      nu.setAccountType("staff"); // HARCODED VALUE
      nu.setEmail(email);
      nu.setUsername(uname);
      nu.setPerson(p);
      nu.setCreatedAt(Instant.now());
      return accounts.save(nu);
    });

    // PATCH account_id in Authentik if fails
    if (akUUID != null && (accountIdClaim == null || !accountIdClaim.equals(String.valueOf(ua.getId())))) {
      var body = Map.of("attributes", Map.of("account_id", String.valueOf(ua.getId())));
      var h = new HttpHeaders(); h.setContentType(MediaType.APPLICATION_JSON); h.setBearerAuth(adminToken);
      try {
        http.exchange(authHost + "/api/v3/core/users/" + akUUID + "/", HttpMethod.PATCH, new HttpEntity<>(body, h), Map.class);
      } catch (Exception ignore) {}
    }

    var dto = new PersonDto(p.getId(), nz(p.getName()), nz(p.getLastName()), nz(p.getEmail()));
    return new WhoAmIResult(ua.getId(), dto);
  }

  // ----- WHOAMI -----
  @Transactional(readOnly = true)
  public WhoAmIResult whoAmIFromAccessToken(Jwt jwt) {
    var iss = jwt.getClaimAsString("iss");
    var sub = jwt.getClaimAsString("sub");

    var ua = accounts.findWithPersonByIdpIssuerAndIdpSub(iss, sub)
        .orElseThrow(); // 404/NotFound

    var p = ua.getPerson(); // it comes initialized because of EntityGraph
    var dto = new PersonDto(p.getId(), nz(p.getName()), nz(p.getLastName()), nz(p.getEmail()));
    return new WhoAmIResult(ua.getId(), dto);
  }

  private static String nz(String s){ return s == null ? "" : s; }
}