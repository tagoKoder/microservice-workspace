package com.tagokoder.account.infra.out.grpc;

import org.springframework.stereotype.Component;

import com.tagokoder.account.domain.port.out.IdentityPrincipalPort;
import com.tagokoder.account.infra.security.grpc.BearerTokenCallCredentials;

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
  public PrincipalInfo resolvePrincipal(String accessToken, boolean requireLink, String purpose) {

    ResolvePrincipalRequest req = ResolvePrincipalRequest.newBuilder()
        .setRequireCustomerLink(requireLink)
        .setPurpose(purpose)
        .build();

      var resp = principalStub
          .withCallCredentials(new BearerTokenCallCredentials(accessToken))
          .resolvePrincipal(req);
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