package com.stop.notification_service.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_JWT = "bearer-jwt";

    @Bean
    OpenApiCustomizer attachBearerJwtToEveryOperation() {
        return openApi -> {
            if (openApi.getPaths() == null) return;
            openApi.getPaths().values().forEach(pathItem -> {
                if (pathItem == null) return;
                pathItem.readOperations().forEach(operation -> {
                    if (operation == null) return;
                    boolean hasBearer = operation.getSecurity() != null
                            && operation.getSecurity().stream()
                            .anyMatch(req -> req.containsKey(BEARER_JWT));
                    if (!hasBearer) {
                        operation.addSecurityItem(new SecurityRequirement().addList(BEARER_JWT));
                    }
                });
            });
        };
    }

    @Bean
    OpenAPI notificationServiceOpenAPI(
            @Value("${spring.application.name:notification-service}") String applicationName
    ) {
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList(BEARER_JWT))
                .info(new Info()
                        .title(applicationName)
                        .description("""
                                Bildirim API (notification-service).

                                **Swagger:** Authorize → `bearer-jwt` alanına **yalnızca access token (ham JWT)** yapıştırın
                                (`Bearer ` önekini yazmayın; UI ekler). Token identity-service üzerinden alınır.
                                """)
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_JWT, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("identity-service login veya /auth/refresh ile dönen access token")));
    }
}
