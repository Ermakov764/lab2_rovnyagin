package ru.hse.lab8.additional.observability;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ObservabilityWebMvcConfig implements WebMvcConfigurer {

    private final ObservabilityService observabilityService;

    public ObservabilityWebMvcConfig(ObservabilityService observabilityService) {
        this.observabilityService = observabilityService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new ObservabilityHttpInterceptor(observabilityService))
                .addPathPatterns("/api/**");
    }
}
