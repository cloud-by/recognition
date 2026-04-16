package com.oj.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.stream.Collectors;

/**
 * IDEA 可直接运行的 Judge0 冒烟测试。
 *
 * 用法（Program arguments）：
 * --base-url=http://127.0.0.1:2358
 * --language-id=54
 * --stdin=1 2\n
 * --expected-output=3\n
 */
public class Judge0SmokeTestRunner {

    public static void main(String[] args) {
        int exitCode;
        try {
            exitCode = run(args);
        } catch (Exception ex) {
            Throwable root = rootCause(ex);
            if (root instanceof ConnectException) {
                System.out.println("❌ 无法连接到 Judge0（ConnectException）。");
                System.out.println("请检查：");
                System.out.println("1) Judge0 是否启动（docker ps / 服务进程）");
                System.out.println("2) --base-url 是否写对（不要默认127.0.0.1，除非Judge0和IDEA在同一台机器）");
                System.out.println("3) 端口 2358 是否放通（云服务器安全组 / 防火墙 / 反向代理）");
                System.out.println("4) 若在 Windows 上访问 Linux 服务器，请用服务器IP而不是 localhost");
                System.exit(2);
                return;
            }
            System.out.println("❌ 测试失败: " + root.getClass().getSimpleName() + " - " + root.getMessage());
            ex.printStackTrace();
            System.exit(99);
            return;
        }
        System.exit(exitCode);
    }

    private static int run(String[] args) throws Exception {
        Config config = Config.fromArgs(args);
        ObjectMapper mapper = new ObjectMapper();
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();

        String baseUrl = config.baseUrl.endsWith("/") ? config.baseUrl.substring(0, config.baseUrl.length() - 1) : config.baseUrl;
        String submitUrl = baseUrl + "/submissions?base64_encoded=false&wait=false";
        String languagesUrl = baseUrl + "/languages";

        System.out.println("[0/3] 连通性预检: " + languagesUrl);
        HttpRequest pingRequest = HttpRequest.newBuilder()
                .uri(URI.create(languagesUrl))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();
        HttpResponse<String> pingResp = client.send(pingRequest, HttpResponse.BodyHandlers.ofString());
        System.out.println("HTTP " + pingResp.statusCode() + ", body-snippet=" + abbreviate(pingResp.body(), 120));
        if (pingResp.statusCode() < 200 || pingResp.statusCode() >= 300) {
            printHttpFailureHints("GET /languages", pingResp);
            return 10;
        }

        String payload = mapper.writeValueAsString(new SubmitPayload(
                config.sourceCode,
                config.languageId,
                config.stdin,
                config.expectedOutput
        ));

        System.out.println("[1/3] 提交到 Judge0: " + submitUrl);
        HttpRequest submitRequest = HttpRequest.newBuilder()
                .uri(URI.create(submitUrl))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> submitResp = client.send(submitRequest, HttpResponse.BodyHandlers.ofString());
        System.out.println("HTTP " + submitResp.statusCode() + ", body=" + submitResp.body());
        if (submitResp.statusCode() < 200 || submitResp.statusCode() >= 300) {
            System.out.println("❌ 提交失败：Judge0 接口不可用或后端异常。");
            printHttpFailureHints("POST /submissions", submitResp);
            return 20;
        }

        String token = mapper.readTree(submitResp.body()).path("token").asText("");
        if (token.isBlank()) {
            System.out.println("❌ 提交失败：返回中没有 token。");
            return 21;
        }

        String queryUrl = baseUrl + "/submissions/" + token
                + "?base64_encoded=false&fields=status,time,memory,stdout,stderr,compile_output,message";
        System.out.println("[2/3] 轮询结果: token=" + token);

        for (int i = 0; i < config.maxPolls; i++) {
            Thread.sleep(config.pollIntervalMs);
            HttpRequest queryRequest = HttpRequest.newBuilder()
                    .uri(URI.create(queryUrl))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> queryResp = client.send(queryRequest, HttpResponse.BodyHandlers.ofString());
            System.out.println("poll#" + (i + 1) + " HTTP " + queryResp.statusCode() + ", body=" + abbreviate(queryResp.body(), 400));

            if (queryResp.statusCode() < 200 || queryResp.statusCode() >= 300) {
                printHttpFailureHints("GET /submissions/{token}", queryResp);
                continue;
            }

            JsonNode result = mapper.readTree(queryResp.body());
            int statusId = result.path("status").path("id").asInt(-1);
            String statusDesc = result.path("status").path("description").asText("UNKNOWN");
            if (statusId == 1 || statusId == 2) {
                continue;
            }

            System.out.println("[3/3] 最终结果");
            System.out.println("status=" + statusDesc + "(" + statusId + "), time=" + result.path("time").asText("")
                    + ", memory=" + result.path("memory").asText(""));
            System.out.println("stdout: " + result.path("stdout").asText(""));
            System.out.println("stderr: " + result.path("stderr").asText(""));
            System.out.println("compile_output: " + result.path("compile_output").asText(""));
            System.out.println("message: " + result.path("message").asText(""));

            if (statusId == 3) {
                System.out.println("✅ Judge0 可用，判题流程正常。");
                return 0;
            } else {
                System.out.println("⚠️ Judge0 可连通，但判题结果不是 AC，请根据输出继续排查。");
                return 30;
            }
        }

        System.out.println("❌ 轮询超时：Judge0 队列拥堵、worker 异常或网络有问题。");
        return 40;
    }

    private static void printHttpFailureHints(String stage, HttpResponse<String> response) {
        String headers = response.headers().map().entrySet().stream()
                .map(e -> e.getKey() + "=" + String.join(",", e.getValue()))
                .collect(Collectors.joining("; "));
        System.out.println("[" + stage + "] 响应头: " + abbreviate(headers, 500));
        if (response.body() == null || response.body().isBlank()) {
            System.out.println("[" + stage + "] 响应体为空：常见于网关/Nginx直接拦截，建议查看反向代理和Judge0容器日志。");
        }
    }

    private static Throwable rootCause(Throwable ex) {
        Throwable cur = ex;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        return cur;
    }

    private static String abbreviate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    private record SubmitPayload(
            String source_code,
            Integer language_id,
            String stdin,
            String expected_output
    ) {}

    private static class Config {
        String baseUrl = "http://127.0.0.1:2358";
        Integer languageId = 54;
        String sourceCode = """
                #include <iostream>
                using namespace std;
                int main(){int a,b; if(!(cin>>a>>b)) return 0; cout<<a+b<<"\\n";}
                """;
        String stdin = "1 2\n";
        String expectedOutput = "3\n";
        int pollIntervalMs = 600;
        int maxPolls = 20;

        static Config fromArgs(String[] args) {
            Config cfg = new Config();
            for (String arg : args) {
                if (arg.startsWith("--base-url=")) {
                    cfg.baseUrl = arg.substring("--base-url=".length());
                } else if (arg.startsWith("--language-id=")) {
                    cfg.languageId = Integer.parseInt(arg.substring("--language-id=".length()));
                } else if (arg.startsWith("--source-code=")) {
                    cfg.sourceCode = arg.substring("--source-code=".length()).replace("\\n", "\n");
                } else if (arg.startsWith("--stdin=")) {
                    cfg.stdin = arg.substring("--stdin=".length()).replace("\\n", "\n");
                } else if (arg.startsWith("--expected-output=")) {
                    cfg.expectedOutput = arg.substring("--expected-output=".length()).replace("\\n", "\n");
                } else if (arg.startsWith("--poll-interval-ms=")) {
                    cfg.pollIntervalMs = Integer.parseInt(arg.substring("--poll-interval-ms=".length()));
                } else if (arg.startsWith("--max-polls=")) {
                    cfg.maxPolls = Integer.parseInt(arg.substring("--max-polls=".length()));
                }
            }
            return cfg;
        }
    }
}
