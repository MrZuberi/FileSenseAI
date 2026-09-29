package com.filesenseai;

import com.filesenseai.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class FileSenseAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(FileSenseAiApplication.class, args);
    }
}