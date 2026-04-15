package com.oj.controller;

import com.oj.dto.ApiResponse;
import com.oj.entity.Contest;
import com.oj.entity.ContestParticipant;
import com.oj.entity.OjUser;
import com.oj.repository.ContestParticipantRepository;
import com.oj.repository.ContestRepository;
import com.oj.repository.OjUserRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final OjUserRepository userRepository;
    private final ContestRepository contestRepository;
    private final ContestParticipantRepository contestParticipantRepository;

    public UserController(
            OjUserRepository userRepository,
            ContestRepository contestRepository,
            ContestParticipantRepository contestParticipantRepository
    ) {
        this.userRepository = userRepository;
        this.contestRepository = contestRepository;
        this.contestParticipantRepository = contestParticipantRepository;
    }

    @GetMapping
    public ApiResponse<List<OjUser>> list() {
        return ApiResponse.ok(userRepository.findAll());
    }

    @PatchMapping("/{id}/username")
    public ApiResponse<?> updateUsername(@PathVariable Long id, @RequestBody UpdateUsernameRequest request) {
        OjUser user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ApiResponse.fail("用户不存在");
        }
        String username = request.newUsername().trim();
        if (username.isBlank()) {
            return ApiResponse.fail("用户名不能为空");
        }
        Optional<OjUser> existed = userRepository.findByUsername(username);
        if (existed.isPresent() && !existed.get().getId().equals(id)) {
            return ApiResponse.fail("用户名已存在");
        }

        boolean inRunningContest = contestParticipantRepository.findAll().stream()
                .filter(cp -> cp.getId().getUserId().equals(id))
                .map(ContestParticipant::getId)
                .map(contestParticipantId -> contestRepository.findById(contestParticipantId.getContestId()).orElse(null))
                .anyMatch(contest -> contest != null && isRunning(contest));

        if (inRunningContest) {
            return ApiResponse.fail("比赛进行期间不允许修改用户名");
        }

        user.setUsername(username);
        userRepository.save(user);
        return ApiResponse.ok(Map.of("id", user.getId(), "username", user.getUsername()));
    }

    private boolean isRunning(Contest contest) {
        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(contest.getStartTime()) && !now.isAfter(contest.getEndTime());
    }

    public record UpdateUsernameRequest(@NotBlank String newUsername) {}
    public record UpdateUserRoleRequest(@NotNull Long adminUserId, @NotBlank String role) {}
}