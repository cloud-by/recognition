package com.oj.controller;

import com.oj.dto.ApiResponse;
import com.oj.entity.Contest;
import com.oj.entity.ContestParticipant;
import com.oj.entity.ContestProblem;
import com.oj.entity.OjUser;
import com.oj.entity.Problem;
import com.oj.repository.ContestParticipantRepository;
import com.oj.repository.ContestProblemRepository;
import com.oj.repository.ContestRepository;
import com.oj.repository.OjUserRepository;
import com.oj.repository.ProblemRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contests")
public class ContestController {

    private final ContestRepository contestRepository;
    private final ProblemRepository problemRepository;
    private final OjUserRepository ojUserRepository;
    private final ContestProblemRepository contestProblemRepository;
    private final ContestParticipantRepository contestParticipantRepository;

    public ContestController(
            ContestRepository contestRepository,
            ProblemRepository problemRepository,
            OjUserRepository ojUserRepository,
            ContestProblemRepository contestProblemRepository,
            ContestParticipantRepository contestParticipantRepository
    ) {
        this.contestRepository = contestRepository;
        this.problemRepository = problemRepository;
        this.ojUserRepository = ojUserRepository;
        this.contestProblemRepository = contestProblemRepository;
        this.contestParticipantRepository = contestParticipantRepository;
    }

    @GetMapping
    public ApiResponse<List<ContestItemResponse>> list(@RequestParam(required = false) Long viewerUserId) {
        OjUser viewer = viewerUserId == null ? null : ojUserRepository.findById(viewerUserId).orElse(null);
        List<Contest> contests = viewer != null && OjUser.Role.TEACHER.name().equals(viewer.getRole())
                ? contestRepository.findByCreatedByUserIdOrderByStartTimeDesc(viewerUserId)
                : contestRepository.findAllByOrderByStartTimeDesc();

        Set<Long> contestIds = contests.stream().map(Contest::getId).collect(Collectors.toSet());
        Map<Long, Long> problemCountMap = contestProblemRepository.findAll().stream()
                .filter(cp -> contestIds.contains(cp.getId().getContestId()))
                .collect(Collectors.groupingBy(cp -> cp.getId().getContestId(), Collectors.counting()));

        final Set<Long> joinedContestIds = viewerUserId == null
                ? Set.of()
                : contestParticipantRepository.findAll().stream()
                .filter(cp -> cp.getId().getUserId().equals(viewerUserId))
                .map(cp -> cp.getId().getContestId())
                .collect(Collectors.toSet());

        LocalDateTime now = LocalDateTime.now();
        List<ContestItemResponse> data = contests.stream().map(contest -> new ContestItemResponse(
                contest.getId(),
                contest.getTitle(),
                contest.getStartTime(),
                contest.getEndTime(),
                contest.getContestType(),
                contest.getRankingPolicy(),
                contest.getFreezeBoard(),
                getContestStatus(contest, now),
                problemCountMap.getOrDefault(contest.getId(), 0L).intValue(),
                joinedContestIds.contains(contest.getId()),
                contest.getCreatedByUserId()
        )).toList();
        return ApiResponse.ok(data);
    }

    @GetMapping("/{id}")
    public ApiResponse<?> detail(@PathVariable Long id) {
        Contest contest = contestRepository.findById(id).orElse(null);
        if (contest == null) {
            return ApiResponse.fail("比赛不存在");
        }

        List<ContestProblem> linked = contestProblemRepository.findByIdContestIdOrderBySortOrderAsc(id);
        List<Long> problemIds = linked.stream().map(cp -> cp.getId().getProblemId()).toList();
        Map<Long, Problem> problemMap = problemRepository.findAllById(problemIds).stream()
                .collect(Collectors.toMap(Problem::getId, p -> p));
        List<ContestProblemResponse> problems = linked.stream().map(item -> {
            Problem p = problemMap.get(item.getId().getProblemId());
            return new ContestProblemResponse(
                    item.getId().getProblemId(),
                    p == null ? "题目已删除" : p.getTitle(),
                    item.getSortOrder(),
                    p == null ? "未知" : p.getDifficulty()
            );
        }).sorted(Comparator.comparing(ContestProblemResponse::sortOrder)).toList();

        return ApiResponse.ok(new ContestDetailResponse(
                contest.getId(),
                contest.getTitle(),
                contest.getStartTime(),
                contest.getEndTime(),
                contest.getContestType(),
                contest.getRankingPolicy(),
                contest.getFreezeBoard(),
                getContestStatus(contest, LocalDateTime.now()),
                contestParticipantRepository.findByIdContestId(id).size(),
                contest.getCreatedByUserId(),
                problems
        ));
    }

    @PostMapping
    public ApiResponse<?> create(@RequestBody CreateContestRequest request) {
        OjUser creator = ojUserRepository.findById(request.creatorUserId()).orElse(null);
        if (creator == null || !(OjUser.Role.ADMIN.name().equals(creator.getRole()) || OjUser.Role.TEACHER.name().equals(creator.getRole()))) {
            return ApiResponse.fail("仅管理员或老师可创建比赛");
        }
        if (request.startTime() == null || request.endTime() == null || !request.endTime().isAfter(request.startTime())) {
            return ApiResponse.fail("比赛时间无效");
        }
        List<Long> deduplicated = request.problemIds().stream().distinct().toList();
        List<Problem> problems = problemRepository.findAllById(deduplicated);
        if (problems.size() != deduplicated.size()) {
            return ApiResponse.fail("存在无效题目，请重新选择");
        }

        Contest contest = new Contest();
        contest.setTitle(request.title().trim());
        contest.setStartTime(request.startTime());
        contest.setEndTime(request.endTime());
        contest.setContestType(request.contestType() == null ? Contest.ContestType.ACM : request.contestType());
        contest.setRankingPolicy(request.rankingPolicy() == null ? Contest.RankingPolicy.FORMAL : request.rankingPolicy());
        contest.setFreezeBoard(Boolean.TRUE.equals(request.freezeBoard()));
        contest.setCreatedByUserId(request.creatorUserId());
        Contest saved = contestRepository.save(contest);

        List<ContestProblem> links = deduplicated.stream()
                .map(problemId -> new ContestProblem(saved.getId(), problemId, deduplicated.indexOf(problemId) + 1))
                .toList();
        contestProblemRepository.saveAll(links);

        return detail(saved.getId());
    }

    @PostMapping("/{id}/register")
    public ApiResponse<?> register(@PathVariable Long id, @RequestBody RegisterContestRequest request) {
        Contest contest = contestRepository.findById(id).orElse(null);
        if (contest == null) {
            return ApiResponse.fail("比赛不存在");
        }
        if (!contest.getStartTime().isAfter(LocalDateTime.now())) {
            return ApiResponse.fail("比赛已开始或结束，无法报名");
        }

        OjUser user = ojUserRepository.findById(request.userId()).orElse(null);
        if (user == null) {
            return ApiResponse.fail("用户不存在");
        }
        if (contestParticipantRepository.existsByIdContestIdAndIdUserId(id, request.userId())) {
            return ApiResponse.fail("你已报名该比赛");
        }

        contestParticipantRepository.save(new ContestParticipant(id, request.userId()));
        return ApiResponse.ok("报名成功");
    }

    @GetMapping("/problems/options")
    public ApiResponse<List<ProblemOptionResponse>> contestProblemOptions() {
        return ApiResponse.ok(problemRepository.findAll().stream()
                .map(item -> new ProblemOptionResponse(item.getId(), item.getTitle(), item.getDifficulty()))
                .sorted(Comparator.comparing(ProblemOptionResponse::id))
                .toList());
    }

    private String getContestStatus(Contest contest, LocalDateTime now) {
        if (now.isBefore(contest.getStartTime())) return "NOT_STARTED";
        if (now.isAfter(contest.getEndTime())) return "FINISHED";
        return "RUNNING";
    }

    public record CreateContestRequest(
            @NotNull Long creatorUserId,
            @NotBlank String title,
            @NotNull LocalDateTime startTime,
            @NotNull LocalDateTime endTime,
            Contest.ContestType contestType,
            Contest.RankingPolicy rankingPolicy,
            Boolean freezeBoard,
            @NotEmpty List<Long> problemIds
    ) {}

    public record RegisterContestRequest(@NotNull Long userId) {}
    public record ContestProblemResponse(Long problemId, String title, Integer sortOrder, String difficulty) {}
    public record ProblemOptionResponse(Long id, String title, String difficulty) {}

    public record ContestItemResponse(
            Long id, String title, LocalDateTime startTime, LocalDateTime endTime,
            Contest.ContestType contestType, Contest.RankingPolicy rankingPolicy,
            Boolean freezeBoard, String status, Integer problemCount, Boolean joined, Long createdByUserId
    ) {}

    public record ContestDetailResponse(
            Long id, String title, LocalDateTime startTime, LocalDateTime endTime,
            Contest.ContestType contestType, Contest.RankingPolicy rankingPolicy,
            Boolean freezeBoard, String status, Integer participantCount, Long createdByUserId,
            List<ContestProblemResponse> problems
    ) {}
}