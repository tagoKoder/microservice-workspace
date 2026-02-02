package com.tagokoder.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.tagokoder.identity.application.AppProps;
import com.tagokoder.identity.application.IdentityClientsProperties;
import com.tagokoder.identity.application.IdentityKycStorageProperties;
import com.tagokoder.identity.application.IdentitySecurityProperties;
import com.tagokoder.identity.application.IdentitySessionProperties;
import com.tagokoder.identity.application.OidcProperties;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableConfigurationProperties({
	AppProps.class,
    IdentityClientsProperties.class,
    IdentityKycStorageProperties.class,
    IdentitySecurityProperties.class,
    IdentitySessionProperties.class,
    OidcProperties.class
})
public class IdentityApplication {

	public static void main(String[] args) {
		SpringApplication.run(IdentityApplication.class, args);
	}

}
