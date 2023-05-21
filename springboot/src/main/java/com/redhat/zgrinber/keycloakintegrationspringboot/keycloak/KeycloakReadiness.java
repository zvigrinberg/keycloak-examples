package com.redhat.zgrinber.keycloakintegrationspringboot.keycloak;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RestControllerEndpoint(id = "readiness")
public class KeycloakReadiness {


    private RestTemplate restTemplate;
    @Value("${keycloak.parameters.adminUser}")
    private String adminUser;
    @Value("${keycloak.parameters.adminPassword}")
    private String adminPass;
    @Value("${keycloak.parameters.serverAddress}")
    private String keycloakAddress;
    private static final String grantType="password";
    private static final String clientId="admin_cli";
    private static final String tokenEndpointMasterRealm="/realms/master/protocol/openid-connect/token";

    @GetMapping
    public @ResponseBody ResponseEntity readiness() {
        String accessToken;
        ResponseEntity result = ResponseEntity.ok("Authorization Server Is Ready for Resources Server!");
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("Content-Type","application/x-www-form-urlencoded");
        String urlEncodedsString = System.out.printf("grant_type=%s&client_id=%s&username=%s&password=%s", grantType, clientId, adminUser, adminPass).toString();
        HttpEntity httpEntity = new HttpEntity(urlEncodedsString,httpHeaders);
        ResponseEntity<Map> response = restTemplate.exchange(keycloakAddress + tokenEndpointMasterRealm, HttpMethod.POST, httpEntity, Map.class);
        if (response.getStatusCode().value() == HttpStatus.OK.value())
        {
            Map body = response.getBody();
            accessToken = (String)body.get("access_token");
        }
        else
        {
            result = ResponseEntity.notFound().build();
        }
        return result;
    }


    @Autowired
    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
}
