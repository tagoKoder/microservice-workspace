package com.tagokoder.identity.infra.in.grpc;

import java.util.List;

import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import com.tagokoder.identity.domain.port.out.IdentityLinkRepositoryPort;
import com.tagokoder.identity.domain.port.out.IdentityRepositoryPort;
import com.tagokoder.identity.infra.security.grpc.GrpcAuth;

import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import bank.identity.v1.*;
import io.grpc.Status;
import org.slf4j.Logger;


@GrpcService
public class PrincipalGrpcService extends PrincipalServiceGrpc.PrincipalServiceImplBase {

  private final JwtDecoder jwtDecoder;
  private final IdentityRepositoryPort identities;
  private final IdentityLinkRepositoryPort identityLinks;
private static final Logger log = LoggerFactory.getLogger(PrincipalGrpcService.class);

  public PrincipalGrpcService(
      JwtDecoder jwtDecoder,
      IdentityRepositoryPort identities,
      IdentityLinkRepositoryPort identityLinks
  ) {
    this.jwtDecoder = jwtDecoder;
    this.identities = identities;
    this.identityLinks = identityLinks;
  }

  @Override
  public void resolvePrincipal(ResolvePrincipalRequest request,
      StreamObserver<ResolvePrincipalResponse> responseObserver) {

    try {
      // 1) extraer bearer token desde metadata
      String token = GrpcAuth.extractBearerOrThrow();
      Jwt jwt = jwtDecoder.decode(token);

      String sub = jwt.getSubject();
      String principalId = buildPrincipalId(jwt); // pool|sub (igual que account)

      // 2) roles: desde token (cognito:groups) si vienen; si no, vacío
      List<String> roles = jwt.getClaimAsStringList("cognito:groups");
      if (roles == null) roles = List.of();

      // 3) user_status: desde tu tabla identity
      var id = identities.findBySubjectAndProvider(sub, "cognito");
      String userStatus = id.map(x -> x.getUserStatus().name()).orElse("ACTIVE");

      // 4) customer_id: desde identity_links
      String customerId = id.flatMap(x -> identityLinks.findCustomerIdByIdentityId(x.getId()))
                            .orElse("");

      if (request.getRequireCustomerLink() && (customerId == null || customerId.isBlank())) {
        responseObserver.onError(Status.NOT_FOUND.withDescription("CUSTOMER_LINK_NOT_FOUND").asRuntimeException());
        return;
      }

      var resp = ResolvePrincipalResponse.newBuilder()
          .setSubjectIdOidc(sub)
          .setPrincipalId(principalId)
          .setCustomerId(customerId == null ? "" : customerId)
          .addAllRoles(roles)
          .setUserStatus(userStatus)
          .build();

      responseObserver.onNext(resp);
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
        log.warn("ResolvePrincipal failed status={} desc={}",
          e.getStatus().getCode(), e.getStatus().getDescription());
      responseObserver.onError(e);
    } catch (Exception e) {
        log.error("ResolvePrincipal INTERNAL error", e);
      responseObserver.onError(Status.INTERNAL.withDescription("INTERNAL").asRuntimeException());
    }
  }

  private static String buildPrincipalId(Jwt jwt) {
    String iss = jwt.getIssuer() != null ? jwt.getIssuer().toString() : "";
    String poolId = iss.substring(iss.lastIndexOf('/') + 1);
    return poolId + "|" + jwt.getSubject();
  }
}