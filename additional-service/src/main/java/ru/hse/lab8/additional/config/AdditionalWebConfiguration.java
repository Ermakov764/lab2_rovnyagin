package ru.hse.lab8.additional.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * {@link RestTemplate} для {@link ru.hse.lab8.additional.client.CinemaCrudClient}.
 * Используем стандартные конвертеры RestTemplate без обязательной зависимости от внешнего бина ObjectMapper.
 */
@Configuration
@EnableConfigurationProperties(MainCrudProperties.class)
public class AdditionalWebConfiguration {

    private static final int CONNECT_TIMEOUT_MS = (int) Duration.ofSeconds(5).toMillis();
    private static final int READ_TIMEOUT_MS = (int) Duration.ofSeconds(10).toMillis();

    @Bean
    public RestTemplate mainCrudRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        return new RestTemplate(factory);
    }
}
