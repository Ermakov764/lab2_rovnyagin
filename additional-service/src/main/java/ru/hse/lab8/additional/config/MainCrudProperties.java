package ru.hse.lab8.additional.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Базовый URL основного CRUD; при установке удаляются пробелы по краям и завершающие слэши.
 */
@ConfigurationProperties(prefix = "main.crud")
public class MainCrudProperties {

    private String baseUrl = "http://localhost:8080";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim().replaceAll("/+$", "");
    }
}
