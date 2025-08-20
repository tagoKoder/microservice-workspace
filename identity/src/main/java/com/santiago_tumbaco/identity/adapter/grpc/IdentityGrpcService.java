package com.santiago_tumbaco.identity.adapter.grpc;
import com.google.protobuf.Empty;
import com.santiago_tumbaco.identity.config.MetadataInterceptor;
import com.santiago_tumbaco.identity.core.IdentityServiceCore;
import com.santiago_tumbaco.identity.oidc.OidcVerifier;

// === STUBS GENERADOS POR PROTO ===
import com.evacent.identity.proto.*;
// =================================

import io.grpc.Context;
import org.springframework.grpc.server.service.GrpcService;  // <- starter oficial
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Objects;

@GrpcService
public class IdentityGrpcService extends IdentityServiceGrpc.IdentityServiceImplBase {

  private final OidcVerifier verifier;
  private final IdentityServiceCore core;

  public IdentityGrpcService(OidcVerifier v, IdentityServiceCore c) {
    this.verifier = v; this.core = c;
  }

  private static String bearerFromContext() {
    return MetadataInterceptor.AUTH_BEARER.get(Context.current());
  }

  @Override
  public void link(Empty req, io.grpc.stub.StreamObserver<LinkResponse> resp) {
    Jwt idt = verifier.verify(bearerFromContext());
    var r = core.linkFromIdToken(idt);

    PersonView pv = PersonView.newBuilder()
        .setId(r.p().getId())
        .setName(Objects.toString(r.p().getName(), ""))
        .setLastName(Objects.toString(r.p().getLastName(), ""))
        .setEmail(Objects.toString(r.p().getEmail(), ""))
        .build();

    resp.onNext(LinkResponse.newBuilder()
        .setAccountId(r.accountId())
        .setPerson(pv)
        .build());
    resp.onCompleted();
  }

  @Override
  public void whoAmI(Empty req, io.grpc.stub.StreamObserver<WhoAmIResponse> resp) {
    Jwt at = verifier.verify(bearerFromContext());
    var r = core.whoAmIFromAccessToken(at);
    
    PersonView pv = PersonView.newBuilder()
        .setId(r.p().getId())
        .setName(Objects.toString(r.p().getName(), ""))
        .setLastName(Objects.toString(r.p().getLastName(), ""))
        .setEmail(Objects.toString(r.p().getEmail(), ""))
        .build();

    resp.onNext(WhoAmIResponse.newBuilder()
        .setAccountId(r.accountId())
        .setPerson(pv)
        .build());
    resp.onCompleted();
  }
}