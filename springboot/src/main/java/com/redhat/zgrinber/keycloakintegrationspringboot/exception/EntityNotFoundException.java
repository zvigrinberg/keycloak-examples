package com.redhat.zgrinber.keycloakintegrationspringboot.exception;

import lombok.AllArgsConstructor;
import lombok.ToString;


public class EntityNotFoundException extends Exception{

    private String entityName;


    public EntityNotFoundException(String message, String entityName) {
        super(message);
        this.entityName = entityName;

    }

    @Override
    public String toString() {
        return "EntityNotFoundException{" +
                " entityName='" + entityName + '\'' +
                ", message='" + super.getMessage() + '\'' +
                '}';
    }
}
