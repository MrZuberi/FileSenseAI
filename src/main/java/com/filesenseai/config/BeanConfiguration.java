package com.filesenseai.config;

import com.filesenseai.ai.CohereClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

    @Bean
    public CohereClient cohereClient(AppProperties appProperties) {
        return new CohereClient(appProperties.getCohereApiKey());
    }
}