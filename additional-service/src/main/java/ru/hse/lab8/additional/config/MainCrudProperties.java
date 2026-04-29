package ru.hse.lab8.additional.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "main.crud")
public class MainCrudProperties {

    /**
     * Базовый URL основного CRUD (контейнер/ВМ): без завершающего /
     */
    private String baseUrl = "http://localhost:8080";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim().replaceAll("/+$", "");
    }
}
