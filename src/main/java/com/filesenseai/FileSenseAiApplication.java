package com.filesenseai;

import com.filesenseai.config.AppProperties;
import com.filesenseai.tui.TerminalUI;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class FileSenseAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(FileSenseAiApplication.class, args);
    }

    @Bean
    public CommandLineRunner run(TerminalUI terminalUI) {
        return args -> terminalUI.start();
    }
}