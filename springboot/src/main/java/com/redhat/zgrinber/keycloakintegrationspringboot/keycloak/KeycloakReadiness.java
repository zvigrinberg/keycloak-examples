package com.redhat.zgrinber.keycloakintegrationspringboot.keycloak;

import com.nimbusds.jose.util.Base64;
import com.redhat.zgrinber.keycloakintegrationspringboot.model.ReadinessPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RestControllerEndpoint(id = "readiness")
public class KeycloakReadiness {


    private RestTemplate restTemplate;
    @Value("${keycloak.parameters.adminUser}")
    private String adminUser;
    @Value("${keycloak.parameters.adminPassword}")
    private String adminPass;

    @Value("${keycloak.parameters.adminUserSpring}")
    private String adminUserSpringRealm;
    @Value("${keycloak.parameters.adminPasswordSpring}")
    private String adminPassSpringRealm;
    @Value("${keycloak.parameters.client-id}")
    private String clientIdSpring;
   @Value("${keycloak.parameters.client-password}")
    private String clientIdSpringPassword;

    @Value("${keycloak.parameters.serverAddress}")
    private String keycloakAddress;
//    @Qualifier("keycloakResources")
    private List<KeycloakResource> keycloakResources;

    private List<String> customClientScopes;

    private List<String> customRoles;
    private static final String grantType="password";
    private static final String clientId="admin-cli";
    private static final String tokenEndpointMasterRealm="/realms/master/protocol/openid-connect/token";
    private static final String tokenEndpointSpringRealm="/realms/spring/protocol/openid-connect/token";

    private static final String getClientScopesEndpoint= "/admin/realms/spring/client-scopes";
    private static final String getClientsEndpoint= "/admin/realms/spring/clients";

    private static final String getClientsBaseEndpoint= "/admin/realms/spring/clients";


//    @Autowired
//    private ApplicationContext context;


    @GetMapping
    public @ResponseBody ResponseEntity readiness() {
        boolean requiredPermissionsExists;
        boolean clientRolesExists;
        boolean customClientScopesExists;
        ReadinessPayload readinessPayload = new ReadinessPayload();
        String accessToken;
        ResponseEntity result;
        ResponseEntity<Map> response = getResponseFromTokenEndpoint(clientId, adminUser, adminPass,false);
        if (response.getStatusCode().value() == HttpStatus.OK.value())
        {
            Map body = response.getBody();
            accessToken = (String)body.get("access_token");
            customClientScopesExists = checkClientScopesForRealm(accessToken, readinessPayload);
            clientRolesExists = checkClientRoles(accessToken, readinessPayload);
            response = getResponseFromTokenEndpoint(clientIdSpring, adminUserSpringRealm, adminPassSpringRealm,true);
            body = response.getBody();
            accessToken = (String)body.get("access_token");
            requiredPermissionsExists = checkForPermissions(accessToken,readinessPayload);

        }
        else
        {
            requiredPermissionsExists=false;
            clientRolesExists=false;
            customClientScopesExists =false;
        }
        if(requiredPermissionsExists && clientRolesExists )
        {
            readinessPayload.setStatus("Authorization Server Is Ready for Resources Server!");
//            "Authorization Server Is Ready for Resources Server!"
            result = ResponseEntity.ok(readinessPayload);
        }
        else
        {
            readinessPayload.setStatus("Keycloak is not Ready with all needed configuration");
            result = ResponseEntity.status(HttpStatusCode.valueOf(503)).body(readinessPayload);
        }
        return result;
    }

    private  boolean checkForPermissions(String accessToken, ReadinessPayload readinessPayload) {

        boolean result=true;
        ResponseEntity<List> responseWithPermissions = getResponseFromSpringTokenEndpointPermissions(accessToken);
        List<Map<String,Object>> permissionsList = responseWithPermissions.getBody();
        for (KeycloakResource resource : this.keycloakResources) {
            Set scopes = (Set) permissionsList
                               .stream()
                               .filter(e->e.get("rsname")
                               .equals(resource.getName()))
                               .map(p -> ((Map)p)
                               .get("scopes"))
                               .collect(Collectors
                               .toList())
                               .stream()
                               .flatMap(list -> ((List)list)
                               .stream())
                               .collect(Collectors.toSet());
            for (String scope : resource.getScopes()) {
                if (!scopes.contains(scope)) {
                    result = false;
                }
            }
        }
        readinessPayload.setResourcesAndPermissionsExists(result);
        readinessPayload.setPermissions(this.keycloakResources);
        return result;
    }


    private boolean checkClientRoles(String accessToken, ReadinessPayload readinessPayload) {
          boolean result = true;
        String urlToBeInvoked = String.format("%s/%s", this.keycloakAddress, this.getClientsBaseEndpoint);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("Authorization",String.format("Bearer %s",accessToken));
        HttpEntity<Object> requestEntity = new HttpEntity<>(httpHeaders);
        ResponseEntity<List> clientLists = restTemplate.exchange(urlToBeInvoked, HttpMethod.GET, requestEntity,List.class);
        List<Map> clients = clientLists.getBody();
        List clientUid = clients.stream().filter(e -> e.get("clientId").equals(this.clientIdSpring)).map(e -> e.get("id")).collect(Collectors.toList());
        String clientUidValue = (String) clientUid.get(0);
//        Get roles for client.
        urlToBeInvoked = String.format("%s/%s/%s",urlToBeInvoked,clientUidValue,"roles");
        ResponseEntity<List> rolesList = restTemplate.exchange(urlToBeInvoked, HttpMethod.GET, requestEntity, List.class);
        List<Map> roles = rolesList.getBody();
        Set setOfRoles = roles.stream().map(pair -> pair.get("name")).collect(Collectors.toSet());

        for (String role : this.customRoles) {
             if (!setOfRoles.contains(role))
             {
                 result = false;
             }
        }
//        curl --location http://localhost:8080/admin/realms/spring/clients -H 'Authorization: Bearer '$TOKEN''
//        curl --location http://localhost:8080/admin/realms/spring/clients/${client-uid}/roles -H 'Authorization: Bearer '$TOKEN''  | jq .
        readinessPayload.setAllRolesExists(result);
        readinessPayload.setRoles(this.customRoles);
        return result;
    }

    private boolean checkClientScopesForRealm(String accessToken, ReadinessPayload readinessPayload) {
        boolean result=true;
        HttpHeaders httpHeaders = new HttpHeaders();
        String bearerToken = String.format("Bearer %s", accessToken);
        httpHeaders.set("Authorization",bearerToken);
        String address = String.format("%s/%s", this.keycloakAddress, this.getClientScopesEndpoint);
        HttpEntity httpEntity = new HttpEntity(httpHeaders);
        ResponseEntity<List> response = restTemplate.exchange(address, HttpMethod.GET, httpEntity, List.class);
        List<Map<String,Object>> clientScopesList = (List<Map<String,Object>>)response.getBody();
//        Set clientScopesSet = clientScopesList.stream().map(e -> e.entrySet().stream().filter(map -> map.getKey().equals("name")).collect(Collectors.toSet())).collect(Collectors.toSet());
//        Set clientScopesSet = clientScopesList.stream().map(e -> e.entrySet().stream().filter(p->p.getKey().equals("name")).map(y->y.getValue()).collect(Collectors.toList())).collect(Collectors.toSet())
        Set clientScopesSet = clientScopesList.stream().map(p->p.get("name")).collect(Collectors.toSet());
//        this.customClientScopes.forEach(scope -> );
        List<String> newList = new ArrayList();
        for (String scope : customClientScopes) {
            newList.add(scope);
            if (!clientScopesSet.contains(scope)) {
                result = false;

            }
        }
    readinessPayload.setCustomClientScopesExist(result);
    readinessPayload.setCustomClientScopes(newList);
    return result;

    }

    private ResponseEntity<Map> getResponseFromTokenEndpoint(String clientId, String adminUser, String adminPass,boolean clientBasicAuthentication) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("Content-Type","application/x-www-form-urlencoded");
        if (clientBasicAuthentication)
        {
            Base64 clientUserPasswordEncoded = Base64.encode(String.format("%s:%s", this.clientId, this.clientIdSpringPassword));
            httpHeaders.set("Authorization",String.format("Basic %s", clientUserPasswordEncoded));
        }
        String urlEncodedsString = String.format("grant_type=%s&client_id=%s&username=%s&password=%s", "password", clientId, adminUser, adminPass);
        HttpEntity httpEntity = new HttpEntity(urlEncodedsString,httpHeaders);
        String url;
        if(clientBasicAuthentication) {
            url = keycloakAddress + tokenEndpointSpringRealm;
        }
        else {
            url = keycloakAddress + tokenEndpointMasterRealm;
        }
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, httpEntity, Map.class);
        return response;
    }

    private ResponseEntity<List> getResponseFromSpringTokenEndpointPermissions(String accessToken) {
        HttpHeaders httpHeaders = new HttpHeaders();
        String bearerToken = String.format("Bearer %s", accessToken);
        httpHeaders.set("Authorization",bearerToken);
        httpHeaders.set("Content-Type","application/x-www-form-urlencoded");
        String urlEncodedsString = String.format("grant_type=%s&client_id=%s&client_secret=%s&username=%s&password=%s&audience=%s&response_mode=%s", "urn:ietf:params:oauth:grant-type:uma-ticket", clientIdSpring, clientIdSpringPassword , adminUser, adminPass,clientIdSpring,"permissions");
        HttpEntity httpEntity = new HttpEntity(urlEncodedsString,httpHeaders);
        ResponseEntity<List> response = restTemplate.exchange(keycloakAddress + tokenEndpointSpringRealm, HttpMethod.POST, httpEntity, List.class);
        return response;
    }


    @Autowired
    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Autowired
    public void setKeycloakResources(List<KeycloakResource> keycloakResources) {
        this.keycloakResources = keycloakResources;
    }

    @Autowired
    public void setCustomClientScopes(List<String> customClientScopes) {
        this.customClientScopes = customClientScopes;
    }
    @Autowired
    public void setCustomRoles(List<String> customRoles) {
        this.customRoles = customRoles;
    }
}
