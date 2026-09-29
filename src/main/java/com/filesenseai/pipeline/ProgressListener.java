package com.filesenseai.pipeline;

public interface ProgressListener {
    void onProgress(String message, int current, int total);
}