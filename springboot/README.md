# SpringBoot Keycloak demo


## Run keycloak instance in a container
```shell
podman run --name keycloak -d  -p 8080:8080 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin -e KC_HEALTH_ENABLED=true quay.io/keycloak/keycloak:21.1.1 start-dev
```

## Export all realm data including users
```shell
podman exec keycloak /opt/keycloak/bin/kc.sh export --dir /tmp --realm spring --users realm_file
```
Save to file:
```shell
 podman exec keycloak cat /tmp/spring-realm.json > keycloak-config/spring-realm.json
```

## Import realm to keycloak
If you have a full json with all realm configuration, you can import it to keycloak instance using the admin-cli client and admin user:
```shell
export TOKEN=$(curl -X POST http://localhost:8080/realms/master/protocol/openid-connect/token --data-urlencode 'grant_type=password' --data-urlencode 'client_id=admin-cli' --data-urlencode  'username=admin' --data-urlencode 'password=admin' --header 'Content-Type: application/x-www-form-urlencoded' | jq .access_token | tr -d '"')
curl -i -X POST http://localhost:8080/admin/realms --header 'Authorization: Bearer '$TOKEN'' --header 'Content-Type: application/json' -T keycloak-config/spring-realm.json
```

## Creating container image

### Using maven Spring boot plugin:
1. If you don't have docker daemon, and have podman or only docker CLI emulation through podman , you need to execute the following script before running the maven plugin to build the spring boot application image:
```shell
#!/bin/bash
systemctl --user enable podman.socket  --now
export DOCKER_HOST=unix:///run/user/${UID}/podman/podman.sock
export TESTCONTAINERS_RYUK_DISABLED=true
export GRAALVM_HOME=/home/zgrinber/bin/graalvm-ce-java11-19.3.6/


if [ -h "/var/run/docker.sock" ]; then
    echo "docker demon socket already exists, /var/run/docker.sock defined as a symbolic link"

else
   echo "creating docker.sock symbolic link to point to podman socket, please enter password:"
   sudo ln -s /run/user/${UID}/podman/podman.sock /var/run/docker.sock
fi

```

2. Run the maven plugin for building the image
```shell
mvn spring-boot:build-image
```

3. re-tag it according your registry and your account:
```shell
 podman tag docker.io/library/keycloak-integration-springboot:0.0.1-SNAPSHOT registry_address/your_user/keycloak-integration-springboot:tag
```

4. login to your account in the registry and push the image
```shell
podman login -u your_user registry_address 
```

5. push to registry:
```shell
podman push registry_address/your_user/keycloak-integration-springboot:tag
```

### Manually using Dockerfile (more Customized way)

1. Create this Dockerfile in current directory: 
```dockerfile

FROM docker.io/openjdk:20
USER root

RUN useradd -u 1001 appuser \
    && mkdir /java-app \
    && chown appuser /java-app

ARG app
COPY /target/*.jar /java-app/app.jar
RUN chmod -R ug+xrw /java-app
USER appuser
EXPOSE 8081 9090
WORKDIR /java-app


ENTRYPOINT ["java", "-jar", "app.jar"]
```

2. Build the image 
```shell
podman build -t  quay.io/zgrinber/keycloak-integration-springboot:1 .
```

3. Push the image to registry
```shell
podman push quay.io/zgrinber/keycloak-integration-springboot:1
```

## Spring Security And Spring boot Integration With Keycloak

### Defining PEP (Policy Enforcement point) using custom JAVA Filter

This piece of code is a filter that intercept all calls before they're arriving to controller, and it runs after all other authentication and authorization filters ( right after Authorization filter) of spring security already processed them.
AuthorizationFilter is not fully integrated with keycloak as authorization server as it only maps from user JWT the `Roles` of users as roles, and `Client Scopes` of user as Authorities , So permissions and their policies are not getting any representation in spring security.
We can use JwtAuthConverter to do custom mapping from user's JWT to `AbstractAuthenticationToken`, But user' JWT doesn't contain the permissions and authorization scopes, only roles and client scopes.
The kind of JWT token that is needed for obtaining the permissions is RPT token (Request Party Token), which is not supported by spring security.

hence `KeyCloakRptTokenFilter` filter is essential at the end of the filter chain, before flow passed (if approved) to controllers.


KeyCloakRptTokenFilter.java:
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class KeyCloakRptTokenFilter implements Filter {

    private static final Map<String, String> methodsToScopes;
    private static final String grantType = "urn:ietf:params:oauth:grant-type:uma-ticket";
    private final RestTemplate restTemplate;
    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:http://localhost:8080/realms/spring}")
    private String keycloakServerPortAndRealm;
    @Value("${keycloak.parameters.client-id}")
    private String jwtAudience;

    static {
        methodsToScopes = new HashMap<>();
        methodsToScopes.put("GET", "read");
        methodsToScopes.put("POST", "write");
        methodsToScopes.put("PUT", "update");
        methodsToScopes.put("DELETE", "delete");

    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
        String resourcePath = httpServletRequest.getServletPath().substring(1);
        String resourceName = "";

        String[] parts = resourcePath.split("v[1-9][0-9]*");
        //path doesn't contains api and version prefix
        if (parts.length == 1) {
            resourceName = parts[0];
        }
        //path contains api and version prefix
        else if (parts.length > 1) {
            resourceName = parts[1];
        }
        resourceName = resourceName.split("/")[0];

        //Actuator endpoints are not authenticated ,so we should skip the whole process for them).
        if (resourcePath.contains("actuator")) {
            filterChain.doFilter(servletRequest, servletResponse);
        } else {


            String method = httpServletRequest.getMethod();
            String authorization = httpServletRequest.getHeader("Authorization");
            String token = authorization.split(" ")[1];
            String scope = methodsToScopes.get(method);
            String fullAddress = keycloakServerPortAndRealm + "/protocol/openid-connect/token";
            String responseMode = "permissions";
            boolean authorizationGranted = false;
            boolean resourceExists;
            try {
                resourceExists = callKeycloakForRptCheckIfResourceExist(token, fullAddress, responseMode, resourceName);
                if (resourceExists) {
                    responseMode = "decision";
                    authorizationGranted = callKeycloakForAuthorizationDecision(token, fullAddress, responseMode, resourceName, scope);
                    if (authorizationGranted) {
                        filterChain.doFilter(servletRequest, servletResponse);
                    } else {
                        sendForbiddenResponse(httpServletResponse, resourceName);

                    }
                } else {
                    filterChain.doFilter(servletRequest, servletResponse);
                }
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode().value() == HttpStatus.FORBIDDEN.value()) {
                    sendForbiddenResponse(httpServletResponse, resourceName);
                }

            } catch (Exception e) {
                throw e;
            }
        }
    }
    //.....
    //.....
    //ommited most of the code as it's not relevant for understanding , only to implementation
}
```


```java
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity(debug = true)
@RequiredArgsConstructor
class SecurityConfig {

//    private final KeycloakLogoutHandler keycloakLogoutHandler;
    private final JwtAuthConverter jwtAuthConverter;
    @Qualifier("KeyCloakRptTokenFilter")
    private final KeyCloakRptTokenFilter myFilter;

    @Bean
    protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new RegisterSessionAuthenticationStrategy(new SessionRegistryImpl());
    }

    //Actuator Endpoints
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher(EndpointRequest.toAnyEndpoint());
        http.authorizeHttpRequests((requests) -> requests.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests()
                .requestMatchers("customers")
                .hasAnyRole("regular-user","admin")
                .requestMatchers("admin")
                .hasRole("admin")
                .anyRequest()
                .authenticated();

        http.addFilterAfter(myFilter, AuthorizationFilter.class);
        http.oauth2ResourceServer().jwt().jwtAuthenticationConverter(jwtAuthConverter);

        return http.build();
    }
}

```

### Readiness Probe
Defined Readiness Probe that will return success (200 HTTP Code) only if all keycloak configuration ( Roles, client scopes, permissions , Policies, and authorization scopes ) of configured keycloak instance are exists, otherwise, it will return Unavailable Service HTTP Code 503.
Define it as A spring-boot actuator endpoint
```java
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
    private List<KeycloakResource> keycloakResources;

    private List<String> customClientScopes;

    private List<String> customRoles;
    private static final String grantType = "password";
    private static final String clientId = "admin-cli";
    private static final String tokenEndpointMasterRealm = "/realms/master/protocol/openid-connect/token";
    private static final String tokenEndpointSpringRealm = "/realms/spring/protocol/openid-connect/token";

    private static final String getClientScopesEndpoint = "/admin/realms/spring/client-scopes";
    private static final String getClientsEndpoint = "/admin/realms/spring/clients";

    private static final String getClientsBaseEndpoint = "/admin/realms/spring/clients";


    @GetMapping
    public @ResponseBody ResponseEntity readiness() {
        boolean requiredPermissionsExists;
        boolean clientRolesExists;
        boolean customClientScopesExists;
        ReadinessPayload readinessPayload = new ReadinessPayload();
        String accessToken;
        ResponseEntity result;
        ResponseEntity<Map> response = getResponseFromTokenEndpoint(clientId, adminUser, adminPass, false);
        if (response.getStatusCode().value() == HttpStatus.OK.value()) {
            Map body = response.getBody();
            accessToken = (String) body.get("access_token");
            customClientScopesExists = checkClientScopesForRealm(accessToken, readinessPayload);
            clientRolesExists = checkClientRoles(accessToken, readinessPayload);
            response = getResponseFromTokenEndpoint(clientIdSpring, adminUserSpringRealm, adminPassSpringRealm, true);
            body = response.getBody();
            accessToken = (String) body.get("access_token");
            requiredPermissionsExists = checkForPermissions(accessToken, readinessPayload);

        } else {
            requiredPermissionsExists = false;
            clientRolesExists = false;
            customClientScopesExists = false;
        }
        if (requiredPermissionsExists && clientRolesExists) {
            readinessPayload.setStatus("Authorization Server Is Ready for Resources Server!");
//            "Authorization Server Is Ready for Resources Server!"
            result = ResponseEntity.ok(readinessPayload);
        } else {
            readinessPayload.setStatus("Keycloak is not Ready with all needed configuration");
            result = ResponseEntity.status(HttpStatusCode.valueOf(503)).body(readinessPayload);
        }
        return result;
    }
   //.....
   //.....
   //ommited most of the code as it's not relevant for understanding , only to implementation    
}
```

## Running Pod in Openshift, with keycloak as side-car container

1. We'll create configmap containing two files:

   a. spring-realm.json, which contain all exported needed data from keycloak to create Spring Realm.

   b. upload-realm-keycloak.sh, a script that will import to keycloak side-car container the realm using spring-realm.json file.  
 
2. the above configmap will be mounted as volume into application container.
3. using the script resides in mounted volume in application' container inside pod, i'll run a pod lifecycle postStart hook, to import to keycloak side-car container the spring realm using REST API POST endpoint
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  labels:
    app: springboot-keycloak-app
  name: springboot-keycloak-app
spec:
  replicas: 1
  selector:
    matchLabels:
      app: springboot-keycloak-app
  template:
    metadata:
      labels:
        app: springboot-keycloak-app
    spec:
      volumes:
        - name: realm-configmap
          configMap:
            name: spring-realm
            defaultMode: 0777
      containers:
      - image: quay.io/zgrinber/keycloak-integration-springboot:1
        imagePullPolicy: IfNotPresent
        name: keycloak-integration-springboot
        env:
          - name: KEYCLOAK_ADMIN
            value: admin
          - name: KEYCLOAK_ADMIN_PASSWORD
            value: admin
        ports:
          - containerPort: 8081
            name: http
          - containerPort: 9090
            name: management
        livenessProbe:
          httpGet:
            port: 9090
            path: /actuator/health
          initialDelaySeconds: 15
          periodSeconds: 10
          failureThreshold: 4
          successThreshold: 1
          timeoutSeconds: 2
        readinessProbe:
          httpGet:
            port: 9090
            path: /actuator/readiness
          initialDelaySeconds: 6
          periodSeconds: 4
          failureThreshold: 4
          successThreshold: 1
        volumeMounts:
          - mountPath: /tmp/scripts
            name: realm-configmap
        lifecycle:
          postStart:
            exec:
              command: ["bash","-c", "/tmp/scripts/upload-realm-keycloak.sh &"]

      - image: quay.io/keycloak/keycloak:21.1.1
        name: keycloak
        imagePullPolicy: IfNotPresent
        ports:
          - containerPort: 8080
            name: keycloak-http
        command:
          - "/opt/keycloak/bin/kc.sh"
        args:
          - start-dev
        env:
          - name: KEYCLOAK_ADMIN
            value: admin
          - name: KEYCLOAK_ADMIN_PASSWORD
            value: admin
          - name: KC_HEALTH_ENABLED
            value: "true"
          - name: KC_HOSTNAME
            value: localhost
          - name: KC_HOSTNAME_STRICT
            value: "false"
          - name: KC_HOSTNAME_PORT
            value: "8080"            

```

4. To automate all the above, we'll use a kustomization overlay, to create configmaps from realm json and script, and to deploy the pod with 2 containers on openshift cluster 
```shell
oc kustomize k8s/ | oc apply -f -
```
Output:
```shell
namespace/keycloak-spring created
configmap/spring-realm-85mght258c created
service/keycloak-app created
service/springboot-app created
deployment.apps/springboot-keycloak-app created
```

5. Track the progression of deployment:
```shell
oc project keycloak-spring
oc get pods -w
```
Output:
```shell
NAME                                      READY   STATUS    RESTARTS   AGE
springboot-keycloak-app-f7bb9fc65-87l4c   0/2     Pending   0          0s
springboot-keycloak-app-f7bb9fc65-87l4c   0/2     Pending   0          0s
springboot-keycloak-app-f7bb9fc65-87l4c   0/2     ContainerCreating    0s
springboot-keycloak-app-f7bb9fc65-87l4c   0/2     ContainerCreating    2s
springboot-keycloak-app-f7bb9fc65-87l4c   1/2     Running              3s
springboot-keycloak-app-f7bb9fc65-87l4c   2/2     Running              24s
```

6. Expose route of application service
```shell
oc expose svc/springboot-app
```
Output:
```shell
route.route.openshift.io/springboot-app exposed
```

7. Get Route http address for testing into Environment variable:
```shell
export SERVICE_URL=$(oc get route springboot-app -o=jsonpath="{.spec.host}")
echo $SERVICE_URL
```

Output:
```shell
springboot-app-keycloak-spring.apps.tem-lab01.fsi.rhecoeng.com
```

8. Try to get some item using `/items/{id}` Endpoint, without token.
```shell
curl -i -X GET http://$SERVICE_URL/items/something
```
Output:
```shell
HTTP/1.1 401 
set-cookie: JSESSIONID=FF8F60639E04FD325D4BE08488FB457C; Path=/; HttpOnly
www-authenticate: Bearer
x-content-type-options: nosniff
x-xss-protection: 0
cache-control: no-cache, no-store, max-age=0, must-revalidate
pragma: no-cache
expires: 0
x-frame-options: DENY
content-length: 0
date: Mon, 29 May 2023 11:22:53 GMT
set-cookie: 20166eafa2b6a12e0e8756143a6ce5a4=89824a3041da6b74fdff4017aad72582; path=/; HttpOnly
```

9. Expose route of keycloak service
```shell
oc expose svc/keycloak-app
```
10. Get route url address for retrieving tokens:
```shell
export KEYCLOAK_URL=$(oc get route keycloak-app -o=jsonpath="{.spec.host}")
```

11. Define in environment variables your client_id, and client_secret( if client is defined as Confidential):
```shell
export CLIENT_ID=spring-boot-app
export CLIENT_SECRET=ZjvIwT5SuQ3wrqwW1kst4vxhCtvgcKo7
```
12. Retrieve token for unprivileged user someone( password is also someone ):
```shell
export USER=someone
export PASSWORD=someone
export TOKEN=$(curl -X POST 'http://'${KEYCLOAK_URL}'/realms/spring/protocol/openid-connect/token' --user ''$CLIENT_ID':'$CLIENT_SECRET'' --header 'content-type: application/x-www-form-urlencoded' --data-urlencode 'username='${USER}''  --data-urlencode 'password='${PASSWORD}'' --data-urlencode 'grant_type=password' | jq .access_token | tr -d "\"")
```

13. Try to get some item using `/items/{id}` Endpoint, now with token of an unprivileged user:
```shell
curl -i -X GET http://$SERVICE_URL/items/demo-item --header 'Authorization: Bearer '$TOKEN''
```
Output:
```shell
HTTP/1.1 403 
x-content-type-options: nosniff
x-xss-protection: 0
cache-control: no-cache, no-store, max-age=0, must-revalidate
pragma: no-cache
expires: 0
x-frame-options: DENY
content-type: application/json
transfer-encoding: chunked
date: Tue, 30 May 2023 07:15:33 GMT
set-cookie: 20166eafa2b6a12e0e8756143a6ce5a4=69b290c4fb3b88aa56878b13862bbcfd; path=/; HttpOnly

{"message": " Keycloak Authorization Server blocked the access to the resource! " , "resource-name": "items"}
```

14. Retrieve token for privileged user my-user, which has permission to read/get items:
```shell
export USER=my-user
export PASSWORD=password
export TOKEN=$(curl -X POST 'http://'${KEYCLOAK_URL}'/realms/spring/protocol/openid-connect/token' --user ''$CLIENT_ID':'$CLIENT_SECRET'' --header 'content-type: application/x-www-form-urlencoded' --data-urlencode 'username='${USER}''  --data-urlencode 'password='${PASSWORD}'' --data-urlencode 'grant_type=password' | jq .access_token | tr -d "\"")
```

13. Try now to get the same item using `/items/{id}` Endpoint, with a token of my-user:
```shell
curl -i -X GET http://$SERVICE_URL/items/demo-item --header 'Authorization: Bearer '$TOKEN''
```
Output:
```shell
HTTP/1.1 200 
x-content-type-options: nosniff
x-xss-protection: 0
cache-control: no-cache, no-store, max-age=0, must-revalidate
pragma: no-cache
expires: 0
x-frame-options: DENY
content-type: application/json
transfer-encoding: chunked
date: Tue, 30 May 2023 07:18:37 GMT
set-cookie: 20166eafa2b6a12e0e8756143a6ce5a4=69b290c4fb3b88aa56878b13862bbcfd; path=/; HttpOnly

{"id":"demo-item","name":"Super Item","type":"unnatural","price":1000}
```

14. Create now new item using the same token (of user my-user) :
```shell
 curl -i -X POST http://$SERVICE_URL/items  -d '{"id": "demo-item2",  "name": "special" , "type": "cpu", "price": 750 }' --header 'Authorization: Bearer '$TOKEN''
```
Output:
```shell
HTTP/1.1 403 
x-content-type-options: nosniff
x-xss-protection: 0
cache-control: no-cache, no-store, max-age=0, must-revalidate
pragma: no-cache
expires: 0
x-frame-options: DENY
content-type: application/json
transfer-encoding: chunked
date: Fri, 02 Jun 2023 14:33:23 GMT
set-cookie: 20166eafa2b6a12e0e8756143a6ce5a4=69b290c4fb3b88aa56878b13862bbcfd; path=/; HttpOnly
```
Note: We got 403 Because user "my-user" is not authorized to create items ( doesn't have permission to create items).

15. Now Authenticate with administrator user in order to fetch from the token endpoint:
```shell
export USER=administrator
export PASSWORD=admin
export TOKEN=$(curl -X POST 'http://'${KEYCLOAK_URL}'/realms/spring/protocol/openid-connect/token' --user ''$CLIENT_ID':'$CLIENT_SECRET'' --header 'content-type: application/x-www-form-urlencoded' --data-urlencode 'username='${USER}''  --data-urlencode 'password='${PASSWORD}'' --data-urlencode 'grant_type=password' | jq .access_token | tr -d "\"")
```

16. Now using authorized token, Call the same POST call to create the item that my-user wasn't authorized to create:
```shell
curl -i -X POST http://$SERVICE_URL/items  -d '{"id": "demo-item2",  "name": "special" , "type": "cpu", "price": 750 }' --header 'Authorization: Bearer '$TOKEN'' -H 'Content-Type: application/json'
```
Output:
```shell
HTTP/1.1 201 
location: http://springboot-app-keycloak-spring.apps.tem-lab01.fsi.rhecoeng.com/items/demo-item2
x-content-type-options: nosniff
x-xss-protection: 0
cache-control: no-cache, no-store, max-age=0, must-revalidate
pragma: no-cache
expires: 0
x-frame-options: DENY
content-type: text/plain;charset=UTF-8
content-length: 12
date: Fri, 02 Jun 2023 14:43:01 GMT
set-cookie: 20166eafa2b6a12e0e8756143a6ce5a4=69b290c4fb3b88aa56878b13862bbcfd; path=/; HttpOnly
```

Note: We got 201 because administrator user has permission to create items (also to read, delete and update items ).

17. Using the token of the my-user again to get this new item from service:
```shell
export USER=my-user
export PASSWORD=password
export TOKEN=$(curl -X POST 'http://'${KEYCLOAK_URL}'/realms/spring/protocol/openid-connect/token' --user ''$CLIENT_ID':'$CLIENT_SECRET'' --header 'content-type: application/x-www-form-urlencoded' --data-urlencode 'username='${USER}''  --data-urlencode 'password='${PASSWORD}'' --data-urlencode 'grant_type=password' | jq .access_token | tr -d "\"")
curl -i -X GET http://$SERVICE_URL/items/demo-item2 --header 'Authorization: Bearer '$TOKEN''
```
Output:
```shell
HTTP/1.1 200 
x-content-type-options: nosniff
x-xss-protection: 0
cache-control: no-cache, no-store, max-age=0, must-revalidate
pragma: no-cache
expires: 0
x-frame-options: DENY
content-type: application/json
transfer-encoding: chunked
date: Fri, 02 Jun 2023 14:46:59 GMT
set-cookie: 20166eafa2b6a12e0e8756143a6ce5a4=69b290c4fb3b88aa56878b13862bbcfd; path=/; HttpOnly

{"id":"demo-item2","name":"special","type":"cpu","price":750}
```