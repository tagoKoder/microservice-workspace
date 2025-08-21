package com.santiago_tumbaco.identity.controller;
import com.google.protobuf.Empty;
import com.santiago_tumbaco.identity.config.MetadataInterceptor;
import com.santiago_tumbaco.identity.domain.service.IdpService;
// === STUBS GENERADOS POR PROTO ===
import com.tagoKoder.identity.proto.*;
// =================================

import io.grpc.Context;
import org.springframework.grpc.server.service.GrpcService;  // <- starter oficial
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Objects;

@GrpcService
public class IdentityGrpcController extends IdentityServiceGrpc.IdentityServiceImplBase {

  private final IdpService idpService;

  public IdentityGrpcController(IdpService c) {
    this.idpService = c;
  }

  private static String bearerFromContext() {
    return MetadataInterceptor.AUTH_BEARER.get(Context.current());
  }

  @Override
  public void link(Empty req, io.grpc.stub.StreamObserver<LinkResponse> resp) {
    Jwt idt = idpService.verifyJWT(bearerFromContext());
    var r = idpService.linkFromIdToken(idt);

    PersonView pv = PersonView.newBuilder()
        .setId(r.person().id())
        .setName(Objects.toString(r.person().name(), ""))
        .setLastName(Objects.toString(r.person().lastName(), ""))
        .setEmail(Objects.toString(r.person().email(), ""))
        .build();

    resp.onNext(LinkResponse.newBuilder()
        .setAccountId(r.accountId())
        .setPerson(pv)
        .build());
    resp.onCompleted();
  }

  @Override
  public void whoAmI(Empty req, io.grpc.stub.StreamObserver<WhoAmIResponse> resp) {
    Jwt at = idpService.verifyJWT(bearerFromContext());
    var r = idpService.whoAmIFromAccessToken(at);
    
    PersonView pv = PersonView.newBuilder()
        .setId(r.person().id())
        .setName(Objects.toString(r.person().name(), ""))
        .setLastName(Objects.toString(r.person().lastName(), ""))
        .setEmail(Objects.toString(r.person().email(), ""))
        .build();

    resp.onNext(WhoAmIResponse.newBuilder()
        .setAccountId(r.accountId())
        .setPerson(pv)
        .build());
    resp.onCompleted();
  }
}