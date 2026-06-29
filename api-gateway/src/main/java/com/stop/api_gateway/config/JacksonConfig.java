package com.stop.api_gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.ObjectProvider;
import com.fasterxml.jackson.databind.Module;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper(ObjectProvider<Module> modules) {

        ObjectMapper mapper = new ObjectMapper();

        modules.orderedStream().forEach(mapper::registerModule);

        return mapper;
    }
}