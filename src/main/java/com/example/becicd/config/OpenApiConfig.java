package com.example.becicd.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI()
                .info(new Info()
                        .title("Quản lý sân bóng")
                        .description("Demo Spring Boot API for CI/CD (Product CRUD)")
                        .version("v1.0.0")
                        .contact(new Contact().name("Demo Team"))
                        .license(new License().name("MIT")));
    }
}
