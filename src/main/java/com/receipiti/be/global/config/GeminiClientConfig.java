package com.receipiti.be.global.config;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeminiClientConfig {

    @Bean(destroyMethod = "close")
    public Client geminiClient(
            @Value("${gemini.api.key}") String apiKey,
            @Value("${gemini.http.timeout-millis:90000}") int timeoutMillis,
            @Value("${gemini.http.retry-attempts:3}") int retryAttempts
    ) {
        return Client.builder()
                .apiKey(apiKey)
                .httpOptions(HttpOptions.builder()
                        .timeout(timeoutMillis)
                        .retryOptions(HttpRetryOptions.builder()
                                // SDK의 attempts는 최초 요청을 포함한 전체 시도 횟수다.
                                .attempts(retryAttempts + 1)
                                .initialDelay(1.0)
                                .maxDelay(4.0)
                                .expBase(2.0)
                                .jitter(0.2)
                                .build())
                        .build())
                .build();
    }
}
