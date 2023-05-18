package com.redhat.zgrinber.keycloakintegrationspringboot.filter;

import com.nimbusds.jose.shaded.gson.Gson;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


// This filter only blocks access for resources defined in keycloak that doesn't have the right permission for accessing the resource.
// resources that not defined in keycloak are continuing with the filter chain until the controller ( if not blocked before this filter by Spring security by role or client scope).
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
        //Actuator endpoints are not authenticated ,so we should skip the whole process for them).
        if(resourceName.contains("actuator"))
        {
            filterChain.doFilter(servletRequest, servletResponse);
        }
        else {



            String method = httpServletRequest.getMethod();
            String authorization = httpServletRequest.getHeader("Authorization");
            String token = authorization.split(" ")[1];
            String scope = methodsToScopes.get(method);
            String fullAddress = keycloakServerPortAndRealm + "/protocol/openid-connect/token";
            String responseMode = "permissions";

            boolean resourceExists = callKeycloakForRptCheckIfResourceExist(token, fullAddress, responseMode, resourceName);
            boolean authorizationGranted = false;
            if (resourceExists) {
                resourceName = resourceName.replace('/', ' ').trim();
                responseMode = "decision";
                authorizationGranted = callKeycloakForAuthorizationDecision(token, fullAddress, responseMode, resourceName, scope);
                if (authorizationGranted) {
                    filterChain.doFilter(servletRequest, servletResponse);
                } else {
                    httpServletResponse.resetBuffer();
                    httpServletResponse.setStatus(HttpStatus.FORBIDDEN.value());
                    httpServletResponse.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
                    String jsonResponse = "{\"message\": \" Keycloak Authorization Server blocked the access to the resource! \" , \"resource-name\": \"placeholder\"}";
                    httpServletResponse.getOutputStream().print(jsonResponse.replace("placeholder", resourceName));
                    httpServletResponse.flushBuffer();

                }
            } else {
                filterChain.doFilter(servletRequest, servletResponse);
            }
        }
    }

    private boolean callKeycloakForAuthorizationDecision(String token, String fullAddress, String responseMode, String resourceName, String scope) {
        boolean result;
        String permission = resourceName + "#" + scope;
        ResponseEntity<String> response;
        try {
            response = callKeycloakForRPTToken(token, fullAddress, responseMode, permission);
            if (response.getStatusCode() == HttpStatusCode.valueOf(200)) {
                String body = response.getBody();
                Gson gson = new Gson();
                Map decision = gson.fromJson(body, Map.class);
                if ((boolean) decision.get("result")) {
                    result = true;
                } else {
                    result = false;
                }
            } else {
                result = false;
            }
        }

        catch(HttpClientErrorException e)
        {
            //Only if forbidden then block access, if another error from keycloak, don't block, and just print stack strace
            result = true;

            if (e.getStatusCode().value() == HttpStatus.FORBIDDEN.value())
            {
                result = false;
            }

            else
            {
                System.out.printf("Calling keycloak for authorization decision failed, with the following message, skipping the filter -  %s", e.getMessage());
                e.printStackTrace();
            }

        }
    return result;
    }

    private boolean callKeycloakForRptCheckIfResourceExist(String token, String fullAddress, String responseMode, String resourceName) {
        boolean result=false;
        ResponseEntity<String> response = callKeycloakForRPTToken(token, fullAddress, responseMode,null);
        String body = response.getBody();
        Gson gson = new Gson();
        List list = gson.fromJson(body, List.class);
        HttpHeaders headers = response.getHeaders();
//        final boolean[] result = new boolean[1];
//        if (response.getStatusCode() ==HttpStatusCode.valueOf(200))
//        {
//            list.forEach(resource -> {
//                 Map<String,String> map = (Map)resource;
//                 result[0] = resourceName.contains(map.get("rsname")) ;
//
//                 });
//        }
        boolean finished = false;
        Iterator iterator = list.iterator();
        while(iterator.hasNext() && !finished)
        {
            Map<String,String> next =(Map) iterator.next();
             if (resourceName.contains(next.get("rsname")))
             {
                 finished = true;
                 result = true;
             }
        }
     return result;
    }

    private ResponseEntity<String> callKeycloakForRPTToken(String token, String fullAddress, String responseMode,String permission) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        httpHeaders.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED.toString());
        String urlEncodedBody = "grant_type=" + grantType + "&audience=" + jwtAudience + "&response_mode=" + responseMode;
        if (permission != null)
        {
            urlEncodedBody = urlEncodedBody + "&permission=" + permission;
        }
        HttpEntity entity = new HttpEntity(urlEncodedBody ,httpHeaders);
        ResponseEntity<String> response = restTemplate.exchange(fullAddress, HttpMethod.POST, entity, String.class);
        return response;
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
