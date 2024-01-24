package com.lv2dev.echonet.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration // 스프링 빈으로 등록
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${front.domain}")
    private String frontDomain;


    // 유지시간
    private final long MAX_AGE_SECS = 3600;

    // Cors 방지
    // 응애
    @Override
    public void addCorsMappings(CorsRegistry registry){
        //모든 경로에 대해
        registry.addMapping("/**")
                // Origin이 frontDomain에 대해
                .allowedOrigins(frontDomain)
                // GET, POST, PUT, PATCH, DELETE, OPTIONS 메서드 허용
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*") // 모든 헤더 허용
                .allowCredentials(true) // 인증에 관한 정보 허용
                .maxAge(MAX_AGE_SECS); // 유지시간
    }

}
