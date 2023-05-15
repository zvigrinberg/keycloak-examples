package com.redhat.zgrinber.keycloakintegrationspringboot.api;

import com.redhat.zgrinber.keycloakintegrationspringboot.model.Customer;
import com.redhat.zgrinber.keycloakintegrationspringboot.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("admin")
@RequiredArgsConstructor
public class AdminResource {
    @GetMapping
    public String getAdminData()
    {
        return "admin access granted!";
    }







}
