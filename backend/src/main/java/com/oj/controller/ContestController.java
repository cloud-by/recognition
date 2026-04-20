package com.oj.controller;

import com.oj.dto.ApiResponse;
import com.oj.entity.AntiCheatLog;
import com.oj.entity.Contest;
import com.oj.entity.ContestParticipant;
import com.oj.entity.ContestParticipantId;
import com.oj.entity.ContestProblem;
import com.oj.entity.OjUser;
import com.oj.entity.Problem;
import com.oj.entity.Submission;
import com.oj.entity.TeachingClass;
import com.oj.repository.AntiCheatLogRepository;
import com.oj.repository.ContestParticipantRepository;
import com.oj.repository.ContestProblemRepository;
import com.oj.repository.ContestRepository;
import com.oj.repository.OjUserRepository;
import com.oj.repository.ProblemRepository;
import com.oj.repository.SubmissionRepository;
import com.oj.repository.TeachingClassRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/contests")
public class ContestController {

    private static final long ENTER_WINDOW_MINUTES = 5;

    private final ContestRepository contestRepository;
    private final ProblemRepository problemRepository;
    private final OjUserRepository ojUserRepository;
    private final ContestProblemRepository contestProblemRepository;
    private final ContestParticipantRepository contestParticipantRepository;
    private final AntiCheatLogRepository antiCheatLogRepository;
    private final SubmissionRepository submissionRepository;
    private final TeachingClassRepository teachingClassRepository;

    public ContestController(
            ContestRepository contestRepository,
            ProblemRepository problemRepository,
            OjUserRepository ojUserRepository,
            ContestProblemRepository contestProblemRepository,
            ContestParticipantRepository contestParticipantRepository,
            AntiCheatLogRepository antiCheatLogRepository,
            SubmissionRepository submissionRepository,
            TeachingClassRepository teachingClassRepository
    ) {
        this.contestRepository = contestRepository;
        this.problemRepository = problemRepository;
        this.ojUserRepository = ojUserRepository;
        this.contestProblemRepository = contestProblemRepository;
        this.contestParticipantRepository = contestParticipantRepository;
        this.antiCheatLogRepository = antiCheatLogRepository;
        this.submissionRepository = submissionRepository;
        this.teachingClassRepository = teachingClassRepository;
    }

    @GetMapping
    public ApiResponse<List<ContestItemResponse>> list(@RequestParam(required = false) Long viewerUserId) {
        OjUser viewer = viewerUserId == null ? null : ojUserRepository.findById(viewerUserId).orElse(null);
        List<Contest> contests = viewer != null && OjUser.Role.TEACHER.name().equals(viewer.getRole())
                ? contestRepository.findByCreatedByUserIdOrderByStartTimeDesc(viewerUserId)
                : contestRepository.findAllByOrderByStartTimeDesc();
        if (viewer != null && OjUser.Role.STUDENT.name().equals(viewer.getRole())) {
            contests = contests.stream()
                    .filter(item -> !Boolean.TRUE.equals(item.getLimitToClass())
                            || (viewer.getTeachingClassId() != null && viewer.getTeachingClassId().equals(item.getTeachingClassId())))
                    .toList();
        }

        Set<Long> contestIds = contests.stream().map(Contest::getId).collect(Collectors.toSet());
        Map<Long, Long> problemCountMap = contestProblemRepository.findAll().stream()
                .filter(cp -> contestIds.contains(cp.getId().getContestId()))
                .collect(Collectors.groupingBy(cp -> cp.getId().getContestId(), Collectors.counting()));

        final Set<Long> joinedContestIds = viewerUserId == null
                ? Set.of()
                : contestParticipantRepository.findByIdUserId(viewerUserId).stream()
                .map(cp -> cp.getId().getContestId())
                .collect(Collectors.toSet());

        LocalDateTime now = LocalDateTime.now();
        List<ContestItemResponse> data = contests.stream().map(contest -> {
            LocalDateTime enterOpenTime = contest.getStartTime().minusMinutes(ENTER_WINDOW_MINUTES);
            boolean canEnter = joinedContestIds.contains(contest.getId()) && !now.isBefore(enterOpenTime) && now.isBefore(contest.getEndTime());
            return new ContestItemResponse(
                    contest.getId(),
                    contest.getTitle(),
                    contest.getContestContent(),
                    contest.getStartTime(),
                    contest.getEndTime(),
                    enterOpenTime,
                    contest.getRankingPolicy(),
                    contest.getFreezeBoard(),
                    contest.getAllowedIpRule(),
                    contest.getLimitToClass(),
                    contest.getTeachingClassId(),
                    getContestStatus(contest, now),
                    problemCountMap.getOrDefault(contest.getId(), 0L).intValue(),
                    joinedContestIds.contains(contest.getId()),
                    canEnter,
                    contest.getCreatedByUserId()
            );
        }).toList();
        return ApiResponse.ok(data);
    }

    @GetMapping("/{id}")
    public ApiResponse<?> detail(@PathVariable Long id, @RequestParam(required = false) Long viewerUserId) {
        Contest contest = contestRepository.findById(id).orElse(null);
        if (contest == null) {
            return ApiResponse.fail("比赛不存在");
        }

        LocalDateTime now = LocalDateTime.now();
        OjUser viewer = viewerUserId == null ? null : ojUserRepository.findById(viewerUserId).orElse(null);
        boolean joined = viewer != null && contestParticipantRepository.existsByIdContestIdAndIdUserId(id, viewer.getId());
        boolean problemVisible = canViewContestProblems(contest, viewer, joined, now);
        boolean canEnter = joined && !now.isBefore(contest.getStartTime().minusMinutes(ENTER_WINDOW_MINUTES)) && now.isBefore(contest.getEndTime());

        List<ContestProblemResponse> problems = problemVisible ? loadContestProblems(id) : List.of();

        return ApiResponse.ok(new ContestDetailResponse(
                contest.getId(),
                contest.getTitle(),
                contest.getContestContent(),
                contest.getStartTime(),
                contest.getEndTime(),
                contest.getStartTime().minusMinutes(ENTER_WINDOW_MINUTES),
                contest.getRankingPolicy(),
                contest.getFreezeBoard(),
                contest.getAllowedIpRule(),
                contest.getLimitToClass(),
                contest.getTeachingClassId(),
                getContestStatus(contest, now),
                contestParticipantRepository.findByIdContestId(id).size(),
                contest.getCreatedByUserId(),
                joined,
                problemVisible,
                canEnter,
                problems
        ));
    }

    @GetMapping("/{id}/arena")
    public ApiResponse<?> arena(@PathVariable Long id, @RequestParam Long viewerUserId) {
        Contest contest = contestRepository.findById(id).orElse(null);
        if (contest == null) {
            return ApiResponse.fail("比赛不存在");
        }
        OjUser viewer = ojUserRepository.findById(viewerUserId).orElse(null);
        if (viewer == null) {
            return ApiResponse.fail("用户不存在");
        }
        boolean joined = contestParticipantRepository.existsByIdContestIdAndIdUserId(id, viewerUserId);
        boolean isManager = OjUser.Role.ADMIN.name().equals(viewer.getRole()) || OjUser.Role.TEACHER.name().equals(viewer.getRole());
        if (!joined && !isManager) {
            return ApiResponse.fail("请先报名比赛");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(contest.getStartTime().minusMinutes(ENTER_WINDOW_MINUTES))) {
            return ApiResponse.fail("仅比赛开始前5分钟内可进入比赛页面");
        }

        List<ContestProblemResponse> problems = loadContestProblems(id);
        List<Long> problemIds = problems.stream().map(ContestProblemResponse::problemId).toList();
        List<ContestParticipant> participants = contestParticipantRepository.findByIdContestId(id);
        Set<Long> participantIds = participants.stream().map(item -> item.getId().getUserId()).collect(Collectors.toSet());
        Map<Long, String> userNameMap = ojUserRepository.findAllById(participantIds).stream()
                .collect(Collectors.toMap(OjUser::getId, item -> Optional.ofNullable(item.getNickname()).filter(v -> !v.isBlank()).orElse(item.getUsername())));

        List<Submission> submissions = problemIds.isEmpty()
                ? List.of()
                : submissionRepository.findByProblemIdInAndSubmitTimeBetweenOrderBySubmitTimeDesc(problemIds, contest.getStartTime(), contest.getEndTime())
                .stream()
                .filter(item -> participantIds.contains(item.getUser().getId()))
                .toList();

        List<ContestSubmissionResponse> recentSubmissions = submissions.stream()
                .limit(80)
                .map(item -> new ContestSubmissionResponse(
                        item.getId(),
                        item.getUser().getId(),
                        userNameMap.getOrDefault(item.getUser().getId(), "未知用户"),
                        item.getProblem().getId(),
                        item.getJudgeStatus().name(),
                        item.getLanguage(),
                        item.getRuntimeMs(),
                        item.getMemoryKb(),
                        item.getSubmitTime()
                )).toList();

        List<ContestRankRowResponse> leaderboard = buildLeaderboard(contest, problems, participants, submissions, userNameMap);

        return ApiResponse.ok(new ContestArenaResponse(
                contest.getId(),
                contest.getTitle(),
                contest.getContestContent(),
                contest.getStartTime(),
                contest.getEndTime(),
                getContestStatus(contest, now),
                problems,
                recentSubmissions,
                leaderboard
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
        contest.setContestContent(normalizeContent(request.contestContent()));
        contest.setStartTime(request.startTime());
        contest.setEndTime(request.endTime());
        contest.setRankingPolicy(request.rankingPolicy() == null ? Contest.RankingPolicy.FORMAL : request.rankingPolicy());
        contest.setFreezeBoard(Boolean.TRUE.equals(request.freezeBoard()));
        contest.setAllowedIpRule(normalizeAllowedIpRuleByRanking(request.allowedIpRule(), contest.getRankingPolicy()));
        ApiResponse<?> classValidation = applyClassLimit(contest, request.limitToClass(), request.teachingClassId(), creator);
        if (!classValidation.success()) {
            return classValidation;
        }
        contest.setCreatedByUserId(request.creatorUserId());
        Contest saved = contestRepository.save(contest);

        List<ContestProblem> links = deduplicated.stream()
                .map(problemId -> new ContestProblem(saved.getId(), problemId, deduplicated.indexOf(problemId) + 1))
                .toList();
        contestProblemRepository.saveAll(links);

        return detail(saved.getId(), request.creatorUserId());
    }

    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable Long id, @RequestBody UpdateContestRequest request) {
        Contest contest = contestRepository.findById(id).orElse(null);
        if (contest == null) {
            return ApiResponse.fail("比赛不存在");
        }
        String currentStatus = getContestStatus(contest, LocalDateTime.now());
        if ("RUNNING".equals(currentStatus) || "FINISHED".equals(currentStatus)) {
            return ApiResponse.fail("仅未开始比赛可编辑");
        }
        OjUser operator = ojUserRepository.findById(request.operatorUserId()).orElse(null);
        if (operator == null) {
            return ApiResponse.fail("操作者不存在");
        }
        boolean canEdit = OjUser.Role.ADMIN.name().equals(operator.getRole()) || contest.getCreatedByUserId().equals(operator.getId());
        if (!canEdit) {
            return ApiResponse.fail("无权限编辑此比赛");
        }
        if (request.startTime() == null || request.endTime() == null || !request.endTime().isAfter(request.startTime())) {
            return ApiResponse.fail("比赛时间无效");
        }
        List<Long> deduplicated = request.problemIds().stream().distinct().toList();
        List<Problem> problems = problemRepository.findAllById(deduplicated);
        if (problems.size() != deduplicated.size()) {
            return ApiResponse.fail("存在无效题目，请重新选择");
        }

        contest.setTitle(request.title().trim());
        contest.setContestContent(normalizeContent(request.contestContent()));
        contest.setStartTime(request.startTime());
        contest.setEndTime(request.endTime());
        contest.setRankingPolicy(request.rankingPolicy() == null ? Contest.RankingPolicy.FORMAL : request.rankingPolicy());
        contest.setFreezeBoard(Boolean.TRUE.equals(request.freezeBoard()));
        contest.setAllowedIpRule(normalizeAllowedIpRuleByRanking(request.allowedIpRule(), contest.getRankingPolicy()));
        ApiResponse<?> classValidation = applyClassLimit(contest, request.limitToClass(), request.teachingClassId(), operator);
        if (!classValidation.success()) {
            return classValidation;
        }
        contestRepository.save(contest);

        List<ContestProblem> existing = contestProblemRepository.findByIdContestIdOrderBySortOrderAsc(id);
        contestProblemRepository.deleteAll(existing);
        List<ContestProblem> links = new ArrayList<>();
        for (int i = 0; i < deduplicated.size(); i++) {
            links.add(new ContestProblem(id, deduplicated.get(i), i + 1));
        }
        contestProblemRepository.saveAll(links);

        return detail(id, request.operatorUserId());
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
        if (Boolean.TRUE.equals(contest.getLimitToClass())) {
            if (user.getTeachingClassId() == null || !user.getTeachingClassId().equals(contest.getTeachingClassId())) {
                return ApiResponse.fail("该活动限定班级参与，你不在指定班级中");
            }
        }
        if (contestParticipantRepository.existsByIdContestIdAndIdUserId(id, request.userId())) {
            return ApiResponse.fail("你已报名该比赛");
        }

        contestParticipantRepository.save(new ContestParticipant(id, request.userId()));
        return ApiResponse.ok("报名成功");
    }

    @PostMapping("/{id}/enter")
    public ApiResponse<?> enter(@PathVariable Long id, @RequestBody EnterContestRequest request, HttpServletRequest httpRequest) {
        Contest contest = contestRepository.findById(id).orElse(null);
        if (contest == null) {
            return ApiResponse.fail("比赛不存在");
        }
        OjUser user = ojUserRepository.findById(request.userId()).orElse(null);
        if (user == null) {
            return ApiResponse.fail("用户不存在");
        }
        ContestParticipant participant = contestParticipantRepository.findById(new ContestParticipantId(id, request.userId())).orElse(null);
        if (participant == null) {
            return ApiResponse.fail("请先报名比赛");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(contest.getEndTime())) {
            return ApiResponse.fail("比赛已结束");
        }
        if (now.isBefore(contest.getStartTime().minusMinutes(ENTER_WINDOW_MINUTES))) {
            return ApiResponse.fail("比赛开始前5分钟内才可进入比赛");
        }

        String currentIp = resolveIp(httpRequest);
        if (contest.getRankingPolicy() == Contest.RankingPolicy.FORMAL && OjUser.Role.STUDENT.name().equals(user.getRole())) {
            if (!ipAllowedByRule(currentIp, contest.getAllowedIpRule())) {
                return ApiResponse.fail("当前IP不在正式比赛允许范围内");
            }
            if (participant.getLastAccessIp() != null && !participant.getLastAccessIp().isBlank()
                    && !participant.getLastAccessIp().equals(currentIp)) {
                saveContestCheatLog(user, "CONTEST_IP_CHANGED",
                        "比赛#" + id + " 进入IP从" + participant.getLastAccessIp() + "变化为" + currentIp);
            }
            participant.setLastAccessIp(currentIp);
        }
        contestParticipantRepository.save(participant);
        return ApiResponse.ok(Map.of("contestId", id, "ip", currentIp, "allowed", true));
    }

    @GetMapping("/problems/options")
    public ApiResponse<List<ProblemOptionResponse>> contestProblemOptions() {
        return ApiResponse.ok(problemRepository.findAll().stream()
                .map(item -> new ProblemOptionResponse(item.getId(), item.getTitle(), item.getDifficulty()))
                .sorted(Comparator.comparing(ProblemOptionResponse::id))
                .toList());
    }

    private boolean canViewContestProblems(Contest contest, OjUser viewer, boolean joined, LocalDateTime now) {
        if (viewer != null && (OjUser.Role.ADMIN.name().equals(viewer.getRole()) || OjUser.Role.TEACHER.name().equals(viewer.getRole()))) {
            return true;
        }
        if (!joined) {
            return false;
        }
        return !now.isBefore(contest.getStartTime().minusMinutes(ENTER_WINDOW_MINUTES));
    }

    private List<ContestProblemResponse> loadContestProblems(Long contestId) {
        List<ContestProblem> linked = contestProblemRepository.findByIdContestIdOrderBySortOrderAsc(contestId);
        List<Long> problemIds = linked.stream().map(cp -> cp.getId().getProblemId()).toList();
        Map<Long, Problem> problemMap = problemRepository.findAllById(problemIds).stream()
                .collect(Collectors.toMap(Problem::getId, p -> p));
        return linked.stream().map(item -> {
            Problem p = problemMap.get(item.getId().getProblemId());
            return new ContestProblemResponse(
                    item.getId().getProblemId(),
                    p == null ? "题目已删除" : p.getTitle(),
                    item.getSortOrder(),
                    p == null ? "未知" : p.getDifficulty()
            );
        }).sorted(Comparator.comparing(ContestProblemResponse::sortOrder)).toList();
    }

    private List<ContestRankRowResponse> buildLeaderboard(
            Contest contest,
            List<ContestProblemResponse> problems,
            List<ContestParticipant> participants,
            List<Submission> submissions,
            Map<Long, String> userNameMap
    ) {
        Map<Long, Map<Long, ProblemStat>> scoreboard = new HashMap<>();
        submissions.stream()
                .sorted(Comparator.comparing(Submission::getSubmitTime))
                .forEach(item -> {
                    Map<Long, ProblemStat> userBoard = scoreboard.computeIfAbsent(item.getUser().getId(), k -> new HashMap<>());
                    ProblemStat stat = userBoard.computeIfAbsent(item.getProblem().getId(), k -> new ProblemStat());
                    if (stat.solved) {
                        return;
                    }
                    stat.submitCount++;
                    if (item.getJudgeStatus() == Submission.JudgeStatus.AC) {
                        stat.solved = true;
                        stat.firstAcTime = item.getSubmitTime();
                    } else {
                        stat.wrongBeforeAc++;
                    }
                });

        List<ContestRankRowResponse> rows = participants.stream().map(participant -> {
                    Long userId = participant.getId().getUserId();
                    Map<Long, ProblemStat> stats = scoreboard.getOrDefault(userId, Map.of());
                    int solved = 0;
                    int penalty = 0;
                    int totalSubmit = 0;
                    List<String> problemStates = new ArrayList<>();
                    for (ContestProblemResponse problem : problems) {
                        ProblemStat stat = stats.get(problem.problemId());
                        if (stat == null) {
                            problemStates.add("-");
                            continue;
                        }
                        totalSubmit += stat.submitCount;
                        if (stat.solved && stat.firstAcTime != null) {
                            solved++;
                            long acMinutes = Duration.between(contest.getStartTime(), stat.firstAcTime).toMinutes();
                            int problemPenalty = (int) Math.max(0, acMinutes) + stat.wrongBeforeAc * 20;
                            penalty += problemPenalty;
                            problemStates.add("AC " + acMinutes + "m");
                        } else {
                            problemStates.add("+" + stat.submitCount);
                        }
                    }
                    return new ContestRankRowResponse(
                            userId,
                            userNameMap.getOrDefault(userId, "未知用户"),
                            solved,
                            penalty,
                            totalSubmit,
                            problemStates
                    );
                }).sorted(Comparator
                        .comparing(ContestRankRowResponse::solved).reversed()
                        .thenComparing(ContestRankRowResponse::penalty)
                        .thenComparing(ContestRankRowResponse::userId))
                .toList();

        List<ContestRankRowResponse> ranked = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            ContestRankRowResponse row = rows.get(i);
            ranked.add(new ContestRankRowResponse(
                    row.userId(),
                    row.nickname(),
                    row.solved(),
                    row.penalty(),
                    row.submitCount(),
                    row.problemStates(),
                    i + 1
            ));
        }
        return ranked;
    }

    private String getContestStatus(Contest contest, LocalDateTime now) {
        if (now.isBefore(contest.getStartTime())) return "NOT_STARTED";
        if (now.isAfter(contest.getEndTime())) return "FINISHED";
        return "RUNNING";
    }

    private ApiResponse<?> applyClassLimit(Contest contest, Boolean limitToClass, Long teachingClassId, OjUser operator) {
        boolean enabled = Boolean.TRUE.equals(limitToClass);
        contest.setLimitToClass(enabled);
        if (!enabled) {
            contest.setTeachingClassId(null);
            return ApiResponse.ok("ok");
        }
        if (teachingClassId == null) {
            return ApiResponse.fail("已勾选限定班级，请选择班级");
        }
        TeachingClass teachingClass = teachingClassRepository.findById(teachingClassId).orElse(null);
        if (teachingClass == null) {
            return ApiResponse.fail("选择的班级不存在");
        }
        if (OjUser.Role.TEACHER.name().equals(operator.getRole()) && !teachingClass.getTeacherId().equals(operator.getId())) {
            return ApiResponse.fail("老师仅可选择自己创建的班级");
        }
        contest.setTeachingClassId(teachingClassId);
        return ApiResponse.ok("ok");
    }

    private String normalizeContent(String content) {
        if (content == null) return null;
        String trimmed = content.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeAllowedIpRuleByRanking(String rule, Contest.RankingPolicy rankingPolicy) {
        if (rankingPolicy == Contest.RankingPolicy.CLASSROOM) {
            return null;
        }
        if (rule == null) return null;
        String normalized = rule.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return Optional.ofNullable(request.getRemoteAddr()).orElse("");
    }

    private boolean ipAllowedByRule(String ip, String allowedIpRule) {
        if (allowedIpRule == null || allowedIpRule.isBlank()) {
            return true;
        }
        return List.of(allowedIpRule.split(",")).stream()
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .anyMatch(ip::startsWith);
    }

    private void saveContestCheatLog(OjUser user, String type, String detail) {
        AntiCheatLog log = new AntiCheatLog();
        log.setUser(user);
        log.setBehaviorType(type);
        log.setDetailInfo(detail);
        log.setOccurredTime(LocalDateTime.now());
        antiCheatLogRepository.save(log);
    }

    private static class ProblemStat {
        int submitCount;
        int wrongBeforeAc;
        boolean solved;
        LocalDateTime firstAcTime;
    }

    public record CreateContestRequest(
            @NotNull Long creatorUserId,
            @NotBlank String title,
            String contestContent,
            @NotNull LocalDateTime startTime,
            @NotNull LocalDateTime endTime,
            Contest.RankingPolicy rankingPolicy,
            Boolean freezeBoard,
            String allowedIpRule,
            Boolean limitToClass,
            Long teachingClassId,
            @NotEmpty List<Long> problemIds
    ) {}

    public record UpdateContestRequest(
            @NotNull Long operatorUserId,
            @NotBlank String title,
            String contestContent,
            @NotNull LocalDateTime startTime,
            @NotNull LocalDateTime endTime,
            Contest.RankingPolicy rankingPolicy,
            Boolean freezeBoard,
            String allowedIpRule,
            Boolean limitToClass,
            Long teachingClassId,
            @NotEmpty List<Long> problemIds
    ) {}

    public record RegisterContestRequest(@NotNull Long userId) {}
    public record EnterContestRequest(@NotNull Long userId) {}
    public record ContestProblemResponse(Long problemId, String title, Integer sortOrder, String difficulty) {}
    public record ProblemOptionResponse(Long id, String title, String difficulty) {}

    public record ContestItemResponse(
            Long id, String title, String contestContent, LocalDateTime startTime, LocalDateTime endTime,
            LocalDateTime enterOpenTime,
            Contest.RankingPolicy rankingPolicy, Boolean freezeBoard, String allowedIpRule,
            Boolean limitToClass, Long teachingClassId,
            String status, Integer problemCount, Boolean joined, Boolean canEnter, Long createdByUserId
    ) {}

    public record ContestDetailResponse(
            Long id,
            String title,
            String contestContent,
            LocalDateTime startTime,
            LocalDateTime endTime,
            LocalDateTime enterOpenTime,
            Contest.RankingPolicy rankingPolicy,
            Boolean freezeBoard,
            String allowedIpRule,
            Boolean limitToClass,
            Long teachingClassId,
            String status,
            Integer participantCount,
            Long createdByUserId,
            Boolean joined,
            Boolean problemVisible,
            Boolean canEnter,
            List<ContestProblemResponse> problems
    ) {}

    public record ContestSubmissionResponse(
            Long id,
            Long userId,
            String nickname,
            Long problemId,
            String judgeStatus,
            String language,
            Integer runtimeMs,
            Integer memoryKb,
            LocalDateTime submitTime
    ) {}

    public record ContestRankRowResponse(
            Long userId,
            String nickname,
            Integer solved,
            Integer penalty,
            Integer submitCount,
            List<String> problemStates,
            Integer rank
    ) {
        public ContestRankRowResponse(Long userId, String nickname, Integer solved, Integer penalty,
                                      Integer submitCount, List<String> problemStates) {
            this(userId, nickname, solved, penalty, submitCount, problemStates, 0);
        }
    }

    public record ContestArenaResponse(
            Long id,
            String title,
            String contestContent,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String status,
            List<ContestProblemResponse> problems,
            List<ContestSubmissionResponse> submissions,
            List<ContestRankRowResponse> leaderboard
    ) {}
}