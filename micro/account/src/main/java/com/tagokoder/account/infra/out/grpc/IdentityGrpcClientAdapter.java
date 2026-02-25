package com.tagokoder.account.infra.out.grpc;

import org.springframework.stereotype.Component;

import com.tagokoder.account.domain.port.out.IdentityPrincipalPort;

import bank.identity.v1.PrincipalServiceGrpc;
import bank.identity.v1.ResolvePrincipalRequest;
import bank.identity.v1.ResolvePrincipalResponse;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;

@Component
public class IdentityGrpcClientAdapter implements IdentityPrincipalPort {

  private static final Metadata.Key<String> AUTH =
      Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

  private final PrincipalServiceGrpc.PrincipalServiceBlockingStub principalStub;

  public IdentityGrpcClientAdapter(PrincipalServiceGrpc.PrincipalServiceBlockingStub principalStub) {
    this.principalStub = principalStub;
  }

  @Override
  public PrincipalInfo resolvePrincipal(String bearerAccessToken, boolean requireLink) {

    ResolvePrincipalRequest req = ResolvePrincipalRequest.newBuilder()
        .setRequireCustomerLink(requireLink)
        .setPurpose("accounts:read")
        .build();

    Metadata md = new Metadata();
    md.put(AUTH, "Bearer " + bearerAccessToken);

    // Adjunta metadata a ESTE call usando interceptor
    var stubWithHeaders = principalStub.withInterceptors(
        MetadataUtils.newAttachHeadersInterceptor(md)
    );

    ResolvePrincipalResponse resp = stubWithHeaders.resolvePrincipal(req);
    System.out.println("Respuesta identityPrincipal: "+ resp.getCustomerId());

    return new PrincipalInfo(
        resp.getSubjectIdOidc(),
        resp.getPrincipalId(),
        resp.getCustomerId(),
        resp.getRolesList(),
        resp.getUserStatus()
    );
  }
}