package com.filesenseai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "filesenseai")
public class AppProperties {

    private String cohereApiKey;
    private String awsS3Bucket;
    private String awsRegion = "us-east-1";

    public String getCohereApiKey() {
        return cohereApiKey;
    }

    public void setCohereApiKey(String cohereApiKey) {
        this.cohereApiKey = cohereApiKey;
    }

    public String getAwsS3Bucket() {
        return awsS3Bucket;
    }

    public void setAwsS3Bucket(String awsS3Bucket) {
        this.awsS3Bucket = awsS3Bucket;
    }

    public String getAwsRegion() {
        return awsRegion;
    }

    public void setAwsRegion(String awsRegion) {
        this.awsRegion = awsRegion;
    }
}