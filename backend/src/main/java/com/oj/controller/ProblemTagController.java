package com.oj.controller;

import com.oj.dto.ApiResponse;
import com.oj.entity.OjUser;
import com.oj.entity.ProblemTag;
import com.oj.repository.OjUserRepository;
import com.oj.repository.ProblemTagRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/problem-tags")
public class ProblemTagController {

    private final ProblemTagRepository problemTagRepository;
    private final OjUserRepository userRepository;

    public ProblemTagController(ProblemTagRepository problemTagRepository, OjUserRepository userRepository) {
        this.problemTagRepository = problemTagRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ApiResponse<List<ProblemTag>> list(@RequestParam(required = false) String keyword) {
        String normalized = Optional.ofNullable(keyword).orElse("").trim().toLowerCase(Locale.ROOT);
        List<ProblemTag> tags = problemTagRepository.findAll().stream()
                .filter(tag -> normalized.isBlank() || tag.getName().toLowerCase(Locale.ROOT).contains(normalized))
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .toList();
        return ApiResponse.ok(tags);
    }

    @PostMapping
    public ApiResponse<ProblemTag> create(@RequestBody CreateProblemTagRequest request) {
        if (!isAdmin(request.operatorUserId())) {
            return ApiResponse.fail("仅管理员可管理标签");
        }
        String name = request.name().trim();
        if (problemTagRepository.findByName(name).isPresent()) {
            return ApiResponse.fail("标签已存在");
        }
        ProblemTag tag = new ProblemTag();
        tag.setName(name);
        return ApiResponse.ok(problemTagRepository.save(tag));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<List<ProblemTag>> delete(@PathVariable Long id, @RequestParam Long operatorUserId) {
        if (!isAdmin(operatorUserId)) {
            return ApiResponse.fail("仅管理员可管理标签");
        }
        if (!problemTagRepository.existsById(id)) {
            return ApiResponse.fail("标签不存在");
        }
        problemTagRepository.deleteById(id);
        return ApiResponse.ok(problemTagRepository.findAll().stream()
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .toList());
    }

    private boolean isAdmin(Long userId) {
        OjUser user = userRepository.findById(userId).orElse(null);
        return user != null && OjUser.Role.ADMIN.name().equals(user.getRole());
    }

    public record CreateProblemTagRequest(@NotNull Long operatorUserId, @NotBlank String name) {}
}
