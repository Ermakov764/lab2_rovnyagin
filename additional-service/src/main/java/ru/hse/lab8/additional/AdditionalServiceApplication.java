package ru.hse.lab8.additional;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AdditionalServiceApplication {

    /**
     * Запуск Spring Boot: загружает {@code application.properties}, поднимает веб-контейнер (порт см. конфиг).
     */
    public static void main(String[] args) {
        SpringApplication.run(AdditionalServiceApplication.class, args);
    }
}
