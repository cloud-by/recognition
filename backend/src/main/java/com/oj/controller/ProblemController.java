package com.oj.controller;

import com.oj.dto.ApiResponse;
import com.oj.entity.Problem;
import com.oj.repository.ProblemRepository;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/problems")
public class ProblemController {

    private final ProblemRepository problemRepository;
    private static final Set<String> SUPPORTED_DIFFICULTY = Set.of("入门", "普及", "提高");

    public ProblemController(ProblemRepository problemRepository) {
        this.problemRepository = problemRepository;
    }

    @GetMapping
    public ApiResponse<List<Problem>> list(@RequestParam(required = false) String keyword) {
        List<Problem> problems = Optional.ofNullable(keyword)
                .filter(value -> !value.isBlank())
                .map(problemRepository::findByTitleContainingIgnoreCase)
                .orElseGet(problemRepository::findAll);
        return ApiResponse.ok(problems);
    }

    @GetMapping("/{id}")
    public ApiResponse<Problem> detail(@PathVariable Long id) {
        return problemRepository.findById(id)
                .map(ApiResponse::ok)
                .orElseGet(() -> ApiResponse.fail("题目不存在"));
    }

    @PostMapping
    public ApiResponse<Problem> create(@RequestBody CreateProblemRequest request) {
        Problem problem = new Problem();
        problem.setTitle(request.title());
        problem.setDescription(request.description());
        problem.setInputFormat(request.inputFormat());
        problem.setOutputFormat(request.outputFormat());
        problem.setSampleInput(request.sampleInput());
        problem.setSampleOutput(request.sampleOutput());

        String difficulty = Optional.ofNullable(request.difficulty()).orElse("普及");
        if (!SUPPORTED_DIFFICULTY.contains(difficulty)) {
            return ApiResponse.fail("难度仅支持：入门 / 普及 / 提高");
        }
        problem.setDifficulty(difficulty);

        problem.setTimeLimitMs(Optional.ofNullable(request.timeLimitMs()).orElse(1000));
        problem.setMemoryLimitMb(Optional.ofNullable(request.memoryLimitMb()).orElse(256));
        problem.setTestcasePath(request.testcasePath());
        return ApiResponse.ok(problemRepository.save(problem));
    }

    public record CreateProblemRequest(
            @NotBlank String title,
            @NotBlank String description,
            String inputFormat,
            String outputFormat,
            String sampleInput,
            String sampleOutput,
            String difficulty,
            Integer timeLimitMs,
            Integer memoryLimitMb,
            @NotBlank String testcasePath
    ) {
    }
}