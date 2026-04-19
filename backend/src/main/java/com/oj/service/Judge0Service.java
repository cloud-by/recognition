package com.oj.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oj.config.Judge0Properties;
import com.oj.entity.Problem;
import com.oj.entity.Submission;
import com.oj.entity.SubmissionTestPoint;
import com.oj.repository.SubmissionTestPointRepository;
import com.oj.tools.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class Judge0Service {
    private static final Logger log = LoggerFactory.getLogger(Judge0Service.class);

    private final Judge0Properties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final SubmissionTestPointRepository submissionTestPointRepository;

    public Judge0Service(Judge0Properties properties, ObjectMapper objectMapper, SubmissionTestPointRepository submissionTestPointRepository) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.submissionTestPointRepository = submissionTestPointRepository;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(6))
                .build();
    }

    public JudgeResult judge(String language, String sourceCode, Problem problem, Submission submission) {
        try {
            Integer languageId = resolveLanguageId(language);
            if (languageId == null) {
                return JudgeResult.error("不支持的语言: " + language);
            }

            // 1. 读取所有测试数据
            FileUtil.TestData testData = FileUtil.readInOutFiles("problem/" + problem.getId());
            if (testData == null || testData.getSize() == 0) {
                return JudgeResult.error("测试数据读取失败或为空");
            }

            int testCount = testData.getSize();
            log.info("开始批量判题，共{}个测试用例", testCount);

            // 2. 构建批量提交请求体
            List<Map<String, Object>> submissionsList = new ArrayList<>();

            for (int i = 0; i < testCount; i++) {
                String input = testData.getInput(i);
                String expectedOutput = testData.getOutput(i);

                Map<String, Object> submissionMap = new HashMap<>();
                submissionMap.put("language_id", languageId);
                submissionMap.put("source_code", sourceCode);
                submissionMap.put("stdin", input == null ? "" : input);
                submissionMap.put("expected_output", expectedOutput == null ? "" : expectedOutput);
                submissionMap.put("cpu_time_limit", Math.max(1, problem.getTimeLimitMs()) / 1000.0);
                submissionMap.put("memory_limit", Math.max(32, problem.getMemoryLimitMb()) * 1024);

                submissionsList.add(submissionMap);
            }

            // 包装在submissions字段中
            Map<String, Object> batchRequest = new HashMap<>();
            batchRequest.put("submissions", submissionsList);

            String baseUrl = normalizeBaseUrl(properties.getBaseUrl());

            // 使用异步模式，wait=false，只获取tokens
            String batchSubmitUrl = baseUrl + "/submissions/batch?base64_encoded=false&wait=false";

            // 3. 发送批量请求，获取tokens
            String batchRequestBody = objectMapper.writeValueAsString(batchRequest);
            log.info("发送批量判题请求，获取tokens...");

            HttpRequest submitRequest = HttpRequest.newBuilder()
                    .uri(URI.create(batchSubmitUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(batchRequestBody))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> submitResponse = httpClient.send(submitRequest, HttpResponse.BodyHandlers.ofString());

            log.info("响应状态码: {}", submitResponse.statusCode());
            log.info("响应体: {}", submitResponse.body());

            if (submitResponse.statusCode() != 200 && submitResponse.statusCode() != 201) {
                return JudgeResult.error("Judge0批量提交失败，HTTP " + submitResponse.statusCode());
            }

            // 4. 解析tokens
            JsonNode responseNode = objectMapper.readTree(submitResponse.body());
            if (!responseNode.isArray()) {
                return JudgeResult.error("返回格式错误: " + submitResponse.body());
            }

            List<String> tokens = new ArrayList<>();
            for (JsonNode tokenNode : responseNode) {
                String token = tokenNode.path("token").asText();
                if (token != null && !token.isEmpty()) {
                    tokens.add(token);
                }
            }

            log.info("成功获取 {} 个token", tokens.size());

            // 5. 轮询获取判题结果
            JsonNode resultsNode = pollForResults(tokens, baseUrl);
            if (resultsNode == null) {
                return JudgeResult.error("获取判题结果超时");
            }

            // 6. 分析结果并保存测试点
            JudgeResult judgeResult = analyzeResultsAndSaveTestPoints(resultsNode, testCount, testData, submission);

            return judgeResult;

        } catch (Exception ex) {
            log.error("Judge0批量判题调用异常", ex);
            return JudgeResult.error("Judge0调用异常: " + ex.getMessage());
        }
    }

    /**
     * 轮询获取判题结果
     */
    private JsonNode pollForResults(List<String> tokens, String baseUrl) throws Exception {
        int maxRetries = 60; // 最多轮询60次
        int retryInterval = 1000; // 每次间隔1秒

        String tokensParam = String.join(",", tokens);
        String queryUrl = baseUrl + "/submissions/batch?tokens=" + tokensParam + "&base64_encoded=false";

        log.info("开始轮询结果，URL: {}", queryUrl);

        for (int i = 0; i < maxRetries; i++) {
            if (i > 0) {
                Thread.sleep(retryInterval);
            }

            HttpRequest queryRequest = HttpRequest.newBuilder()
                    .uri(URI.create(queryUrl))
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> queryResponse = httpClient.send(queryRequest, HttpResponse.BodyHandlers.ofString());

            if (queryResponse.statusCode() == 200) {
                JsonNode responseNode = objectMapper.readTree(queryResponse.body());

                // 关键修复：获取实际的 submissions 数组
                JsonNode resultsNode;
                if (responseNode.has("submissions")) {
                    resultsNode = responseNode.get("submissions");
                    log.debug("从响应中提取 submissions 数组");
                } else {
                    resultsNode = responseNode;
                }

                if (resultsNode.isArray() && resultsNode.size() == tokens.size()) {
                    // 检查是否所有任务都已完成
                    boolean allCompleted = true;
                    int completedCount = 0;

                    for (JsonNode result : resultsNode) {
                        int statusId = result.path("status").path("id").asInt(-1);
                        // 状态1: In Queue, 2: Processing，其他表示已完成
                        if (statusId == 1 || statusId == 2) {
                            allCompleted = false;
                        } else {
                            completedCount++;
                        }
                    }

                    if (allCompleted) {
                        log.info("所有判题任务已完成，轮询次数: {}", i + 1);
                        return resultsNode;
                    }

                    // 每5次打印一次进度
                    if (i % 5 == 0 && i > 0) {
                        log.info("等待判题完成... 已完成: {}/{}, 已等待: {}秒",
                                completedCount, tokens.size(), i + 1);
                    }
                } else {
                    log.warn("返回结果格式错误或数量不匹配，期望{}个，实际{}个",
                            tokens.size(), resultsNode.isArray() ? resultsNode.size() : "不是数组");
                }
            } else {
                log.warn("查询结果失败，HTTP状态码: {}", queryResponse.statusCode());
            }
        }

        log.error("轮询超时，{}次尝试后仍未完成", maxRetries);
        return null;
    }

    /**
     * 分析判题结果并保存测试点
     */
    private JudgeResult analyzeResultsAndSaveTestPoints(JsonNode responseNode, int testCount,
                                                        FileUtil.TestData testData, Submission submission) {
        // 如果传入的是包含 submissions 字段的对象，提取数组
        JsonNode resultsNode;
        if (responseNode.has("submissions")) {
            resultsNode = responseNode.get("submissions");
            log.info("从响应中提取 submissions 数组进行分析");
        } else {
            resultsNode = responseNode;
        }

        int passedCount = 0;
        String firstFailedDetail = null;
        Submission.JudgeStatus finalStatus = Submission.JudgeStatus.AC;
        Integer maxRuntimeMs = 0;
        Integer maxMemoryKb = 0;
        String firstToken = null;

        log.info("开始分析结果，共{}个测试用例", resultsNode.size());

        // 用于保存所有测试点
        List<SubmissionTestPoint> testPoints = new ArrayList<>();

        for (int i = 0; i < resultsNode.size(); i++) {
            JsonNode result = resultsNode.get(i);
            int statusId = result.path("status").path("id").asInt(-1);
            String statusDescription = result.path("status").path("description").asText("UNKNOWN");
            Submission.JudgeStatus judgeStatus = mapStatus(statusId);

            if (i == 0) {
                firstToken = result.path("token").asText("");
            }

            Integer runtimeMs = parseRuntimeMs(result.path("time").asText(""));
            Integer memoryKb = result.path("memory").isNumber() ? result.path("memory").asInt() : null;

            if (runtimeMs != null && runtimeMs > maxRuntimeMs) {
                maxRuntimeMs = runtimeMs;
            }
            if (memoryKb != null && memoryKb > maxMemoryKb) {
                maxMemoryKb = memoryKb;
            }

            log.info("测试用例 {} - 状态: {} ({})", i + 1, statusDescription, statusId);

            // 创建 SubmissionTestPoint 实体
            SubmissionTestPoint testPoint = new SubmissionTestPoint();
            testPoint.setSubmission(submission);
            testPoint.setTestPointId(i + 1);
            testPoint.setJudgeStatus(judgeStatus);
            testPoint.setRuntimeMs(runtimeMs);
            testPoint.setMemoryKb(memoryKb);
            testPoint.setStdout(result.path("stdout").asText(""));
            testPoint.setExpectedOutput(testData.getOutput(i));
            testPoint.setStderr(result.path("stderr").asText(""));
            testPoint.setCompileOutput(result.path("compile_output").asText(""));
            testPoint.setJudge0Token(result.path("token").asText(""));
            testPoint.setCreatedAt(LocalDateTime.now());

            testPoints.add(testPoint);

            if (judgeStatus == Submission.JudgeStatus.AC) {
                passedCount++;
                log.info("测试用例 {} 通过", i + 1);
            } else {
                log.warn("测试用例 {} 失败，状态: {}", i + 1, statusDescription);
                if (finalStatus == Submission.JudgeStatus.AC) {
                    finalStatus = judgeStatus;
                }
                if (firstFailedDetail == null) {
                    String detail = pickFirstNonBlank(
                            result.path("compile_output").asText(""),
                            result.path("stderr").asText(""),
                            result.path("message").asText(""),
                            result.path("stdout").asText("")
                    );
                    firstFailedDetail = String.format("测试用例 %d 失败 [%s]: %s", i + 1, statusDescription, detail);
                }
            }
        }

        // 批量保存所有测试点
        try {
            submissionTestPointRepository.saveAll(testPoints);
            log.info("成功保存 {} 个测试点结果", testPoints.size());
        } catch (Exception e) {
            log.error("保存测试点结果失败", e);
        }

        boolean allPassed = (passedCount == testCount);
        log.info("判题完成: 通过 {}/{}, 最终状态: {}", passedCount, testCount, finalStatus);

        if (allPassed) {
            return new JudgeResult(true, Submission.JudgeStatus.AC, maxRuntimeMs, maxMemoryKb,
                    firstToken, String.format("✅ 所有 %d 个测试用例全部通过", testCount), "ACCEPTED");
        } else {
            return new JudgeResult(false, finalStatus, maxRuntimeMs, maxMemoryKb,
                    firstToken, firstFailedDetail, "FAILED");
        }
    }

    private String normalizeBaseUrl(String url) {
        if (url == null) return "";
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private Integer resolveLanguageId(String language) {
        if (language == null) return null;
        return switch (language.toLowerCase(Locale.ROOT)) {
            case "c" -> 50;
            case "cpp", "c++" -> 54;
            case "java" -> 62;
            case "python", "python3" -> 71;
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
            default -> Submission.JudgeStatus.SE;
        };
    }

    private Integer parseRuntimeMs(String secondsText) {
        if (secondsText == null || secondsText.isBlank()) return null;
        try {
            return (int) Math.round(Double.parseDouble(secondsText) * 1000.0);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String pickFirstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.length() > 800 ? value.substring(0, 800) : value;
            }
        }
        return "";
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