package com.redhat.zgrinber.keycloakintegrationspringboot.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Item {

    private String id;
    private String name;
    private String type;
    private Integer price;
}
