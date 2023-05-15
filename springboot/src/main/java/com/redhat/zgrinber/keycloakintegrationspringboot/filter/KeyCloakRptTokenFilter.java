package com.redhat.zgrinber.keycloakintegrationspringboot.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class KeyCloakRptTokenFilter implements Filter {

    private static final Map<String,String> methodsToScopes;
    private static final String grantType = "urn:ietf:params:oauth:grant-type:uma-ticket";
    private final RestTemplate restTemplate;
    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:http://localhost:8080/realms/spring}")
    private String keycloakServerPortAndRealm;
    @Value("${keycloak.parameters.client-id}")
    private String jwtAudience;

    static
    {
        methodsToScopes = new HashMap<>();
        methodsToScopes.put("GET","read");
        methodsToScopes.put("POST","write");
        methodsToScopes.put("PUT","update");
        methodsToScopes.put("DELETE","delete");

    }
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;

        String resourceName = httpServletRequest.getServletPath().substring(1);
        String method = httpServletRequest.getMethod();
        String authorization = httpServletRequest.getHeader("Authorization");
        String token = authorization.split(" ")[1];
//        httpServletResponse.getOutputStream().wr
        filterChain.doFilter(servletRequest, servletResponse);

    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
