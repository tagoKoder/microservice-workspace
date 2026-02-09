package com.tagokoder.identity.infra.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import com.tagokoder.identity.application.AppProps;
import com.tagokoder.identity.infra.out.audit.AuditPublisher;
import com.tagokoder.identity.infra.security.authz.ActionResolver;
import com.tagokoder.identity.infra.security.authz.ResourceResolver;
import com.tagokoder.identity.infra.security.avp.AvpAuthorizer;
import com.tagokoder.identity.infra.security.grpc.AuthzServerInterceptor;
import com.tagokoder.identity.infra.security.grpc.CorrelationServerInterceptor;
import com.tagokoder.identity.infra.security.grpc.NoAuthzServerInterceptor;

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
    public ResourceResolver resourceResolver() {
        return new ResourceResolver();
    }

    @Bean(name = "authzAuditPublisher")
    @Profile("!local")
    public AuditPublisher auditPublisher(EventBridgeClient eb, AppProps props) {
        return new AuditPublisher(eb, props);
    }

    @Bean
    @Profile("!local")
    public AvpAuthorizer avpAuthorizer(VerifiedPermissionsClient avp, AppProps props) {
        return new AvpAuthorizer(avp, props);
    }

    @Bean
    @Profile("!local")
    public AuthzServerInterceptor authzServerInterceptor(
            AppProps props,
            JwtDecoder jwtDecoder,
            AvpAuthorizer avpAuthorizer,
            @Qualifier("authzAuditPublisher") AuditPublisher auditPublisher,
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
