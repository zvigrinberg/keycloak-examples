package com.redhat.zgrinber.keycloakintegrationspringboot.config;

import com.redhat.zgrinber.keycloakintegrationspringboot.filter.KeyCloakRptTokenFilter;
import com.redhat.zgrinber.keycloakintegrationspringboot.keycloak.KeycloakLogoutHandler;
import jakarta.servlet.Filter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;

import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;


@Configuration
@EnableWebSecurity(debug = true)
@RequiredArgsConstructor
class SecurityConfig {

//    private final KeycloakLogoutHandler keycloakLogoutHandler;
    private final JwtAuthConverter jwtAuthConverter;
    @Qualifier("KeyCloakRptTokenFilter")
    private final KeyCloakRptTokenFilter myFilter;

    @Bean
    protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new RegisterSessionAuthenticationStrategy(new SessionRegistryImpl());
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests()
                .requestMatchers("customers")
                .hasAnyRole("regular-user","admin")
                .requestMatchers("admin")
                .hasRole("admin")
//                .requestMatchers("items")
//                .hasAuthority("SCOPE_manage_items")
                .anyRequest()
                .authenticated();
//        http.oauth2Login()
//                .and()
//                .logout()
//                .addLogoutHandler(keycloakLogoutHandler)
//                .logoutSuccessUrl("/");
        http.addFilterAfter(myFilter, AuthorizationFilter.class);
        http.oauth2ResourceServer().jwt().jwtAuthenticationConverter(jwtAuthConverter);
        return http.build();
    }
}
