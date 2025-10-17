package com.equinor.onlypikks.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI onlyPikksOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("OnlyPikks API")
                        .description("API documentation for the OnlyPikks backend")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("OnlyPikks Team"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .externalDocs(new ExternalDocumentation()
                        .description("Project Repository")
                        .url("https://github.com/equinor"));
    }
}
