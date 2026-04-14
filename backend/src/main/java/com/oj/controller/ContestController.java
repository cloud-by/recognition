package com.oj.controller;

import com.oj.dto.ApiResponse;
import com.oj.entity.Contest;
import com.oj.repository.ContestRepository;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contests")
public class ContestController {

    private final ContestRepository contestRepository;

    public ContestController(ContestRepository contestRepository) {
        this.contestRepository = contestRepository;
    }

    @GetMapping
    public ApiResponse<List<Contest>> list() {
        return ApiResponse.ok(contestRepository.findAll());
    }

    @PostMapping
    public ApiResponse<Contest> create(@RequestBody CreateContestRequest request) {
        Contest contest = new Contest();
        contest.setTitle(request.title());
        contest.setStartTime(request.startTime());
        contest.setEndTime(request.endTime());
        contest.setContestType(request.contestType());
        contest.setFreezeBoard(Boolean.TRUE.equals(request.freezeBoard()));
        return ApiResponse.ok(contestRepository.save(contest));
    }

    public record CreateContestRequest(
            @NotBlank String title,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Contest.ContestType contestType,
            Boolean freezeBoard
    ) {
    }
}