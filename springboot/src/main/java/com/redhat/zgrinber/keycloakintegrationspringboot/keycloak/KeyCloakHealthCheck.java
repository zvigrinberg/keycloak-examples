package com.redhat.zgrinber.keycloakintegrationspringboot.keycloak;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
public class KeyCloakHealthCheck implements HealthIndicator {


    private RestTemplate restTemplate;
    @Value("${keycloak.parameters.serverAddress}")
    private String keyCloakAddress;
    private static final String keycloakHealthEndpoints = "/health/live";
    @Autowired
    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public Health getHealth(boolean includeDetails) {
        return this.health();
    }

    @Override
    public Health health() {

//        restTemplate.exchange(keyCloakAddress + keycloakHealthEndpoints, HttpMethod.GET,);
        Health indicator;
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(keyCloakAddress + keycloakHealthEndpoints, String.class);
            if (response.getStatusCode().value() == HttpStatus.OK.value()) {
                indicator = new Health.Builder().up().withDetail("KeyCloak Server Address", keyCloakAddress).withDetail("Auth Server Status", "UP").build();
            } else {
                indicator = new Health.Builder().down().withDetail("KeyCloak Server Address", keyCloakAddress).withDetail("Auth Server Status", "DOWN").build();
            }
        }
        catch  (Exception e)
        {
            indicator = new Health.Builder().outOfService().withDetail("KeyCloak Server Address", keyCloakAddress).withDetail("Auth Server Status", "DOWN").build();
        }

        return indicator;
    }
}
