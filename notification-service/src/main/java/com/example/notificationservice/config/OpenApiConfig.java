package com.example.notificationservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI(
            @Value("${openapi.server.url}") String serverUrl,
            @Value("${openapi.server.description}") String serverDescription
    ) {
        return new OpenAPI()
                .info(new Info()
                        .title("Notification Service API")
                        .description("API для отправки сообщений на email")
                        .version("1.0.0"))
                .servers(List.of(
                        new Server()
                                .url(serverUrl)
                                .description(serverDescription)
                ));
    }
}
