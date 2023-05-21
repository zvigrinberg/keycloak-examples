package com.redhat.zgrinber.keycloakintegrationspringboot.api;

import com.redhat.zgrinber.keycloakintegrationspringboot.exception.EntityNotFoundException;
import com.redhat.zgrinber.keycloakintegrationspringboot.model.Customer;
import com.redhat.zgrinber.keycloakintegrationspringboot.service.CustomerService;
import lombok.RequiredArgsConstructor;
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
    public ResponseEntity<Customer> getCustomerService(@PathVariable String id)
    {
        Customer oneCustomer = null;
        ResponseEntity response;
        try {
            oneCustomer = customerService.getOneCustomer(id);
            response = ResponseEntity.ok(oneCustomer);
        } catch (EntityNotFoundException e) {
            response = ResponseEntity.notFound().header("errorMessage",e.toString()).build();
        }
        return response;
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
        return ResponseEntity.created(URI.create("http://" + host + "/customers/" + customer.getId())).body("Customer Created");
    }




}
