package com.attendx.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI attendxOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AttendX API")
                        .description("Student Attendance Management System REST API")
                        .version("1.0.0")
                        .contact(new Contact().name("AttendX Team")));
    }
}
