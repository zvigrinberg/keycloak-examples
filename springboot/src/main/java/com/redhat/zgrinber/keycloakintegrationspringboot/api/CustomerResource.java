package com.redhat.zgrinber.keycloakintegrationspringboot.api;

import com.redhat.zgrinber.keycloakintegrationspringboot.model.Customer;
import com.redhat.zgrinber.keycloakintegrationspringboot.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("customers")
@RequiredArgsConstructor
public class CustomerResource {

    private final CustomerService customerService;
    @GetMapping("/{id}")
    public Customer getCustomerService(@PathVariable String id)
    {
     return customerService.getOneCustomer(id);
    }

    @GetMapping
    public List<Customer> getAll()
    {
        return customerService.getAll();
    }

    @PostMapping

    public ResponseEntity createCustomer(@RequestBody Customer customer, @RequestHeader("host") String host)
    {
        customerService.createCustomer(customer);
        return ResponseEntity.created(URI.create("http://" + host + "/v1/api/customers/" + customer.getId())).body("Customer Created");
    }




}
