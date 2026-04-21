package com.backend;

import com.oj.controller.SubmissionController;
import com.oj.repository.SubmissionRepository;
import com.oj.tools.FileUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class test {
    static PasswordEncoder passwordEncoder;
    static SubmissionController submissionController;
    public static void main(String[] args){
//        passwordEncoder=new BCryptPasswordEncoder();
//        System.out.println(passwordEncoder.encode("123456"));
//        System.out.println(FileUtil.readInOutFiles("problem/1").getOutput(1));
//        submissionController=new SubmissionController(new SubmissionRepository(),);
//        StringRedisTemplate redisTemplate=new StringRedisTemplate();
//        redisTemplate.opsForValue().set("test", "Hello Redis");
//        String value = redisTemplate.opsForValue().get("test");
//        System.out.println("Redis连接成功，值：" + value);
    }
}
