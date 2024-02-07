package com.lv2dev.echonet.controller;

import com.lv2dev.echonet.model.Member;
import com.lv2dev.echonet.service.TokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class TokenController {

    @Autowired
    private TokenService tokenService;

    /**
     * 사용자 로그인 처리.
     * 성공 시, AccessToken을 반환하고 RefreshToken을 HttpOnly 쿠키로 설정.
     *
     * @param loginDetails 로그인 정보
     * @param response     클라이언트 응답 객체
     * @return AccessToken 문자열
     */
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody Member loginDetails, HttpServletResponse response) {
        // 사용자 인증 로직 구현 필요
        String accessToken = tokenService.createAccessToken(loginDetails);
        String refreshToken = tokenService.createRefreshToken(loginDetails);

        setRefreshTokenCookie(response, refreshToken);

        return ResponseEntity.ok().body(accessToken);
    }

    /**
     * RefreshToken을 사용하여 AccessToken 재발급.
     *
     * @param refreshToken RefreshToken 쿠키 값
     * @return 새로운 AccessToken
     */
    @GetMapping("/refresh")
    public ResponseEntity<String> refreshAccessToken(@CookieValue(name = "refreshToken") String refreshToken) {
        try {
            String newAccessToken = tokenService.refreshAccessToken(refreshToken);
            return ResponseEntity.ok().body(newAccessToken);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid refresh token");
        }
    }

    /**
     * AccessToken 유효성 검증.
     *
     * @param token 검증하고자 하는 토큰
     * @return 유효성 검증 결과 메시지
     */
    @GetMapping("/validate")
    public ResponseEntity<String> validateToken(@RequestParam String token) {
        boolean isValid = tokenService.isTokenValid(token);
        if (isValid) {
            return ResponseEntity.ok().body("Token is valid");
        } else {
            return ResponseEntity.badRequest().body("Invalid token");
        }
    }

    /**
     * 사용자 로그아웃 처리.
     * 클라이언트에서 RefreshToken 쿠키를 제거.
     *
     * @param response 클라이언트 응답 객체
     * @return 로그아웃 성공 메시지
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletResponse response) {
        removeRefreshTokenCookie(response);
        return ResponseEntity.ok().body("Logged out successfully");
    }

    /**
     * RefreshToken을 HttpOnly 쿠키로 설정하는 헬퍼 메서드
     * @param response 클라이언트 응답 객체
     * */
    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/");
        response.addCookie(refreshCookie);
    }

    /**
     * 클라이언트의 RefreshToken 쿠키를 제거하는 헬퍼 메서드
     * @param response 클라이언트 응답 객체
     * */
    private void removeRefreshTokenCookie(HttpServletResponse response) {
        Cookie refreshCookie = new Cookie("refreshToken", null);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setMaxAge(0);
        refreshCookie.setPath("/");
        response.addCookie(refreshCookie);
    }
}