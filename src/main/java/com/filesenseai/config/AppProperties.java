package com.filesenseai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "filesenseai")
public class AppProperties {

    private String cohereApiKey;

    public String getCohereApiKey() {
        return cohereApiKey;
    }

    public void setCohereApiKey(String cohereApiKey) {
        this.cohereApiKey = cohereApiKey;
    }
}   