package com.redhat.zgrinber.keycloakintegrationspringboot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class Customer {

    private String id;
    private String name;
    private String company;
    private Integer grade;
    private String companySector;
    private Boolean payingCustomer;


}
