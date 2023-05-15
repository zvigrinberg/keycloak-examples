package com.redhat.zgrinber.keycloakintegrationspringboot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

public class VariousBeans {



@Bean
public RestTemplate restTemplate()
{
    return new RestTemplate();
}

}
