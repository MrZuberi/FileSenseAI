package com.filesenseai.config;

import com.filesenseai.ai.CohereClient;
import com.filesenseai.backup.S3BackupService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

    @Bean
    public CohereClient cohereClient(AppProperties appProperties) {
        return new CohereClient(appProperties.getCohereApiKey());
    }

    @Bean
    public S3BackupService s3BackupService(AppProperties appProperties) {
        return new S3BackupService(appProperties.getAwsS3Bucket(), appProperties.getAwsRegion());
    }
}