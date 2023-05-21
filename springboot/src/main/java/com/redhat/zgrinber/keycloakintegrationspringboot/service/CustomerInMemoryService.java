package com.redhat.zgrinber.keycloakintegrationspringboot.service;

import com.redhat.zgrinber.keycloakintegrationspringboot.exception.EntityNotFoundException;
import com.redhat.zgrinber.keycloakintegrationspringboot.model.Customer;
import org.springframework.stereotype.Service;

import java.util.*;

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
    public Customer getOneCustomer(String id) throws EntityNotFoundException {
        return Optional.ofNullable(customers.get(id)).orElseThrow(() ->  new EntityNotFoundException("Customer with id=" + id + " wasn't found in DB" ,"Customer"));
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
