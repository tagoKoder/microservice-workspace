package com.tagokoder.account.infra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import com.tagokoder.account.domain.port.out.IdentityPrincipalPort;
import com.tagokoder.account.infra.out.audit.AuditPublisher;
import com.tagokoder.account.infra.security.authz.AccountResourceTemplates;
import com.tagokoder.account.infra.security.authz.RouteAuthzRegistry;
import com.tagokoder.account.infra.security.avp.AvpAuthorizer;
import com.tagokoder.account.infra.security.grpc.AuthzServerInterceptor;
import com.tagokoder.account.infra.security.grpc.CorrelationServerInterceptor;
import com.tagokoder.account.infra.security.grpc.NoAuthzServerInterceptor;

import io.grpc.ServerInterceptor;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.verifiedpermissions.VerifiedPermissionsClient;

@Configuration
public class SecurityWiringConfig {

    @Bean
    public CorrelationServerInterceptor correlationServerInterceptor(AppProps props) {
        return new CorrelationServerInterceptor(props);
    }

     // local = no auth
    @Bean(name = "authzInterceptor")
    @Profile("local")
    public ServerInterceptor authzInterceptorLocal() {
        return new NoAuthzServerInterceptor();
    }

    @Bean
    @Profile("!local")
    public AuditPublisher auditPublisher(EventBridgeClient eb, AppProps props) {
        return new AuditPublisher(eb, props);
    }

    @Bean
    @Profile("!local")
    public AvpAuthorizer avpAuthorizer(VerifiedPermissionsClient avp, AppProps props) {
        return new AvpAuthorizer(avp, props);
    }

    @Bean(name = "authzInterceptor")
    @Profile("!local")
    public ServerInterceptor authzServerInterceptor(
        AppProps props,
        JwtDecoder jwtDecoder,
        AvpAuthorizer avpAuthorizer,
        AuditPublisher auditPublisher,
        RouteAuthzRegistry registry,
        AccountResourceTemplates templates,
        IdentityPrincipalPort principalPort
    ) {
        return new AuthzServerInterceptor(
            props,
            jwtDecoder,
            avpAuthorizer,
            auditPublisher,
            registry,
            templates,
            principalPort
        );
    }
}
