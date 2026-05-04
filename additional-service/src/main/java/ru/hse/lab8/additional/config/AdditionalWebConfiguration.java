package ru.hse.lab8.additional.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * {@link RestTemplate} для {@link ru.hse.lab8.additional.client.CinemaCrudClient}; Jackson берётся из общего бина {@link ObjectMapper}.
 *
 * <p>После полного перехода фреймворка на {@code JacksonJsonHttpMessageConverter} можно заменить конвертер и снять {@code SuppressWarnings}.
 */
@Configuration
@EnableConfigurationProperties(MainCrudProperties.class)
@SuppressWarnings("removal")
public class AdditionalWebConfiguration {

    private static final int CONNECT_TIMEOUT_MS = (int) Duration.ofSeconds(5).toMillis();
    private static final int READ_TIMEOUT_MS = (int) Duration.ofSeconds(10).toMillis();

    @Bean
    public RestTemplate mainCrudRestTemplate(ObjectMapper objectMapper) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        RestTemplate restTemplate = new RestTemplate(factory);
        for (int i = 0; i < restTemplate.getMessageConverters().size(); i++) {
            if (restTemplate.getMessageConverters().get(i) instanceof MappingJackson2HttpMessageConverter) {
                restTemplate.getMessageConverters().set(i, new MappingJackson2HttpMessageConverter(objectMapper));
                break;
            }
        }
        return restTemplate;
    }
}
