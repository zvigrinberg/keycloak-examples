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