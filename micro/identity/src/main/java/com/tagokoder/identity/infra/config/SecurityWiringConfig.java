package com.tagokoder.identity.infra.config;

import com.tagokoder.identity.infra.out.audit.AuditPublisher;
import com.tagokoder.identity.infra.security.authz.ActionResolver;
import com.tagokoder.identity.infra.security.authz.ResourceResolver;
import com.tagokoder.identity.infra.security.avp.AvpAuthorizer;
import com.tagokoder.identity.infra.security.grpc.AuthzServerInterceptor;
import com.tagokoder.identity.infra.security.grpc.CorrelationServerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.verifiedpermissions.VerifiedPermissionsClient;

@Configuration
public class SecurityWiringConfig {

    @Bean
    public CorrelationServerInterceptor correlationServerInterceptor(AppProps props) {
        return new CorrelationServerInterceptor(props);
    }

    @Bean
    public ActionResolver actionResolver() {
        return new ActionResolver();
    }

    @Bean
    public ResourceResolver resourceResolver() {
        return new ResourceResolver();
    }

    @Bean
    public AuditPublisher auditPublisher(EventBridgeClient eb, AppProps props) {
        return new AuditPublisher(eb, props);
    }

    @Bean
    public AvpAuthorizer avpAuthorizer(VerifiedPermissionsClient avp, AppProps props) {
        return new AvpAuthorizer(avp, props);
    }

    @Bean
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
