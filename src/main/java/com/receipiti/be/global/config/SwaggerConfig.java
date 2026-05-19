package com.receipiti.be.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization");

        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("bearerAuth");
        return new OpenAPI()
                .info(new Info()
                        .title("Receipiti API")
                        .description("26-1 졸업프로젝트 Receipiti API 문서")
                        .version("v1.0.0"))
                .components(new Components().addSecuritySchemes("bearerAuth", securityScheme))  // 추가
                .addSecurityItem(securityRequirement);
    }
}