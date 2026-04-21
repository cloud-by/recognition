package com.oj.tools;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.oj.controller.AuthController;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TokenUtils {

    private static ObjectMapper mapper=new ObjectMapper();

    public static AuthController.LoginResponse getLoginUserDetailsFromToken(String token){
        if(StringUtils.hasText(token)){
            String key="login:token:"+token;
            String json=RedisUtil.get(key);
            if(StringUtils.hasText(json)) {
                try {
                    return mapper.readValue(json, AuthController.LoginResponse.class);
                }catch (Exception e){
                    return null;
                }
            }
        }
        return null;
    }
}
