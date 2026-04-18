package com.oj.controller;

import com.oj.dto.ApiResponse;
import com.oj.entity.Contest;
import com.oj.entity.ContestProblem;
import com.oj.entity.OjUser;
import com.oj.entity.Problem;
import com.oj.repository.ContestParticipantRepository;
import com.oj.repository.ContestProblemRepository;
import com.oj.repository.ContestRepository;
import com.oj.repository.OjUserRepository;
import com.oj.repository.ProblemRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/problems")
public class ProblemController {

    private final ProblemRepository problemRepository;
    private final OjUserRepository userRepository;
    private final ContestRepository contestRepository;
    private final ContestProblemRepository contestProblemRepository;
    private final ContestParticipantRepository contestParticipantRepository;
    private static final Set<String> SUPPORTED_DIFFICULTY = Set.of("入门", "普及", "提高");

    public ProblemController(
            ProblemRepository problemRepository,
            OjUserRepository userRepository,
            ContestRepository contestRepository,
            ContestProblemRepository contestProblemRepository,
            ContestParticipantRepository contestParticipantRepository
    ) {
        this.problemRepository = problemRepository;
        this.userRepository = userRepository;
        this.contestRepository = contestRepository;
        this.contestProblemRepository = contestProblemRepository;
        this.contestParticipantRepository = contestParticipantRepository;
    }

    @GetMapping
    public ApiResponse<List<Problem>> list(@RequestParam(required = false) String keyword, @RequestParam(required = false) Long viewerUserId) {
        OjUser viewer = Optional.ofNullable(viewerUserId).flatMap(userRepository::findById).orElse(null);
        List<Problem> problems = Optional.ofNullable(keyword)
                .filter(value -> !value.isBlank())
                .map(problemRepository::findByTitleContainingIgnoreCase)
                .orElseGet(problemRepository::findAll)
                .stream()
                .filter(problem -> canViewProblem(problem, viewer, null))
                .toList();
        return ApiResponse.ok(problems);
    }

    @GetMapping("/{id}")
    public ApiResponse<Problem> detail(
            @PathVariable Long id,
            @RequestParam(required = false) Long viewerUserId,
            @RequestParam(required = false) Long contestId
    ) {
        OjUser viewer = Optional.ofNullable(viewerUserId).flatMap(userRepository::findById).orElse(null);
        return problemRepository.findById(id)
                .filter(problem -> canViewProblem(problem, viewer, contestId))
                .map(ApiResponse::ok)
                .orElseGet(() -> ApiResponse.fail("题目不存在或无查看权限"));
    }

    @PostMapping
    public ApiResponse<Problem> create(@RequestBody CreateProblemRequest request) {
        OjUser creator = userRepository.findById(request.creatorUserId()).orElse(null);
        if (creator == null || !OjUser.Role.ADMIN.name().equals(creator.getRole())) {
            return ApiResponse.fail("仅管理员可创建题目");
        }

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

        problem.setPermissionType(Optional.ofNullable(request.permissionType()).orElse(Problem.PermissionType.PUBLIC));
        problem.setTags(Optional.ofNullable(request.tags()).orElse(""));
        problem.setTimeLimitMs(Optional.ofNullable(request.timeLimitMs()).orElse(1000));
        problem.setMemoryLimitMb(Optional.ofNullable(request.memoryLimitMb()).orElse(256));
        problem.setTestcasePath(request.testcasePath());
        return ApiResponse.ok(problemRepository.save(problem));
    }

    @PutMapping("/{id}")
    public ApiResponse<Problem> update(@PathVariable Long id, @RequestBody UpdateProblemRequest request) {
        OjUser editor = userRepository.findById(request.editorUserId()).orElse(null);
        if (editor == null || !OjUser.Role.ADMIN.name().equals(editor.getRole())) {
            return ApiResponse.fail("仅管理员可编辑题目");
        }

        Problem problem = problemRepository.findById(id).orElse(null);
        if (problem == null) {
            return ApiResponse.fail("题目不存在");
        }

        String difficulty = Optional.ofNullable(request.difficulty()).orElse("普及");
        if (!SUPPORTED_DIFFICULTY.contains(difficulty)) {
            return ApiResponse.fail("难度仅支持：入门 / 普及 / 提高");
        }

        problem.setTitle(request.title());
        problem.setDescription(request.description());
        problem.setInputFormat(request.inputFormat());
        problem.setOutputFormat(request.outputFormat());
        problem.setSampleInput(request.sampleInput());
        problem.setSampleOutput(request.sampleOutput());
        problem.setDifficulty(difficulty);
        problem.setPermissionType(Optional.ofNullable(request.permissionType()).orElse(Problem.PermissionType.PUBLIC));
        problem.setTags(Optional.ofNullable(request.tags()).orElse(""));
        problem.setTimeLimitMs(Optional.ofNullable(request.timeLimitMs()).orElse(1000));
        problem.setMemoryLimitMb(Optional.ofNullable(request.memoryLimitMb()).orElse(256));
        problem.setTestcasePath(request.testcasePath());
        return ApiResponse.ok(problemRepository.save(problem));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ApiResponse<List<Problem>> delete(@PathVariable Long id, @RequestParam Long operatorUserId) {
        OjUser operator = userRepository.findById(operatorUserId).orElse(null);
        if (operator == null || !OjUser.Role.ADMIN.name().equals(operator.getRole())) {
            return ApiResponse.fail("仅管理员可删除题目");
        }

        Problem problem = problemRepository.findById(id).orElse(null);
        if (problem == null) {
            return ApiResponse.fail("题目不存在");
        }

        problemRepository.delete(problem);
        problemRepository.flush();
        problemRepository.compactIdsAfterDelete(id);
        return ApiResponse.ok(problemRepository.findAll());
    }

    private boolean canViewProblem(Problem problem, OjUser viewer, Long contestId) {
        if (viewer != null && (OjUser.Role.ADMIN.name().equals(viewer.getRole()) || OjUser.Role.TEACHER.name().equals(viewer.getRole()))) {
            return true;
        }

        if (problem.getPermissionType() == Problem.PermissionType.PUBLIC) {
            return true;
        }
        if (problem.getPermissionType() == Problem.PermissionType.LOGIN_REQUIRED) {
            return viewer != null;
        }
        if (viewer == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        if (contestId != null && isContestRunningAndAuthorized(contestId, problem.getId(), viewer.getId(), now)) {
            return true;
        }

        return contestProblemRepository.findByIdProblemId(problem.getId()).stream().anyMatch(cp ->
                isContestRunningAndAuthorized(cp.getId().getContestId(), problem.getId(), viewer.getId(), now));
    }

    private boolean isContestRunningAndAuthorized(Long contestId, Long problemId, Long viewerId, LocalDateTime now) {
        if (contestProblemRepository.findByIdContestIdAndIdProblemId(contestId, problemId).isEmpty()) {
            return false;
        }
        Contest contest = contestRepository.findById(contestId).orElse(null);
        if (contest == null || now.isBefore(contest.getStartTime()) || now.isAfter(contest.getEndTime())) {
            return false;
        }
        return contestParticipantRepository.existsByIdContestIdAndIdUserId(contestId, viewerId);
    }

    public record CreateProblemRequest(
            @NotNull Long creatorUserId,
            @NotBlank String title,
            @NotBlank String description,
            String inputFormat,
            String outputFormat,
            String sampleInput,
            String sampleOutput,
            String difficulty,
            Problem.PermissionType permissionType,
            String tags,
            Integer timeLimitMs,
            Integer memoryLimitMb,
            @NotBlank String testcasePath
    ) {}

    public record UpdateProblemRequest(
            @NotNull Long editorUserId,
            @NotBlank String title,
            @NotBlank String description,
            String inputFormat,
            String outputFormat,
            String sampleInput,
            String sampleOutput,
            String difficulty,
            Problem.PermissionType permissionType,
            String tags,
            Integer timeLimitMs,
            Integer memoryLimitMb,
            @NotBlank String testcasePath
    ) {}
}