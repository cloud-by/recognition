package com.backend;

import com.oj.controller.SubmissionController;
import com.oj.repository.SubmissionRepository;
import com.oj.tools.FileUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

public class test {
    static PasswordEncoder passwordEncoder;
    static SubmissionController submissionController;
    public static void main(String[] args){
//        passwordEncoder=new BCryptPasswordEncoder();
//        System.out.println(passwordEncoder.encode("123456"));
        System.out.println(UUID.randomUUID());
    }
}
