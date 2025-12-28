package com.tagokoder.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.tagokoder.identity.application.OidcProperties;

@SpringBootApplication
@EnableConfigurationProperties(OidcProperties.class)
public class IdentityApplication {

	public static void main(String[] args) {
		SpringApplication.run(IdentityApplication.class, args);
	}

}
