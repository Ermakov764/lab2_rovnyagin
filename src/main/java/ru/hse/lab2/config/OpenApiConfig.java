package ru.hse.lab2.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI cinemaLabOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Cinema Lab2 API")
                        .version("1.0")
                        .description("REST API для фильмов, зрителей и билетов. Веб-страницы: /, /films/page, …"));
    }
}
