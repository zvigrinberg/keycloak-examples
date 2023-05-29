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
springboot-keycloak-app-f7bb9fc65-87l4c   0/2     ContainerCreating   0          0s
springboot-keycloak-app-f7bb9fc65-87l4c   0/2     ContainerCreating   0          2s
springboot-keycloak-app-f7bb9fc65-87l4c   1/2     Running             0          3s
springboot-keycloak-app-f7bb9fc65-87l4c   2/2     Running             0          24s
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
12. Retrieve token for user someone( password is also someone ):
```shell
export USER=my-user
export PASSWORD=password
export TOKEN=$(curl -X POST 'http://'${KEYCLOAK_URL}'/realms/spring/protocol/openid-connect/token' --user ''$CLIENT_ID':'$CLIENT_SECRET'' --header 'content-type: application/x-www-form-urlencoded' --data-urlencode 'username='${USER}''  --data-urlencode 'password='${PASSWORD}'' --data-urlencode 'grant_type=password' | jq .access_token | tr -d "\"")
```

13. Try to get some item using `/items/{id}` Endpoint, now with token of unprivileged token:
```shell
curl -i -X GET http://$SERVICE_URL/items/something --header 'Authorization: Bearer '$TOKEN''
```