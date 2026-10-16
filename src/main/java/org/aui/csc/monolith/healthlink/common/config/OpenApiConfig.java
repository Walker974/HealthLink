package org.aui.csc.monolith.healthlink.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI healthLinkOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("HealthLink API")
                        .version("0.0.1-SNAPSHOT")
                        .description("Public imaging availability and authorized hospital management. "
                                + "Citizen searches and organization reference reads are public. "
                                + "Protected operations require a Keycloak access token and the documented role; "
                                + "hospital and department boundaries are enforced by the services."))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste a Keycloak access token from the healthlink realm, "
                                        + "without the Bearer prefix. The token must target healthlink-api. "
                                        + "The local realm issues tokens valid for five minutes."))
                        .addResponses("BadRequest", new ApiResponse()
                                .description("Invalid request body, parameters, pagination or staff assignment"))
                        .addResponses("Unauthorized", new ApiResponse()
                                .description("Missing, expired or invalid bearer access token"))
                        .addResponses("Forbidden", new ApiResponse()
                                .description("Insufficient role or missing, inactive or mismatched staff assignment"))
                        .addResponses("NotFound", new ApiResponse()
                                .description("A referenced resource does not exist or is inactive where required"))
                        .addResponses("Conflict", new ApiResponse()
                                .description("A unique value already exists or a database constraint was violated")));
    }
}
