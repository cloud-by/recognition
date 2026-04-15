package com.oj.controller;

import com.oj.dto.ApiResponse;
import com.oj.entity.OjUser;
import com.oj.repository.OjUserRepository;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final OjUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(OjUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ApiResponse<OjUser> register(@RequestBody RegisterRequest request) {
        if (userRepository.findByUsername(request.username()).isPresent()) {
            return ApiResponse.fail("用户名已存在");
        }

        OjUser user = new OjUser();
        user.setUsername(request.username());
        user.setNickname(request.nickname());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        return ApiResponse.ok(userRepository.save(user));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        return userRepository.findByUsername(request.username())
                .filter(user -> passwordEncoder.matches(request.password(), user.getPasswordHash()))
                .map(user -> ApiResponse.ok(new LoginResponse(user.getId(), user.getUsername(), user.getNickname())))
                .orElseGet(() -> ApiResponse.fail("用户名或密码错误"));
    }

    public record RegisterRequest(@NotBlank String username, @NotBlank String password, @NotBlank String nickname) {
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record LoginResponse(Long id, String username, String nickname) {
    }
}