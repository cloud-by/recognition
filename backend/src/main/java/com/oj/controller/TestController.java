package com.oj.controller;

import com.oj.dto.ApiResponse;
import com.oj.tools.RedisUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/test")
public class TestController {
    @GetMapping()
    public ApiResponse<?> test(){
        RedisUtil.set("key","adw");
        return ApiResponse.ok(Map.of());
    }
}
