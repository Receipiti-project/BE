package com.receipiti.be.global.config;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeminiClientConfig {

    private static final int REQUEST_TIMEOUT_MILLIS = 30_000;

    @Bean(destroyMethod = "close")
    public Client geminiClient(@Value("${gemini.api.key}") String apiKey) {
        return Client.builder()
                .apiKey(apiKey)
                .httpOptions(HttpOptions.builder()
                        .timeout(REQUEST_TIMEOUT_MILLIS)
                        .build())
                .build();
    }
}
