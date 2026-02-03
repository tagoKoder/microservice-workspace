package com.tagokoder.account.infra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import com.tagokoder.account.domain.port.out.AccountRepositoryPort;
import com.tagokoder.account.infra.out.audit.AuditPublisher;
import com.tagokoder.account.infra.security.authz.ActionResolver;
import com.tagokoder.account.infra.security.authz.ResourceResolver;
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

     // ✅ local = no auth
    @Bean(name = "authzInterceptor")
    @Profile("local")
    public ServerInterceptor authzInterceptorLocal() {
        return new NoAuthzServerInterceptor();
    }

    @Bean
    @Profile("!local")
    public ActionResolver actionResolver() {
        return new ActionResolver();
    }

    @Bean
    @Profile("!local")
    public ResourceResolver resourceResolver(AccountRepositoryPort accountRepo) {
        return new ResourceResolver(accountRepo);
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
    public AuthzServerInterceptor authzServerInterceptor(
            AppProps props,
            JwtDecoder jwtDecoder,
            AvpAuthorizer avpAuthorizer,
            AuditPublisher auditPublisher,
            ActionResolver actionResolver,
            ResourceResolver resourceResolver
    ) {
        return new AuthzServerInterceptor(
                props,
                jwtDecoder,
                avpAuthorizer,
                auditPublisher,
                actionResolver,
                resourceResolver
        );
    }
}
