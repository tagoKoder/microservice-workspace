package com.santiago_tumbaco.identity.service;

import com.santiago_tumbaco.identity.domain.dto.PersonDto;
import com.santiago_tumbaco.identity.domain.dto.WhoAmIResult;
import com.santiago_tumbaco.identity.domain.model.Person;
import com.santiago_tumbaco.identity.domain.model.UserAccount;
import com.santiago_tumbaco.identity.domain.service.IdpService;
import com.santiago_tumbaco.identity.repository.PersonRepository;
import com.santiago_tumbaco.identity.repository.UserAccountRepository;
import com.santiago_tumbaco.identity.security.MultiIssuerJwtVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional
public class IdpServiceImpl implements IdpService {

  private final PersonRepository persons;
  private final UserAccountRepository accounts;
  private final MultiIssuerJwtVerifier jwtVerifier;
  private final RestTemplate http = new RestTemplate();

  @Value("${authentik.host}") String authHost;
  @Value("${authentik.adminToken}") String adminToken;

  public IdpServiceImpl(PersonRepository p, UserAccountRepository a, MultiIssuerJwtVerifier v) {
    this.persons = p; this.accounts = a; this.jwtVerifier = v;
  }

  // ----- VERIFY JWT TOKEN (multi-issuer) -----
  public Jwt verifyJWT(String rawJwt) {
    return jwtVerifier.verify(rawJwt);
  }

  // ----- LINK desde ID Token -----
  public WhoAmIResult linkFromIdToken(Jwt jwt) {
    String iss   = jwt.getClaimAsString("iss");
    String sub   = jwt.getClaimAsString("sub");          // sujeto del AS
    String email = nz(jwt.getClaimAsString("email"));
    String uname = nz(jwt.getClaimAsString("preferred_username"));
    String akUUID= jwt.getClaimAsString("ak_uuid");      // authentik lo incluye en ID/Access token
    String accountIdClaim = jwt.getClaimAsString("account_id");

    Person p = persons.findByEmail(email).orElseGet(() -> {
      var np = new Person();
      np.setEmail(email);
      np.setName(uname.isBlank() ? email : uname);
      np.setCreatedAt(Instant.now());
      return persons.save(np);
    });

    UserAccount ua = accounts.findByIdpIssuerAndIdpSub(iss, sub).orElseGet(() -> {
      var nu = new UserAccount();
      nu.setIdpIssuer(iss);
      nu.setIdpSub(sub != null ? sub : nz(akUUID)); // fallback
      nu.setEmail(email);
      nu.setUsername(uname);
      nu.setPerson(p);
      nu.setCreatedAt(Instant.now());
      return accounts.save(nu);
    });

    // Sincroniza "account_id" hacia authentik (atributo del user)
    if (akUUID != null && (accountIdClaim == null || !Objects.equals(accountIdClaim, String.valueOf(ua.getId())))) {
      var body = Map.of("attributes", Map.of("account_id", String.valueOf(ua.getId())));
      var h = new HttpHeaders(); h.setContentType(MediaType.APPLICATION_JSON); h.setBearerAuth(adminToken);
      try {
        http.exchange(authHost + "/api/v3/core/users/" + akUUID + "/", HttpMethod.PATCH, new HttpEntity<>(body, h), Map.class);
      } catch (Exception ignore) {}
    }

    var dto = new PersonDto(p.getId(), nz(p.getName()), nz(p.getLastName()), nz(p.getEmail()));
    return new WhoAmIResult(ua.getId(), dto);
  }

  // ----- WHOAMI (desde Access Token) -----
  @Transactional(readOnly = true)
  public WhoAmIResult whoAmIFromAccessToken(Jwt jwt) {
    var iss = jwt.getClaimAsString("iss");
    var sub = jwt.getClaimAsString("sub");

    var ua = accounts.findWithPersonByIdpIssuerAndIdpSub(iss, sub).orElseThrow(); // 404
    var p = ua.getPerson();
    var dto = new PersonDto(p.getId(), nz(p.getName()), nz(p.getLastName()), nz(p.getEmail()));
    return new WhoAmIResult(ua.getId(), dto);
  }

  private static String nz(String s){ return s == null ? "" : s; }
}
