package com.redhat.zgrinber.keycloakintegrationspringboot.service;

import com.redhat.zgrinber.keycloakintegrationspringboot.model.Customer;

import java.util.List;

public interface CustomerService {
    Customer getOneCustomer(String id);
    List<Customer> getAll();

    void createCustomer(Customer customer);
}
