package com.tagokoder.account.infra.config;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.tagokoder.account.infra.security.grpc.AuthzServerInterceptor;
import com.tagokoder.account.infra.security.grpc.CorrelationServerInterceptor;

import java.util.List;

@Configuration
public class GrpcServerConfig {

    @Value("${grpc.server.port:9091}")
    private int grpcPort;

    @Bean(destroyMethod = "shutdown")
    public Server grpcServer(
            List<BindableService> services,
            CorrelationServerInterceptor correlationInterceptor,
            AuthzServerInterceptor authzInterceptor
    ) {
        NettyServerBuilder builder = NettyServerBuilder.forPort(grpcPort)
                // OJO: correlation primero, luego authz
                .intercept(correlationInterceptor)
                .intercept(authzInterceptor);

        services.forEach(builder::addService);

        return builder.build();
    }

    @Bean
    public ApplicationRunner grpcServerRunner(Server server) {
        return args -> {
            server.start();
            Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));
        };
    }
}
