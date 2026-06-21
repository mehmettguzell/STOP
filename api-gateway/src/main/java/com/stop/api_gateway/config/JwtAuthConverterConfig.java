package com.stop.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.util.List;

@Configuration
public class JwtAuthConverterConfig {

    @Bean
    @Primary
    public JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String role = jwt.getClaimAsString("role");
            String roleName = (role != null && !role.isBlank()) ? role : "USER";
            return List.of(new SimpleGrantedAuthority("ROLE_" + roleName));
        });

        converter.setPrincipalClaimName("sub");

        return converter;
    }
}
