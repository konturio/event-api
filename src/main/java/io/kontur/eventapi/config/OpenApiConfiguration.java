package io.kontur.eventapi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.oauth2.OAuthFlow;
import io.swagger.v3.oas.models.security.oauth2.OAuthFlows;
import io.swagger.v3.oas.models.security.oauth2.Scopes;
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
            String issuerUri = environment.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri");

            if (issuerUri != null) {
                String authUrl = issuerUri + "/protocol/openid-connect/auth";
                String tokenUrl = issuerUri + "/protocol/openid-connect/token";

                SecurityScheme securityScheme = new SecurityScheme()
                        .name(securitySchemeName)
                        .type(SecurityScheme.Type.OAUTH2)
                        .flows(new OAuthFlows()
                                .authorizationCode(new OAuthFlow()
                                        .authorizationUrl(authUrl)
                                        .tokenUrl(tokenUrl)
                                        .scopes(new Scopes())));

                openAPI.addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                        .components(new Components().addSecuritySchemes(securitySchemeName, securityScheme));
            } else {
                openAPI.addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                        .components(new Components()
                                .addSecuritySchemes(securitySchemeName,
                                        new SecurityScheme()
                                                .name(securitySchemeName)
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")));
            }
        }

        return openAPI;
    }
}
