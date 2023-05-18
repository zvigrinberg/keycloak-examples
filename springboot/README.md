# SpringBoot Keycloak demo


## Run keycloak instance in a container
```shell
podman run --name keycloak -d  -p 8080:8080 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin quay.io/keycloak/keycloak:21.1.1 start-dev
```

## Export all realm data including users
```shell
podman exec keycloak /opt/keycloak/bin/kc.sh export --dir /tmp --realm spring --users realm_file
```