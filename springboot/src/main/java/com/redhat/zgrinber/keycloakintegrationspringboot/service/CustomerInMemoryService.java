package com.redhat.zgrinber.keycloakintegrationspringboot.service;

import com.redhat.zgrinber.keycloakintegrationspringboot.model.Customer;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CustomerInMemoryService implements CustomerService {

    private static final Map<String,Customer> customers;
    static
    {
        customers = new HashMap();
        Customer customer = new Customer("demo", "name", "bank-cargo", 1, "banking", true);
        customers.put("demo",customer);
    }

    @Override
    public Customer getOneCustomer(String id) {
        return customers.get(id);
    }

    @Override
    public List<Customer> getAll() {
        return customers.values().stream().toList();
    }

    @Override
    public void createCustomer(Customer customer) {
        customers.put(customer.getId(),customer);
    }
}
