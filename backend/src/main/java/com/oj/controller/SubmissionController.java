package com.oj.controller;

import com.oj.dto.ApiResponse;
import com.oj.entity.OjUser;
import com.oj.entity.Problem;
import com.oj.entity.Submission;
import com.oj.repository.OjUserRepository;
import com.oj.repository.ProblemRepository;
import com.oj.repository.SubmissionRepository;
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
@RequestMapping("/api/submissions")
public class SubmissionController {

    private final SubmissionRepository submissionRepository;
    private final OjUserRepository userRepository;
    private final ProblemRepository problemRepository;

    public SubmissionController(
            SubmissionRepository submissionRepository,
            OjUserRepository userRepository,
            ProblemRepository problemRepository
    ) {
        this.submissionRepository = submissionRepository;
        this.userRepository = userRepository;
        this.problemRepository = problemRepository;
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
                        "submitTime", submission.getSubmitTime()
                ))
                .toList();
        return ApiResponse.ok(data);
    }

    @PostMapping
    public ApiResponse<?> submit(@RequestBody SubmitRequest request) {
        Optional<OjUser> user = userRepository.findById(request.userId());
        Optional<Problem> problem = problemRepository.findById(request.problemId());

        if (user.isEmpty() || problem.isEmpty()) {
            return ApiResponse.fail("用户或题目不存在");
        }

        String language = request.language().toLowerCase();
        if (!List.of("c", "cpp").contains(language)) {
            return ApiResponse.fail("当前仅支持 C / C++ 提交");
        }

        Submission submission = new Submission();
        submission.setUser(user.get());
        submission.setProblem(problem.get());
        submission.setSourceCode(request.sourceCode());
        submission.setLanguage(language);
        submission.setJudgeStatus(Submission.JudgeStatus.PENDING);
        submission.setSubmitTime(LocalDateTime.now());

        Submission saved = submissionRepository.save(submission);
        Map<String, Object> resp = Map.of(
                "submissionId", saved.getId(),
                "queueStatus", "QUEUED",
                "message", "基础框架已接收提交，后续可接入 Judge0 + Redis 队列"
        );
        return ApiResponse.ok(resp);
    }

    public record SubmitRequest(
            @NotNull Long userId,
            @NotNull Long problemId,
            @NotBlank String sourceCode,
            @NotBlank String language
    ) {
    }
}