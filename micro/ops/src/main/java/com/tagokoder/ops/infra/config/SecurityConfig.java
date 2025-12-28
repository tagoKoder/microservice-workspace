package com.tagokoder.ops.infra.config;

import com.tagokoder.ops.infra.config.OpsProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

public class SecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http, OpsProperties props) throws Exception {
    http.csrf(csrf -> csrf.disable());
    http.httpBasic(Customizer.withDefaults());
    http.addFilterBefore(new InternalTokenFilter(props.getSecurity().getInternalToken()),
        org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

    http.authorizeHttpRequests(auth -> auth
        .requestMatchers("/actuator/**", "/openapi/**", "/swagger/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
        .anyRequest().authenticated()
    );

    return http.build();
  }
}
