package com.lv2dev.echonet.controller;

import com.lv2dev.echonet.model.Member;
import com.lv2dev.echonet.persistence.MemberRepository;
import com.lv2dev.echonet.service.MemberService;
import com.lv2dev.echonet.service.TokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
public class TokenController {

    @Autowired
    private TokenService tokenService;

    private MemberRepository memberRepository;

    private PasswordEncoder passwordEncoder;

    @Autowired
    private MemberService memberService;

    /**
     * 사용자 로그인 처리.
     * 성공 시, AccessToken을 반환하고 RefreshToken을 HttpOnly 쿠키로 설정.
     *
     * @param loginDetails 로그인 정보
     * @param response     클라이언트 응답 객체
     * @param request HttpServletRequest 객체
     * @return AccessToken 문자열
     */
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody Member loginDetails, HttpServletResponse response, HttpServletRequest request) {
        // 사용자 이메일로 멤버 조회
        Member member = memberRepository.findByEmail(loginDetails.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid email or password"));

        // 비밀번호 검증
        if (!passwordEncoder.matches(loginDetails.getPassword(), member.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid email or password");
        }

        // 로그인 기록 저장
        memberService.createLoginHistory(member, request);

        // AccessToken 생성
        String accessToken = tokenService.createAccessToken(member);
        // RefreshToken 생성 및 저장
        String refreshToken = tokenService.createRefreshToken(member);
        member.setRefreshToken(refreshToken);
        memberRepository.save(member);

        // RefreshToken을 HttpOnly 쿠키로 설정
        setRefreshTokenCookie(response, refreshToken);

        return ResponseEntity.ok().header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken).body("Login successful");
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