package io.kontur.eventapi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.Collections;

@Configuration
public class OpenApiConfiguration {

    private final Environment environment;

    public OpenApiConfiguration(Environment environment) {
        this.environment = environment;
    }

    @Bean
    public OpenAPI customOpenAPI() {
        Server server = new Server();
        server.setUrl("/events");

        OpenAPI openAPI = new OpenAPI().info(new Info()
                .title("Event API")
                .description("Disaster footprints & alerts"))
                .servers(Collections.singletonList(server));

        boolean isJwtAuthDisabledProfileActive = Arrays.asList(environment.getActiveProfiles()).contains("jwtAuthDisabled");

        if (!isJwtAuthDisabledProfileActive) {
            String securitySchemeName = "bearerAuth";

            openAPI.addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                    .components(new Components()
                            .addSecuritySchemes(securitySchemeName,
                                    new SecurityScheme()
                                            .name(securitySchemeName)
                                            .type(SecurityScheme.Type.HTTP)
                                            .scheme("bearer")
                                            .bearerFormat("JWT")));
        }

        return openAPI;
    }
}
