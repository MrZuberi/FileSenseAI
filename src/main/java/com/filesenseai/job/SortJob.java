package com.filesenseai.job;

import com.filesenseai.pipeline.SortResult;

public class SortJob {

    public enum Status { RUNNING, DONE, ERROR }

    private volatile Status status = Status.RUNNING;
    private volatile String message = "Starting";
    private volatile int current = 0;
    private volatile int total = 0;
    private volatile SortResult result;
    private volatile String errorMessage;

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getCurrent() {
        return current;
    }

    public void setCurrent(int current) {
        this.current = current;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public SortResult getResult() {
        return result;
    }

    public void setResult(SortResult result) {
        this.result = result;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}