package com.redhat.zgrinber.keycloakintegrationspringboot.exception;


public class DuplicateEntityFoundException extends Exception{

    private String entityName;


    public DuplicateEntityFoundException(String message, String entityName) {
        super(message);
        this.entityName = entityName;

    }

    @Override
    public String toString() {
        return "DuplicateEntityFoundException{" +
                " entityName='" + entityName + '\'' +
                ", message='" + super.getMessage() + '\'' +
                '}';
    }
}
