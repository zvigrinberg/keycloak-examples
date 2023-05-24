package com.redhat.zgrinber.keycloakintegrationspringboot.config;

import com.redhat.zgrinber.keycloakintegrationspringboot.keycloak.KeycloakResource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class VariousBeans {



@Bean
@Lazy
public RestTemplate restTemplate()
{
    return new RestTemplate();
}



//@Bean("keycloakResources")
@Bean
@ConfigurationProperties(prefix = "keycloak.parameters.resources")
public List<KeycloakResource> keycloakResources()
{
    return new ArrayList<>();
}

@Bean
@ConfigurationProperties(prefix = "keycloak.parameters.custom-client-scopes")
public List<String> customClientScopes()
{
    return new ArrayList<>();
}
@Bean
@ConfigurationProperties(prefix = "keycloak.parameters.roles")
public List<String> customRoles()
{
    return new ArrayList<>();
}

}