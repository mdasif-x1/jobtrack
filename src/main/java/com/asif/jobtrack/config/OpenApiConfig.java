package com.asif.jobtrack.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI jobTrackOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("JobTrack API")
                        .description(
                                "REST API for managing job applications, "
                                        + "companies, search, filters, "
                                        + "upcoming actions, and status statistics."
                        )
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Md Asif")
                        )
                );
    }
}