package com.tripvisito.tripservice.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * RestTemplate configuration for external HTTP API calls.
 * Used by {@link com.tripvisito.tripservice.service.GeminiService}
 * and {@link com.tripvisito.tripservice.service.UnsplashService}.
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                // Timeout for connecting to Gemini / Unsplash (generous for AI response)
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(90))
                .build();
    }
}
