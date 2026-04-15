package com.oj.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "judge0")
public class Judge0Properties {

    private String baseUrl = "http://139.9.114.118:2358";
    private int pollIntervalMs = 800;
    private int maxPollCount = 25;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getPollIntervalMs() {
        return pollIntervalMs;
    }

    public void setPollIntervalMs(int pollIntervalMs) {
        this.pollIntervalMs = pollIntervalMs;
    }

    public int getMaxPollCount() {
        return maxPollCount;
    }

    public void setMaxPollCount(int maxPollCount) {
        this.maxPollCount = maxPollCount;
    }
}