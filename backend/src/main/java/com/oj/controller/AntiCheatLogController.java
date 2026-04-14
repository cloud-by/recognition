package com.oj.controller;

import com.oj.dto.ApiResponse;
import com.oj.entity.AntiCheatLog;
import com.oj.entity.OjUser;
import com.oj.entity.Submission;
import com.oj.repository.AntiCheatLogRepository;
import com.oj.repository.OjUserRepository;
import com.oj.repository.SubmissionRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/anti-cheat/logs")
public class AntiCheatLogController {

    private final AntiCheatLogRepository antiCheatLogRepository;
    private final OjUserRepository userRepository;
    private final SubmissionRepository submissionRepository;

    public AntiCheatLogController(
            AntiCheatLogRepository antiCheatLogRepository,
            OjUserRepository userRepository,
            SubmissionRepository submissionRepository
    ) {
        this.antiCheatLogRepository = antiCheatLogRepository;
        this.userRepository = userRepository;
        this.submissionRepository = submissionRepository;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String behaviorType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        Specification<AntiCheatLog> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (userId != null) {
                predicates.add(criteriaBuilder.equal(root.get("user").get("id"), userId));
            }
            if (behaviorType != null && !behaviorType.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("behaviorType"), behaviorType));
            }
            if (startTime != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("occurredTime"), startTime));
            }
            if (endTime != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("occurredTime"), endTime));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };

        List<Map<String, Object>> logs = antiCheatLogRepository
                .findAll(spec, Sort.by(Sort.Direction.DESC, "occurredTime"))
                .stream()
                .map(log -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", log.getId());
                    item.put("userId", log.getUser().getId());
                    item.put("submissionId", Optional.ofNullable(log.getSubmission()).map(Submission::getId).orElse(null));
                    item.put("behaviorType", log.getBehaviorType());
                    item.put("detailInfo", Optional.ofNullable(log.getDetailInfo()).orElse(""));
                    item.put("occurredTime", log.getOccurredTime());
                    return item;
                })
                .toList();
        return ApiResponse.ok(logs);
    }

    @PostMapping
    public ApiResponse<?> create(@RequestBody CreateLogRequest request) {
        Optional<OjUser> user = userRepository.findById(request.userId());
        if (user.isEmpty()) {
            return ApiResponse.fail("用户不存在");
        }

        Optional<Submission> submission = Optional.ofNullable(request.submissionId())
                .flatMap(submissionRepository::findById);

        AntiCheatLog log = new AntiCheatLog();
        log.setUser(user.get());
        submission.ifPresent(log::setSubmission);
        log.setBehaviorType(request.behaviorType());
        log.setDetailInfo(request.detailInfo());
        log.setOccurredTime(Optional.ofNullable(request.occurredTime()).orElse(LocalDateTime.now()));

        AntiCheatLog saved = antiCheatLogRepository.save(log);
        return ApiResponse.ok(Map.of("id", saved.getId()));
    }

    public record CreateLogRequest(
            @NotNull Long userId,
            Long submissionId,
            @NotBlank String behaviorType,
            String detailInfo,
            LocalDateTime occurredTime
    ) {
    }
}