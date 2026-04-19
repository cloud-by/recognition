package com.oj.controller;

import com.oj.dto.ApiResponse;
import com.oj.entity.AntiCheatLog;
import com.oj.entity.OjUser;
import com.oj.entity.Problem;
import com.oj.entity.Submission;
import com.oj.repository.AntiCheatLogRepository;
import com.oj.repository.OjUserRepository;
import com.oj.repository.ProblemRepository;
import com.oj.repository.SubmissionRepository;
import com.oj.service.Judge0Service;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/submissions")
public class SubmissionController {

    private final SubmissionRepository submissionRepository;
    private final OjUserRepository userRepository;
    private final ProblemRepository problemRepository;
    private final AntiCheatLogRepository antiCheatLogRepository;
    private final Judge0Service judge0Service;

    public SubmissionController(
            SubmissionRepository submissionRepository,
            OjUserRepository userRepository,
            ProblemRepository problemRepository,
            AntiCheatLogRepository antiCheatLogRepository,
            Judge0Service judge0Service
    ) {
        this.submissionRepository = submissionRepository;
        this.userRepository = userRepository;
        this.problemRepository = problemRepository;
        this.antiCheatLogRepository = antiCheatLogRepository;
        this.judge0Service = judge0Service;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(@RequestParam Long userId) {
        List<Map<String, Object>> data = submissionRepository.findByUserIdOrderBySubmitTimeDesc(userId)
                .stream()
                .map(submission -> Map.<String, Object>of(
                        "id", submission.getId(),
                        "problemId", submission.getProblem().getId(),
                        "language", submission.getLanguage(),
                        "judgeStatus", submission.getJudgeStatus(),
                        "runtimeMs", Optional.ofNullable(submission.getRuntimeMs()).orElse(0),
                        "memoryKb", Optional.ofNullable(submission.getMemoryKb()).orElse(0),
                        "submitIp", Optional.ofNullable(submission.getSubmitIp()).orElse(""),
                        "submitTime", submission.getSubmitTime(),
                        "judgeDetail", Optional.ofNullable(submission.getJudgeDetail()).orElse("")
                ))
                .toList();
        return ApiResponse.ok(data);
    }

    @PostMapping
    public ApiResponse<?> submit(@RequestBody SubmitRequest request, HttpServletRequest httpRequest) {
        //分别根据用户id和题目id获取用户和题目
        Optional<OjUser> user = userRepository.findById(request.userId());
        Optional<Problem> problem = problemRepository.findById(request.problemId());

        if (user.isEmpty() || problem.isEmpty()) {
            return ApiResponse.fail("用户或题目不存在");
        }
        //获取语言并存为小写
        String language = request.language().toLowerCase();
        if (!List.of("c", "cpp", "c++", "java", "python").contains(language)) {
            return ApiResponse.fail("当前仅支持 C / C++ / java / python 提交");
        }

        String submitIp = resolveIp(httpRequest);   //解析获得对方IPV6地址，如0:0:0:0:0:0:0:1
        //创建当前提交记录
        Submission submission = new Submission();
        submission.setUser(user.get());
        submission.setProblem(problem.get());
        submission.setSourceCode(request.sourceCode());
        submission.setLanguage(language);
        submission.setJudgeStatus(Submission.JudgeStatus.PENDING);  //记录为待定
        submission.setSubmitTime(LocalDateTime.now());
        submission.setSubmitIp(submitIp);

        submission.setJudgeStatus(Submission.JudgeStatus.JUDGING);  //将记录改写为判题中
        Submission saved = submissionRepository.save(submission);   //将当前创建的记录添加到数据库中，并获得赋值id的数据
        doIpCheatCheck(user.get(), saved, submitIp);    //检查IP地址是否合规
        //判题并获得判题结果
        Judge0Service.JudgeResult judgeResult = judge0Service.judge(language, request.sourceCode(), problem.get());
        //更新判题结果
        saved.setJudgeStatus(judgeResult.judgeStatus());
        saved.setRuntimeMs(judgeResult.runtimeMs());
        saved.setMemoryKb(judgeResult.memoryKb());
        saved.setJudgeToken(judgeResult.token());
        saved.setJudgeDetail(judgeResult.detail());
        submissionRepository.save(saved);
        //返回结果
        Map<String, Object> resp = Map.of(
                "submissionId", saved.getId(),           // 提交记录ID
                "judgeStatus", saved.getJudgeStatus(),    // 评测状态（AC/WA/TLE等）
                "runtimeMs", Optional.ofNullable(saved.getRuntimeMs()).orElse(0),  // 运行时间（毫秒）
                "memoryKb", Optional.ofNullable(saved.getMemoryKb()).orElse(0),    // 内存使用（KB）
                "judgeDetail", Optional.ofNullable(saved.getJudgeDetail()).orElse(""), // 评测详情
                "judgeToken", Optional.ofNullable(saved.getJudgeToken()).orElse(""),   // Judge0令牌
                "rawStatus", judgeResult.rawStatus(),     // Judge0原始状态描述
                "message", judgeResult.success() ? "Judge0评测完成" : "Judge0评测失败，已标记为SE", // 提示消息
                "submitIp", submitIp                      // 提交IP
        );
        return ApiResponse.ok(resp);
    }

    private void doIpCheatCheck(OjUser user, Submission submission, String submitIp) {
        if (submitIp.isBlank()) {
            return;
        }
        if (!isLabIp(submitIp)) {
            saveLog(user, submission, "OUTSIDE_LAB_IP", "提交IP不在机房网段: " + submitIp);
        }
        if (user.getLastSubmitIp() != null && !user.getLastSubmitIp().isBlank() && !user.getLastSubmitIp().equals(submitIp)) {
            saveLog(user, submission, "SUBMIT_IP_CHANGED", "IP从" + user.getLastSubmitIp() + "变化为" + submitIp);
        }
        user.setLastSubmitIp(submitIp);
        userRepository.save(user);
    }

    private void saveLog(OjUser user, Submission submission, String type, String detail) {
        AntiCheatLog log = new AntiCheatLog();
        log.setUser(user);
        log.setSubmission(submission);
        log.setBehaviorType(type);
        log.setDetailInfo(detail);
        log.setOccurredTime(LocalDateTime.now());
        antiCheatLogRepository.save(log);
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return Optional.ofNullable(request.getRemoteAddr()).orElse("");
    }

    private boolean isLabIp(String ip) {
        return ip.startsWith("10.") || ip.startsWith("192.168.")
                || ip.startsWith("172.16.") || ip.startsWith("172.17.") || ip.startsWith("172.18.")
                || ip.startsWith("172.19.") || ip.startsWith("172.20.") || ip.startsWith("172.21.")
                || ip.startsWith("172.22.") || ip.startsWith("172.23.") || ip.startsWith("172.24.")
                || ip.startsWith("172.25.") || ip.startsWith("172.26.") || ip.startsWith("172.27.")
                || ip.startsWith("172.28.") || ip.startsWith("172.29.") || ip.startsWith("172.30.")
                || ip.startsWith("172.31.");
    }

    public record SubmitRequest(
            @NotNull Long userId,
            @NotNull Long problemId,
            @NotBlank String sourceCode,
            @NotBlank String language
    ) {}
}