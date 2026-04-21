package com.oj.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oj.dto.ApiResponse;
import com.oj.entity.OjUser;
import com.oj.repository.OjUserRepository;
import com.oj.tools.RedisUtil;
import com.oj.tools.TokenUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final OjUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper mapper;

    public AuthController(OjUserRepository userRepository, PasswordEncoder passwordEncoder,ObjectMapper mapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mapper=mapper;
    }

    @GetMapping("/user-info")
    public ApiResponse<LoginResponse> getUserInfo(HttpServletRequest httpServletRequest){
        System.out.println("sb");
        System.out.println(httpServletRequest.getHeader("token"));
        LoginResponse loginResponse=TokenUtils.getLoginUserDetailsFromToken(httpServletRequest.getHeader("token"));
        System.out.println(loginResponse);
        return loginResponse==null?ApiResponse.fail("没有登录信息"):ApiResponse.ok(loginResponse);
    }

    @PostMapping("/register")
    public ApiResponse<LoginResponse> register(@RequestBody RegisterRequest request) {
        if (userRepository.findByUsername(request.username()).isPresent()) {
            return ApiResponse.fail("用户名已存在");
        }

        OjUser user = new OjUser();
        user.setUsername(request.username().trim());
        user.setNickname(request.nickname());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(normalizeRole(request.role()));
        OjUser saved = userRepository.save(user);
        return ApiResponse.ok(new LoginResponse(saved.getId(), saved.getUsername(), saved.getNickname(), saved.getRole()));
    }

    @PostMapping("/login")
    public ApiResponse<String> login(@RequestBody LoginRequest request) {
        //获取登录用户信息
        LoginResponse loginResponse=userRepository.findByUsername(request.username())
                .filter(user -> passwordEncoder.matches(request.password(), user.getPasswordHash()))
                .map(user->new LoginResponse(user.getId(), user.getUsername(), user.getNickname(), user.getRole()))
                .orElseGet(null);
        if(loginResponse==null){
            return ApiResponse.fail("账号或密码错误");
        }
        String token=UUID.randomUUID().toString();
        try {
            RedisUtil.set("login:token:"+token, mapper.writeValueAsString(loginResponse), 7, TimeUnit.DAYS);
        }catch (Exception e){
            return ApiResponse.fail("账号或密码错误");
        }
        return ApiResponse.ok(token);
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return OjUser.Role.STUDENT.name();
        }
        return switch (role.toUpperCase(Locale.ROOT)) {
            case "ADMIN" -> OjUser.Role.ADMIN.name();
            case "TEACHER" -> OjUser.Role.TEACHER.name();
            default -> OjUser.Role.STUDENT.name();
        };
    }

    public record RegisterRequest(@NotBlank String username, @NotBlank String password, @NotBlank String nickname, String role) { }
    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record LoginResponse(Long id, String username, String nickname, String role) {}
}