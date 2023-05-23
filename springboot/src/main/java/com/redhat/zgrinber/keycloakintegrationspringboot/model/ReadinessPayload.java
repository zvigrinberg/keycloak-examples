package com.redhat.zgrinber.keycloakintegrationspringboot.model;

import com.redhat.zgrinber.keycloakintegrationspringboot.keycloak.KeycloakResource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReadinessPayload {

    private String status;
    private Boolean allRolesExists;
    private List roles;
    private Boolean resourcesAndPermissionsExists;
    private List<KeycloakResource> permissions;
    private Boolean customClientScopesExist;
    private List customClientScopes;



}
