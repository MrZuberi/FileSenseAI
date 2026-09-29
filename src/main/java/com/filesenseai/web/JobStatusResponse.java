package com.filesenseai.web;

import com.filesenseai.job.SortJob;
import com.filesenseai.pipeline.SortResult;

public record JobStatusResponse(String status, String message, int current, int total, SortResult result, String errorMessage) {

    public static JobStatusResponse from(SortJob job) {
        return new JobStatusResponse(
                job.getStatus().name(),
                job.getMessage(),
                job.getCurrent(),
                job.getTotal(),
                job.getResult(),
                job.getErrorMessage()
        );
    }
}