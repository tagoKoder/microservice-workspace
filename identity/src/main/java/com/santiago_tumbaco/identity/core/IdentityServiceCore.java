package com.santiago_tumbaco.identity.core;


import com.santiago_tumbaco.identity.domain.model.Person;
import com.santiago_tumbaco.identity.domain.model.UserAccount;
import com.santiago_tumbaco.identity.repository.PersonRepository;
import com.santiago_tumbaco.identity.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;

@Service
@Transactional
public class IdentityServiceCore {

  private final PersonRepository persons;
  private final UserAccountRepository accounts;
  private final RestTemplate http = new RestTemplate();

  @Value("${authentik.host}") String authHost;
  @Value("${authentik.adminToken}") String adminToken;

  public IdentityServiceCore(PersonRepository p, UserAccountRepository a) {
    this.persons = p; this.accounts = a;
  }

  public record Result(Long accountId, Person p){}

  public Result linkFromIdToken(Jwt jwt) {
    String iss   = jwt.getClaimAsString("iss");
    String sub   = jwt.getClaimAsString("sub");
    String email = jwt.getClaimAsString("email");
    String uname = jwt.getClaimAsString("preferred_username");
    String akUUID= jwt.getClaimAsString("ak_uuid");
    String accountIdClaim = jwt.getClaimAsString("account_id");

    // upsert person
    Person p = persons.findByEmail(email).orElseGet(() -> {
      var np = new Person(); np.setEmail(email); np.setName(uname!=null?uname:email); np.setCreatedAt(Instant.now());
      return persons.save(np);
    });

    // upsert user_account by (iss, sub)
    UserAccount ua = accounts.findByIdpIssuerAndIdpSub(iss, sub).orElseGet(() -> {
      var nu = new UserAccount();
      nu.setIdpIssuer(iss); nu.setIdpSub(sub); nu.setAccountType("staff");
      nu.setEmail(email); nu.setUsername(uname); nu.setPerson(p); nu.setCreatedAt(Instant.now());
      return accounts.save(nu);
    });

    // PATCH account_id a Authentik si falta
    if (akUUID != null && (accountIdClaim==null || !accountIdClaim.equals(String.valueOf(ua.getId())))) {
      var body = Map.of("attributes", Map.of("account_id", String.valueOf(ua.getId())));
      var h = new HttpHeaders(); h.setContentType(MediaType.APPLICATION_JSON); h.setBearerAuth(adminToken);
      try {
        http.exchange(authHost+"/api/v3/core/users/"+akUUID+"/", HttpMethod.PATCH, new HttpEntity<>(body, h), Map.class);
      } catch (Exception ignore) {}
    }
    return new Result(ua.getId(), p);
  }

  public Result whoAmIFromAccessToken(Jwt jwt) {
    var iss = jwt.getClaimAsString("iss");
    var sub = jwt.getClaimAsString("sub");
    var ua = accounts.findByIdpIssuerAndIdpSub(iss, sub).orElseThrow();
    return new Result(ua.getId(), ua.getPerson());
  }
}