package com.tagokoder.identity.infra.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {

  @Bean
  public WebClient.Builder webClientBuilder() {
    HttpClient httpClient = HttpClient.create()
        .responseTimeout(Duration.ofSeconds(3));

    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient));
  }
}
