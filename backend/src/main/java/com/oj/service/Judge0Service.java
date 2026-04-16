package com.oj.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oj.config.Judge0Properties;
import com.oj.entity.Problem;
import com.oj.entity.Submission;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class Judge0Service {
    private static final Logger log = LoggerFactory.getLogger(Judge0Service.class);

    private final Judge0Properties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public Judge0Service(Judge0Properties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();
    }

    public JudgeResult judge(String language, String sourceCode, Problem problem) {
        try {
            Integer languageId = resolveLanguageId(language);
            if (languageId == null) {
                return JudgeResult.error("不支持的语言: " + language);
            }

            String baseUrl = normalizeBaseUrl(properties.getBaseUrl());
            Map<String, Object> payload = new HashMap<>();
            payload.put("source_code", sourceCode);
            payload.put("language_id", languageId);
            payload.put("stdin", defaultString(problem.getSampleInput()));
            payload.put("expected_output", defaultString(problem.getSampleOutput()));
            payload.put("cpu_time_limit", Math.max(1, problem.getTimeLimitMs()) / 1000.0);
            payload.put("memory_limit", Math.max(32, problem.getMemoryLimitMb()) * 1024);

            HttpRequest submitRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/submissions?base64_encoded=false&wait=false"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .timeout(Duration.ofSeconds(8))
                    .build();

            HttpResponse<String> submitResponse = httpClient.send(submitRequest, HttpResponse.BodyHandlers.ofString());
            if (submitResponse.statusCode() < 200 || submitResponse.statusCode() >= 300) {
                String bodySnippet = abbreviate(submitResponse.body(), 300);
                log.warn("Judge0 submit failed: status={}, body={}", submitResponse.statusCode(), bodySnippet);
                return JudgeResult.error("Judge0提交失败，HTTP " + submitResponse.statusCode()
                        + "，响应: " + bodySnippet);
            }

            JsonNode submitJson = objectMapper.readTree(submitResponse.body());
            String token = submitJson.path("token").asText();
            if (token.isBlank()) {
                return JudgeResult.error("Judge0返回token为空");
            }

            String queryUrl = baseUrl + "/submissions/" + token
                    + "?base64_encoded=false&fields=status,time,memory,stdout,stderr,compile_output,message";

            for (int i = 0; i < properties.getMaxPollCount(); i++) {
                Thread.sleep(properties.getPollIntervalMs());

                HttpRequest queryRequest = HttpRequest.newBuilder()
                        .uri(URI.create(queryUrl))
                        .GET()
                        .timeout(Duration.ofSeconds(8))
                        .build();
                HttpResponse<String> queryResponse = httpClient.send(queryRequest, HttpResponse.BodyHandlers.ofString());
                if (queryResponse.statusCode() < 200 || queryResponse.statusCode() >= 300) {
                    log.warn("Judge0 query failed: status={}, token={}, body={}",
                            queryResponse.statusCode(), token, abbreviate(queryResponse.body(), 300));
                    continue;
                }

                JsonNode result = objectMapper.readTree(queryResponse.body());
                int statusId = result.path("status").path("id").asInt(-1);
                String statusDescription = result.path("status").path("description").asText("UNKNOWN");
                if (statusId == 1 || statusId == 2) {
                    continue;
                }

                Submission.JudgeStatus mapped = mapStatus(statusId);
                Integer runtimeMs = parseRuntimeMs(result.path("time").asText(""));
                Integer memoryKb = result.path("memory").isNumber() ? result.path("memory").asInt() : null;
                String detail = pickFirstNonBlank(
                        result.path("compile_output").asText(""),
                        result.path("stderr").asText(""),
                        result.path("message").asText(""),
                        result.path("stdout").asText("")
                );

                return new JudgeResult(true, mapped, runtimeMs, memoryKb, token, detail, statusDescription);
            }

            return JudgeResult.error("Judge0轮询超时，未在限定时间内返回结果");
        } catch (Exception ex) {
            return JudgeResult.error("Judge0调用异常: " + ex.getMessage());
        }
    }

    private String normalizeBaseUrl(String url) {
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }

    private Integer resolveLanguageId(String language) {
        String normalized = language.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "c" -> 50;
            case "cpp", "c++" -> 54;
            default -> null;
        };
    }

    private Submission.JudgeStatus mapStatus(int statusId) {
        return switch (statusId) {
            case 3 -> Submission.JudgeStatus.AC;
            case 4 -> Submission.JudgeStatus.WA;
            case 5 -> Submission.JudgeStatus.TLE;
            case 6 -> Submission.JudgeStatus.CE;
            case 7, 8, 9, 10, 11, 12, 14 -> Submission.JudgeStatus.RE;
            case 13 -> Submission.JudgeStatus.SE;
            default -> Submission.JudgeStatus.SE;
        };
    }

    private Integer parseRuntimeMs(String secondsText) {
        if (secondsText == null || secondsText.isBlank()) {
            return null;
        }
        try {
            double seconds = Double.parseDouble(secondsText);
            return (int) Math.round(seconds * 1000.0);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String defaultString(String text) {
        return text == null ? "" : text;
    }

    private String pickFirstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.length() > 800 ? value.substring(0, 800) : value;
            }
        }
        return "";
    }

    private String abbreviate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    public record JudgeResult(
            boolean success,
            Submission.JudgeStatus judgeStatus,
            Integer runtimeMs,
            Integer memoryKb,
            String token,
            String detail,
            String rawStatus
    ) {
        public static JudgeResult error(String detail) {
            return new JudgeResult(false, Submission.JudgeStatus.SE, null, null, "", detail, "SYSTEM_ERROR");
        }
    }
}