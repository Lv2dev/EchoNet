package com.lv2dev.echonet.config;

import com.lv2dev.echonet.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.filter.CorsFilter;

import static org.springframework.security.config.Customizer.withDefaults;

@EnableWebSecurity
@Configuration
public class WebSecurityConfig {
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String[] SWAGGER_URI = {
            "/swagger-ui.html", "/v2/api-docs", "/swagger-resources/**", "/webjars/**", "/swagger/**"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // http 시큐리티 빌더
        http
                .csrf((csrf) -> csrf
                        .ignoringRequestMatchers("/api/**", "/api/token/**","/api/auth/**")
                ) // csrf를 비활성화할 경로 설정
                .httpBasic(withDefaults()) // 기본 http 인증 사용하지 않음
                // 세션을 사용하지 않는 상태로 설정
                .sessionManagement((session) -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // 특정 경로에 대한 접근 허용 (인증되지 않은 사용자도 접근 가능)
                .authorizeHttpRequests((authorize) -> authorize
                        .requestMatchers(SWAGGER_URI).permitAll()
                        .requestMatchers(
                                "/",
                                "/api/unauth/**",
                                "/api/token/**",
                                "/error",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()
                        // 그 외 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                );
        http
                // 인증되지 않은 요청에 대한 처리 (401 Unauthorized 응답)
                .exceptionHandling(exceptionHandling ->
                        exceptionHandling.authenticationEntryPoint(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
                        )
                );
        // JWT 인증 필터를 CorsFilter 뒤에 추가
        http.addFilterAfter(jwtAuthenticationFilter, CorsFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


}